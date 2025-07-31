package org.waveywaves.jenkins.plugins.tekton.client.e2e;

import hudson.util.Secret;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import java.io.FileInputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.global.ClusterConfig;
import org.waveywaves.jenkins.plugins.tekton.client.global.TektonGlobalConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithJenkins
public abstract class E2ETestBase {

    private static final Logger LOGGER = Logger.getLogger(E2ETestBase.class.getName());

    protected static final String KIND_CLUSTER_NAME = "tekton-e2e-test";
    protected static final String TEST_NAMESPACE = "tekton-test";
    protected static final String TEKTON_VERSION = "v1.0.0";

    protected JenkinsRule jenkinsRule;
    protected KubernetesClient kubernetesClient;
    protected TektonClient tektonClient;
    protected String currentTestNamespace;

    @BeforeAll
    public void setUpE2EEnvironment(JenkinsRule jenkins) throws Exception {
        this.jenkinsRule = jenkins;

        // Detect if running in GitHub Actions with pre-setup
        if (isGitHubActionsWithPreSetup()) {
            LOGGER.info("Detected GitHub Actions environment with pre-setup cluster");
            setupForGitHubActions();
        } else {
            LOGGER.info("Setting up E2E environment from scratch");
            setupFromScratch();
        }

        LOGGER.info("E2E test environment setup complete");
    }

    private boolean isGitHubActionsWithPreSetup() {
        boolean isGitHubActions = "true".equals(System.getenv("GITHUB_ACTIONS"));
        boolean hasKubeconfig = System.getenv("KUBECONFIG") != null;

        if (isGitHubActions && hasKubeconfig) {
            // Check if cluster is already accessible
            try {
                ProcessBuilder pb = new ProcessBuilder("kubectl", "cluster-info");
                pb.environment().put("KUBECONFIG", System.getenv("KUBECONFIG"));
                Process process = pb.start();
                boolean accessible = process.waitFor(15, TimeUnit.SECONDS) && process.exitValue() == 0;
                LOGGER.info("Cluster accessibility check: " + accessible);
                return accessible;
            } catch (Exception e) {
                LOGGER.warning("Failed to check cluster accessibility: " + e.getMessage());
                // Don't fail immediately, try a simpler check
                try {
                    ProcessBuilder pb = new ProcessBuilder("kubectl", "get", "nodes");
                    pb.environment().put("KUBECONFIG", System.getenv("KUBECONFIG"));
                    Process process = pb.start();
                    boolean accessible = process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
                    LOGGER.info("Fallback cluster accessibility check: " + accessible);
                    return accessible;
                } catch (Exception e2) {
                    LOGGER.warning("Fallback cluster check also failed: " + e2.getMessage());
                    return false;
                }
            }
        }

        return false;
    }

    private void setupForGitHubActions() throws Exception {
        LOGGER.info("Using pre-configured GitHub Actions environment");

        // Use existing kubeconfig from environment
        String kubeconfigPath = System.getenv("KUBECONFIG");
        LOGGER.info("Using kubeconfig: " + kubeconfigPath);

        // Setup Kubernetes clients with existing config
        setupKubernetesClientsFromExistingConfig(kubeconfigPath);

        // Verify Tekton is already installed and ready
        verifyTektonInstallation();

        // Configure Jenkins global configuration
        configureJenkinsGlobal();
    }

    private void setupFromScratch() throws Exception {
        // Original setup logic
        setupKindCluster();
        setupKubernetesClients();
        installTekton();
        configureJenkinsGlobal();
    }

    private void setupKubernetesClientsFromExistingConfig(String kubeconfigPath) throws Exception {
        LOGGER.info("Setting up Kubernetes clients with existing config");

        try {
            // Read existing kubeconfig
            Config config;
            if (kubeconfigPath != null && !kubeconfigPath.isEmpty()) {
                try (FileInputStream fis = new FileInputStream(kubeconfigPath);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

                    StringBuilder kubeconfigContent = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        kubeconfigContent.append(line).append("\n");
                    }

                    config = Config.fromKubeconfig(kubeconfigContent.toString());
                }
            } else {
                // Fallback to default config
                config = new ConfigBuilder().build();
            }

            // Configure for Kind cluster
            config.setTrustCerts(true);
            config.setDisableHostnameVerification(true);

            kubernetesClient = new DefaultKubernetesClient(config);
            tektonClient = new DefaultTektonClient(config);

            // Test connectivity
            kubernetesClient.namespaces().list();
            LOGGER.info("Successfully connected to existing Kubernetes cluster");

            // Initialize TektonUtils
            TektonUtils.initializeKubeClients(config);

        } catch (Exception e) {
            LOGGER.severe("Failed to setup clients from existing config: " + e.getMessage());
            throw new RuntimeException("Failed to connect to pre-configured cluster", e);
        }
    }

    private void verifyTektonInstallation() throws Exception {
        LOGGER.info("Verifying Tekton installation");

        try {
            // Check if Tekton controller is running
            ProcessBuilder pb = new ProcessBuilder("kubectl", "get", "deployment",
                    "tekton-pipelines-controller", "-n", "tekton-pipelines");
            pb.environment().put("KUBECONFIG", System.getenv("KUBECONFIG"));
            Process process = pb.start();

            if (process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0) {
                LOGGER.info("Tekton controller deployment found");

                // Wait for it to be ready (but with shorter timeout since it should be ready)
                waitForExistingTekton();
            } else {
                throw new RuntimeException("Tekton controller deployment not found");
            }

        } catch (Exception e) {
            LOGGER.severe("Tekton verification failed: " + e.getMessage());
            throw new RuntimeException("Pre-installed Tekton is not ready", e);
        }
    }

    private void waitForExistingTekton() throws Exception {
        LOGGER.info("Waiting for pre-installed Tekton to be ready");

        // Shorter timeout since Tekton should already be installed
        for (int i = 0; i < 30; i++) { // 2.5 minutes max
            try {
                ProcessBuilder pb = new ProcessBuilder("kubectl", "get", "pods", "-n", "tekton-pipelines",
                        "--no-headers", "--field-selector=status.phase=Running");
                pb.environment().put("KUBECONFIG", System.getenv("KUBECONFIG"));
                Process process = pb.start();

                if (process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        int runningPods = 0;
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("Running")) {
                                runningPods++;
                            }
                        }

                        if (runningPods >= 2) {
                            LOGGER.info("Pre-installed Tekton is ready with " + runningPods + " running pods");
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Error checking pre-installed Tekton: " + e.getMessage());
            }

            Thread.sleep(5000);
        }

        throw new RuntimeException("Pre-installed Tekton did not become ready within timeout");
    }

    private void cleanupOldTestNamespaces() {
        try {
            if (kubernetesClient != null) {
                long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);

                kubernetesClient.namespaces().list().getItems().stream()
                        .filter(ns -> ns.getMetadata().getName().startsWith(TEST_NAMESPACE + "-"))
                        .filter(ns -> {
                            try {
                                String timestamp = ns.getMetadata().getName().substring(TEST_NAMESPACE.length() + 1);
                                long nsTime = Long.parseLong(timestamp);
                                return nsTime < oneHourAgo;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .forEach(ns -> {
                            try {
                                kubernetesClient.namespaces().delete(ns);
                                LOGGER.info("Cleaned up old namespace: " + ns.getMetadata().getName());
                            } catch (Exception e) {
                                LOGGER.warning("Failed to cleanup namespace: " + e.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to cleanup old namespaces: " + e.getMessage());
        }
    }

    @AfterAll
    public void tearDownE2EEnvironment() throws Exception {
        // Clean up test namespaces
        cleanupOldTestNamespaces();

        // Only cleanup if we created the cluster ourselves
        if (!isGitHubActionsWithPreSetup() && !"true".equals(System.getenv("SKIP_CLEANUP"))) {
            cleanupKindCluster();
        } else {
            LOGGER.info("Skipping cluster cleanup (using external cluster or SKIP_CLEANUP set)");
        }

        LOGGER.info("E2E test environment cleanup complete");
    }

    @AfterEach
    public void tearDownTestNamespace() throws Exception {
        // Clean up the current test namespace
        if (currentTestNamespace != null && kubernetesClient != null) {
            try {
                kubernetesClient.namespaces().withName(currentTestNamespace).delete();
                LOGGER.info("Cleaned up test namespace: " + currentTestNamespace);
            } catch (Exception e) {
                LOGGER.warning("Failed to cleanup test namespace: " + currentTestNamespace + " - " + e.getMessage());
            }
            currentTestNamespace = null;
        }
    }

    @BeforeEach
    public void setUpTestNamespace() throws Exception {
        // Create test namespace for each test
        String namespaceName = TEST_NAMESPACE + "-" + System.currentTimeMillis();
        Namespace testNamespace = new NamespaceBuilder()
                .withNewMetadata()
                .withName(namespaceName)
                .endMetadata()
                .build();

        try {
            kubernetesClient.namespaces().create(testNamespace);
            LOGGER.info("Created test namespace: " + namespaceName);

            // Store the current test namespace
            currentTestNamespace = namespaceName;

            // Wait for namespace to be ready
            Thread.sleep(1000);

            // Verify namespace was created
            Namespace createdNamespace = kubernetesClient.namespaces().withName(namespaceName).get();
            if (createdNamespace == null) {
                throw new RuntimeException("Failed to create test namespace: " + namespaceName);
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to create test namespace: " + e.getMessage());
            throw new RuntimeException("Failed to create test namespace: " + namespaceName, e);
        }
    }

    private void setupKindCluster() throws Exception {
        LOGGER.info("Setting up Kind cluster: " + KIND_CLUSTER_NAME);

        // Check if kind is installed
        if (!isCommandAvailable("kind")) {
            throw new RuntimeException(
                    "Kind is not installed. Please install kind first: https://kind.sigs.k8s.io/docs/user/quick-start/");
        }

        // Check if cluster already exists
        if (isKindClusterRunning()) {
            LOGGER.info("Kind cluster already exists, using existing cluster");
            return;
        }

        // Create kind cluster with custom config
        ProcessBuilder pb = new ProcessBuilder("kind", "create", "cluster",
                "--name", KIND_CLUSTER_NAME,
                "--config", "-");

        Process process = pb.start();

        // Write kind config to stdin
        String kindConfig = getKindConfig();
        process.getOutputStream().write(kindConfig.getBytes());
        process.getOutputStream().close();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // Get error output
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            String errorMsg = errorOutput.toString();
            if (errorMsg.contains("node(s) already exist") || errorMsg.contains("already exists")) {
                LOGGER.info("Kind cluster already exists (detected from error), using existing cluster");
                return;
            }

            throw new RuntimeException("Failed to create Kind cluster. Exit code: " + exitCode +
                    "\nError output: " + errorMsg);
        }

        // Wait for cluster to be ready
        waitForKindClusterReady();

        LOGGER.info("Kind cluster created successfully");
    }

    private void setupKubernetesClients() throws Exception {
        LOGGER.info("Setting up Kubernetes clients");

        // Write kubeconfig to a temporary file to avoid config parsing issues
        String tempDir = System.getProperty("java.io.tmpdir");
        String kubeconfigFile = tempDir + "/kind-kubeconfig-" + KIND_CLUSTER_NAME;

        // Export kubeconfig to a file
        ProcessBuilder pb = new ProcessBuilder("kind", "export", "kubeconfig",
                "--name", KIND_CLUSTER_NAME, "--kubeconfig", kubeconfigFile);
        Process process = pb.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to export kubeconfig. Exit code: " + exitCode);
        }

        LOGGER.info("Exported kubeconfig to: " + kubeconfigFile);

        // Set KUBECONFIG environment variable for kubectl commands
        System.setProperty("KUBECONFIG", kubeconfigFile);

        // Create Kubernetes clients using the exported config file
        Config config;
        try (FileInputStream fis = new FileInputStream(kubeconfigFile);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            StringBuilder kubeconfigContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                kubeconfigContent.append(line).append("\n");
            }

            config = Config.fromKubeconfig(kubeconfigContent.toString());

            // Configure for Kind cluster - trust self-signed certificates
            config.setTrustCerts(true);
            config.setDisableHostnameVerification(true);

            kubernetesClient = new DefaultKubernetesClient(config);
            tektonClient = new DefaultTektonClient(config);
        }

        // Initialize TektonUtils with the config
        TektonUtils.initializeKubeClients(config);

        LOGGER.info("Kubernetes clients configured successfully");
    }

    private void installTekton() throws Exception {
        LOGGER.info("Installing Tekton Pipelines version: " + TEKTON_VERSION);

        // Install Tekton Pipelines
        ProcessBuilder pb = new ProcessBuilder("kubectl", "apply", "-f",
                "https://storage.googleapis.com/tekton-releases/pipeline/previous/" + TEKTON_VERSION + "/release.yaml");
        setKubeconfigEnv(pb);

        Process process = pb.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // Get error output
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            String errorMsg = errorOutput.toString();
            if (errorMsg.contains("AlreadyExists") || errorMsg.contains("already exists")) {
                LOGGER.info("Tekton components already exist, continuing...");
            } else {
                throw new RuntimeException("Failed to install Tekton. Exit code: " + exitCode +
                        "\nError output: " + errorMsg);
            }
        }

        // Wait for Tekton controller to be ready
        waitForTektonReady();

        // Configure webhook for Kind cluster compatibility
        configureWebhookForKind();

        LOGGER.info("Tekton Pipelines installed successfully");
    }

    private void configureJenkinsGlobal() {
        LOGGER.info("Configuring Jenkins global Tekton settings");

        try {
            TektonGlobalConfiguration globalConfig = TektonGlobalConfiguration.get();

            if (globalConfig == null) {
                LOGGER.info("TektonGlobalConfiguration not available in test environment");
                LOGGER.info("E2E tests will use direct cluster specification instead");
                return;
            }

            List<ClusterConfig> clusterConfigs = new ArrayList<>();
            ClusterConfig kindClusterConfig = new ClusterConfig(
                    "kind-cluster",
                    tektonClient.getConfiguration().getMasterUrl(),
                    TEST_NAMESPACE);
            clusterConfigs.add(kindClusterConfig);

            globalConfig.setClusterConfigs(clusterConfigs);

            LOGGER.info("Jenkins global configuration completed");

        } catch (Exception e) {
            LOGGER.warning("Global configuration failed: " + e.getMessage());
            LOGGER.info("Continuing with direct cluster specification");
        }
    }

    private void setKubeconfigEnv(ProcessBuilder pb) {
        String kubeconfigFile = System.getProperty("KUBECONFIG");
        if (kubeconfigFile != null) {
            pb.environment().put("KUBECONFIG", kubeconfigFile);
        }
    }

    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", command);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isKindClusterRunning() {
        try {
            ProcessBuilder pb = new ProcessBuilder("kind", "get", "clusters");
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().equals(KIND_CLUSTER_NAME)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void waitForKindClusterReady() throws Exception {
        LOGGER.info("Waiting for Kind cluster to be ready...");

        for (int i = 0; i < 60; i++) { // Wait up to 5 minutes
            try {
                ProcessBuilder pb = new ProcessBuilder("kubectl", "get", "nodes", "--no-headers");
                setKubeconfigEnv(pb);
                Process process = pb.start();

                if (process.waitFor() == 0) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line = reader.readLine();
                        if (line != null && line.contains("Ready")) {
                            LOGGER.info("Kind cluster is ready");
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore and retry
            }

            Thread.sleep(5000); // Wait 5 seconds
        }

        throw new RuntimeException("Kind cluster did not become ready within timeout");
    }

    private void waitForTektonReady() throws Exception {
        LOGGER.info("Waiting for Tekton to be ready...");

        for (int i = 0; i < 180; i++) { // Wait up to 5 minutes
            try {
                ProcessBuilder pb = new ProcessBuilder("kubectl", "get", "pods", "-n", "tekton-pipelines",
                        "--no-headers", "--field-selector=status.phase=Running");
                setKubeconfigEnv(pb);
                Process process = pb.start();

                if (process.waitFor() == 0) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        int runningPods = 0;
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("Running")) {
                                runningPods++;
                            }
                        }

                        if (runningPods >= 2) { // Controller and webhook
                            // Additional check: ensure webhook is responding
                            if (isWebhookReady()) {
                                LOGGER.info("Tekton is ready");
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore and retry
            }

            Thread.sleep(5000); // Wait 5 seconds
        }

        throw new RuntimeException("Tekton did not become ready within timeout");
    }

    private boolean isWebhookReady() {
        try {
            // Check if webhook service is available
            ProcessBuilder pb = new ProcessBuilder("kubectl", "get", "service", "-n", "tekton-pipelines",
                    "tekton-pipelines-webhook", "-o", "jsonpath={.spec.clusterIP}");
            setKubeconfigEnv(pb);
            Process process = pb.start();

            if (process.waitFor() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String clusterIP = reader.readLine();
                    if (clusterIP != null && !clusterIP.trim().isEmpty()) {
                        LOGGER.info("Webhook service cluster IP: " + clusterIP);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to check webhook readiness: " + e.getMessage());
        }
        return false;
    }

    private void configureWebhookForKind() {
        try {
            LOGGER.info("Configuring Tekton webhook for Kind cluster compatibility...");

            // Wait a bit more for webhook to be fully ready
            Thread.sleep(10000);

            // Restart webhook deployment to ensure proper networking
            ProcessBuilder pb = new ProcessBuilder("kubectl", "rollout", "restart", "deployment",
                    "tekton-pipelines-webhook", "-n", "tekton-pipelines");
            setKubeconfigEnv(pb);
            Process process = pb.start();

            if (process.waitFor() == 0) {
                LOGGER.info("Restarted webhook deployment");

                // Wait for rollout to complete
                pb = new ProcessBuilder("kubectl", "rollout", "status", "deployment",
                        "tekton-pipelines-webhook", "-n", "tekton-pipelines", "--timeout=120s");
                setKubeconfigEnv(pb);
                process = pb.start();

                if (process.waitFor() == 0) {
                    LOGGER.info("Webhook deployment rollout completed");
                } else {
                    LOGGER.warning("Webhook deployment rollout may not have completed properly");
                }
            } else {
                LOGGER.warning("Failed to restart webhook deployment");
            }

        } catch (Exception e) {
            LOGGER.warning("Failed to configure webhook for Kind: " + e.getMessage());
        }
    }

    private void cleanupKindCluster() throws Exception {
        LOGGER.info("Cleaning up Kind cluster");

        try {
            ProcessBuilder pb = new ProcessBuilder("kind", "delete", "cluster", "--name", KIND_CLUSTER_NAME);
            Process process = pb.start();
            process.waitFor(30, TimeUnit.SECONDS); // Don't wait too long for cleanup
        } catch (Exception e) {
            LOGGER.warning("Failed to cleanup Kind cluster: " + e.getMessage());
        }
    }

    private String getKindConfig() {
        return """
                kind: Cluster
                apiVersion: kind.x-k8s.io/v1alpha4
                nodes:
                - role: control-plane
                  extraPortMappings:
                  - containerPort: 30080
                    hostPort: 30080
                    protocol: TCP
                  - containerPort: 30443
                    hostPort: 30443
                    protocol: TCP
                networking:
                  apiServerAddress: "127.0.0.1"
                  apiServerPort: 6443
                """;
    }

    protected String getCurrentTestNamespace() {
        // Return the stored current test namespace
        if (currentTestNamespace != null) {
            return currentTestNamespace;
        }

        // Fallback to finding the namespace (for backward compatibility)
        return kubernetesClient.namespaces().list().getItems().stream()
                .filter(ns -> ns.getMetadata().getName().startsWith(TEST_NAMESPACE + "-"))
                .findFirst()
                .map(ns -> ns.getMetadata().getName())
                .orElse(TEST_NAMESPACE);
    }
}