package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ArrayOrString;
import io.fabric8.tekton.pipeline.v1beta1.Param;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunSpec;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.jenkins.plugins.checks.api.ChecksConclusion;
import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksOutput;
import io.jenkins.plugins.checks.api.ChecksPublisher;
import io.jenkins.plugins.checks.api.ChecksPublisherFactory;
import io.jenkins.plugins.checks.api.ChecksStatus;

import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.LogUtils;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils.TektonResourceType;
import org.waveywaves.jenkins.plugins.tekton.client.ToolUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;
import org.waveywaves.jenkins.plugins.tekton.client.logwatch.PipelineRunLogWatch;
import org.waveywaves.jenkins.plugins.tekton.client.logwatch.TaskRunLogWatch;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.waveywaves.jenkins.plugins.tekton.client.global.TektonGlobalConfiguration;
import org.waveywaves.jenkins.plugins.tekton.client.global.ClusterConfig;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import jenkins.model.Jenkins;

public class CreateRaw extends BaseStep {
    private static final Logger LOGGER = Logger.getLogger(CreateRaw.class.getName());
    /** Default namespace when none is specified (CI-safe fallback). Package visibility for tests. */
    static final String DEFAULT_NAMESPACE = "default";

    private final String input;
    private final String inputType;
    private String namespace;
    private String clusterName;
    private boolean enableCatalog;

    private transient PrintStream consoleLogger;
    private transient ClassLoader toolClassLoader;
    private transient ChecksPublisher checksPublisher;

    @DataBoundConstructor
    public CreateRaw(String input, String inputType) {
        super();
        this.inputType = inputType;
        this.input = input;
        setKubernetesClient(TektonUtils.getKubernetesClient(getClusterName()));
        setTektonClient(TektonUtils.getTektonClient(getClusterName()));
    }

    @DataBoundSetter
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @DataBoundSetter
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
        setKubernetesClient(TektonUtils.getKubernetesClient(getClusterName()));
        setTektonClient(TektonUtils.getTektonClient(getClusterName()));
    }

    @DataBoundSetter
    public void setEnableCatalog(boolean enableCatalog) {
        this.enableCatalog = enableCatalog;
    }

    protected ClassLoader getToolClassLoader() {
        if (toolClassLoader == null) {
            toolClassLoader = ToolUtils.class.getClassLoader();
        }
        return toolClassLoader;
    }

    /**
     * Only exposed for testing so that we can use a test class loader to load test tools
     *
     * @param toolClassLoader
     */
    protected void setToolClassLoader(ClassLoader toolClassLoader) {
        this.toolClassLoader = toolClassLoader;
    }

    /**
     * Resolves the namespace to use for Tekton resources using a priority hierarchy:
     * 1. Explicit namespace from the step configuration
     * 2. Default namespace from Global Plugin Configuration
     * 3. Namespace from current KubeConfig context (if running on Minikube or similar)
     * 4. Hard fallback to "default" namespace
     *
     * @return the resolved namespace, or null if the resource already has one
     */
    private String resolveNamespace(String resourceNamespace) {
        // If the resource already has a namespace, respect it
        if (!Strings.isNullOrEmpty(resourceNamespace)) {
            return resourceNamespace;
        }

        // Priority 1: Check explicit namespace from step configuration
        if (!Strings.isNullOrEmpty(this.namespace)) {
            return this.namespace;
        }

        // Priority 2: Check Global Plugin Configuration for default namespace
        // CI-safe: only access global config when Jenkins is running (avoid NPE in headless JUnit)
        if (Jenkins.getInstanceOrNull() != null) {
            TektonGlobalConfiguration globalConfig = TektonGlobalConfiguration.get();
            if (globalConfig != null) {
                for (ClusterConfig cc : globalConfig.getClusterConfigs()) {
                    if (cc.getName().equals(getClusterName()) && !Strings.isNullOrEmpty(cc.getDefaultNamespace())) {
                        return cc.getDefaultNamespace();
                    }
                }
            }
        }

        // Priority 3: Try to get namespace from KubeConfig context (via KubernetesClient)
        if (kubernetesClient != null) {
            try {
                Config config = ((io.fabric8.kubernetes.client.KubernetesClient) kubernetesClient).getConfiguration();
                if (config != null && !Strings.isNullOrEmpty(config.getNamespace())) {
                    return config.getNamespace();
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to get namespace from KubeConfig: " + e.getMessage());
            }
        }

        // Priority 4: Hard fallback to "default" namespace
        return DEFAULT_NAMESPACE;
    }

    public void setChecksPublisher(ChecksPublisher checksPublisher) {
        this.checksPublisher = checksPublisher;
    }

    // the getters must be public to work with the Configure page...
    public String getInput() {
        return this.input;
    }

    public String getInputType() {
        return this.inputType;
    }

    public boolean isEnableCatalog() {
        return enableCatalog;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getClusterName() {
        if (Strings.isNullOrEmpty(clusterName)) {
            clusterName = TektonUtils.DEFAULT_CLIENT_KEY;
        }
        return clusterName;
    }

    protected String createWithResourceSpecificClient(TektonResourceType resourceType, InputStream inputStream, EnvVars envVars) throws Exception {
        switch (resourceType) {
            case task:
                return createTask(inputStream);
            case taskrun:
                return createTaskRun(inputStream);
            case pipeline:
                return createPipeline(inputStream);
            case pipelinerun:
                return createPipelineRun(inputStream, envVars);
            default:
                return "";
        }
    }

    public String createTaskRun(InputStream inputStream) throws Exception {
        byte[] data = ByteStreams.toByteArray(inputStream);
        String apiVersion = TektonUtils.getApiVersionFromData(data);
        if (apiVersion == null) {
            apiVersion = "v1beta1";
        }
        if (!"v1".equals(apiVersion) && !"v1beta1".equals(apiVersion)) {
            throw new AbortException("Unsupported Tekton API version: " + apiVersion + ". Supported versions are v1 and v1beta1.");
        }
        if ("v1".equals(apiVersion)) {
            return createTaskRunV1(data);
        }
        return createTaskRunV1Beta1(data);
    }

    public String createTask(InputStream inputStream) throws Exception {
        byte[] data = ByteStreams.toByteArray(inputStream);
        String apiVersion = TektonUtils.getApiVersionFromData(data);
        if (apiVersion == null) {
            apiVersion = "v1beta1";
        }
        if (!"v1".equals(apiVersion) && !"v1beta1".equals(apiVersion)) {
            throw new AbortException("Unsupported Tekton API version: " + apiVersion + ". Supported versions are v1 and v1beta1.");
        }
        if ("v1".equals(apiVersion)) {
            return createTaskV1(data);
        }
        return createTaskV1Beta1(data);
    }

    public String createPipeline(InputStream inputStream) throws Exception {
        byte[] data = ByteStreams.toByteArray(inputStream);
        String apiVersion = TektonUtils.getApiVersionFromData(data);
        if (apiVersion == null) {
            apiVersion = "v1beta1";
        }
        if (!"v1".equals(apiVersion) && !"v1beta1".equals(apiVersion)) {
            throw new AbortException("Unsupported Tekton API version: " + apiVersion + ". Supported versions are v1 and v1beta1.");
        }
        if ("v1".equals(apiVersion)) {
            return createPipelineV1(data);
        }
        return createPipelineV1Beta1(data);
    }

    private String createTaskRunV1(byte[] data) throws Exception {
        KubernetesClient kc = (KubernetesClient) kubernetesClient;
        if (kc == null) {
            throw new AbortException("Kubernetes client is not available. Check Jenkins global configuration for the cluster.");
        }
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = yamlMapper.readTree(data);
        if (!root.isObject()) {
            throw new AbortException("TaskRun manifest is invalid: expected a YAML object.");
        }
        ObjectNode rootObj = (ObjectNode) root;
        JsonNode metadata = rootObj.get("metadata");
        if (metadata == null || !metadata.isObject()) {
            throw new AbortException("TaskRun manifest is invalid: missing 'metadata'.");
        }
        ObjectNode metadataObj = (ObjectNode) metadata;
        String resourceNamespace = metadataObj.has("namespace") ? metadataObj.get("namespace").asText(null) : null;
        String resolvedNamespace = resolveNamespace(resourceNamespace);
        metadataObj.put("namespace", resolvedNamespace);
        if (Strings.isNullOrEmpty(resourceNamespace)) {
            LOGGER.info("No namespace specified in TaskRun manifest (v1), using resolved namespace: " + resolvedNamespace);
            logMessage("Using namespace: " + resolvedNamespace);
        }
        byte[] enhancedBytes = yamlMapper.writeValueAsBytes(rootObj);
        LOGGER.info("Creating TaskRun (tekton.dev/v1) in namespace " + resolvedNamespace);
        Resource<HasMetadata> resource = kc.resource(new ByteArrayInputStream(enhancedBytes));
        HasMetadata created = resource.inNamespace(resolvedNamespace).create();
        if (created == null || created.getMetadata() == null) {
            throw new AbortException("Failed to create TaskRun (v1): no resource returned.");
        }
        String resourceName = created.getMetadata().getName() != null ? created.getMetadata().getName() : created.getMetadata().getGenerateName();
        LOGGER.info("TaskRun (v1) created: " + resourceName + ". Log streaming is not supported for v1.");
        return resourceName != null ? resourceName : "taskrun-v1";
    }

    private String createTaskRunV1Beta1(byte[] data) throws Exception {
        if (taskRunClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setTaskRunClient(tc.v1beta1().taskRuns());
        }
        TaskRun taskrun = taskRunClient.load(new ByteArrayInputStream(data)).get();
        String resourceNamespace = taskrun.getMetadata().getNamespace();
        String resolvedNamespace = resolveNamespace(resourceNamespace);
        if (Strings.isNullOrEmpty(resourceNamespace)) {
            LOGGER.info("No namespace specified in TaskRun manifest, using resolved namespace: " + resolvedNamespace);
            taskrun.getMetadata().setNamespace(resolvedNamespace);
            logMessage("Using namespace: " + resolvedNamespace);
        }
        taskrun = taskRunClient.inNamespace(resolvedNamespace).create(taskrun);
        String resourceName = taskrun.getMetadata().getName();
        streamTaskRunLogsToConsole(taskrun);
        return resourceName;
    }

    private String createTaskV1(byte[] data) throws Exception {
        KubernetesClient kc = (KubernetesClient) kubernetesClient;
        if (kc == null) {
            throw new AbortException("Kubernetes client is not available. Check Jenkins global configuration for the cluster.");
        }
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = yamlMapper.readTree(data);
        if (!root.isObject()) {
            throw new AbortException("Task manifest is invalid: expected a YAML object.");
        }
        ObjectNode rootObj = (ObjectNode) root;
        JsonNode metadata = rootObj.get("metadata");
        if (metadata == null || !metadata.isObject()) {
            throw new AbortException("Task manifest is invalid: missing 'metadata'.");
        }
        ObjectNode metadataObj = (ObjectNode) metadata;
        String resourceNamespace = metadataObj.has("namespace") ? metadataObj.get("namespace").asText(null) : null;
        String resolvedNamespace = resolveNamespace(resourceNamespace);
        metadataObj.put("namespace", resolvedNamespace);
        if (Strings.isNullOrEmpty(resourceNamespace)) {
            LOGGER.info("No namespace specified in Task manifest (v1), using resolved namespace: " + resolvedNamespace);
            logMessage("Using namespace: " + resolvedNamespace);
        }
        byte[] enhancedBytes = yamlMapper.writeValueAsBytes(rootObj);
        LOGGER.info("Creating Task (tekton.dev/v1) in namespace " + resolvedNamespace);
        Resource<HasMetadata> resource = kc.resource(new ByteArrayInputStream(enhancedBytes));
        HasMetadata created = resource.inNamespace(resolvedNamespace).create();
        if (created == null || created.getMetadata() == null) {
            throw new AbortException("Failed to create Task (v1): no resource returned.");
        }
        return created.getMetadata().getName();
    }

    private String createTaskV1Beta1(byte[] data) {
        if (taskClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setTaskClient(tc.v1beta1().tasks());
        }
        Task task = taskClient.load(new ByteArrayInputStream(data)).get();
        String resourceNamespace = task.getMetadata().getNamespace();
        String resolvedNamespace = resolveNamespace(resourceNamespace);
        if (Strings.isNullOrEmpty(resourceNamespace)) {
            LOGGER.info("No namespace specified in Task manifest, using resolved namespace: " + resolvedNamespace);
            task.getMetadata().setNamespace(resolvedNamespace);
            logMessage("Using namespace: " + resolvedNamespace);
        }
        task = taskClient.inNamespace(resolvedNamespace).create(task);
        return task.getMetadata().getName();
    }

    private String createPipelineV1(byte[] data) throws Exception {
        KubernetesClient kc = (KubernetesClient) kubernetesClient;
        if (kc == null) {
            throw new AbortException("Kubernetes client is not available. Check Jenkins global configuration for the cluster.");
        }
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = yamlMapper.readTree(data);
        if (!root.isObject()) {
            throw new AbortException("Pipeline manifest is invalid: expected a YAML object.");
        }
        ObjectNode rootObj = (ObjectNode) root;
        JsonNode metadata = rootObj.get("metadata");
        if (metadata == null || !metadata.isObject()) {
            throw new AbortException("Pipeline manifest is invalid: missing 'metadata'.");
        }
        ObjectNode metadataObj = (ObjectNode) metadata;
        String resourceNamespace = metadataObj.has("namespace") ? metadataObj.get("namespace").asText(null) : null;
        String resolvedNamespace = resolveNamespace(resourceNamespace);
        metadataObj.put("namespace", resolvedNamespace);
        if (Strings.isNullOrEmpty(resourceNamespace)) {
            LOGGER.info("No namespace specified in Pipeline manifest (v1), using resolved namespace: " + resolvedNamespace);
            logMessage("Using namespace: " + resolvedNamespace);
        }
        byte[] enhancedBytes = yamlMapper.writeValueAsBytes(rootObj);
        LOGGER.info("Creating Pipeline (tekton.dev/v1) in namespace " + resolvedNamespace);
        Resource<HasMetadata> resource = kc.resource(new ByteArrayInputStream(enhancedBytes));
        HasMetadata created = resource.inNamespace(resolvedNamespace).create();
        if (created == null || created.getMetadata() == null) {
            throw new AbortException("Failed to create Pipeline (v1): no resource returned.");
        }
        return created.getMetadata().getName();
    }

    private String createPipelineV1Beta1(byte[] data) {
        if (pipelineClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setPipelineClient(tc.v1beta1().pipelines());
        }
        Pipeline pipeline = pipelineClient.load(new ByteArrayInputStream(data)).get();
        String resourceNamespace = pipeline.getMetadata().getNamespace();
        String resolvedNamespace = resolveNamespace(resourceNamespace);
        if (Strings.isNullOrEmpty(resourceNamespace)) {
            LOGGER.info("No namespace specified in Pipeline manifest, using resolved namespace: " + resolvedNamespace);
            pipeline.getMetadata().setNamespace(resolvedNamespace);
            logMessage("Using namespace: " + resolvedNamespace);
        }
        pipeline = pipelineClient.inNamespace(resolvedNamespace).create(pipeline);
        return pipeline.getMetadata().getName();
    }

    public String createPipelineRun(InputStream inputStream, EnvVars envVars) throws Exception {
        byte[] data = ByteStreams.toByteArray(inputStream);
        String apiVersion = TektonUtils.getApiVersionFromData(data);
        if (apiVersion == null) {
            apiVersion = "v1beta1";
        }
        if (!"v1".equals(apiVersion) && !"v1beta1".equals(apiVersion)) {
            throw new AbortException("Unsupported Tekton API version: " + apiVersion + ". Supported versions are v1 and v1beta1.");
        }

        if ("v1".equals(apiVersion)) {
            return createPipelineRunV1(data, envVars);
        }

        return createPipelineRunV1Beta1(data, envVars);
    }

    /**
     * Create PipelineRun using the generic Kubernetes client so the request uses the manifest's apiVersion (tekton.dev/v1).
     * Applies namespace fallback and env params to the YAML, then creates. Log streaming is not performed for v1.
     */
    private String createPipelineRunV1(byte[] data, EnvVars envVars) throws Exception {
        KubernetesClient kc = (KubernetesClient) kubernetesClient;
        if (kc == null) {
            throw new AbortException("Kubernetes client is not available. Check Jenkins global configuration for the cluster.");
        }
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = yamlMapper.readTree(data);
        if (!root.isObject()) {
            throw new AbortException("PipelineRun manifest is invalid: expected a YAML object.");
        }
        ObjectNode rootObj = (ObjectNode) root;
        JsonNode metadata = rootObj.get("metadata");
        if (metadata == null || !metadata.isObject()) {
            throw new AbortException("PipelineRun manifest is invalid: missing 'metadata'. Ensure your YAML has a metadata section (e.g. name, namespace).");
        }
        ObjectNode metadataObj = (ObjectNode) metadata;
        String resourceNamespace = metadataObj.has("namespace") ? metadataObj.get("namespace").asText(null) : null;
        String resolvedNamespace = resolveNamespace(resourceNamespace);
        metadataObj.put("namespace", resolvedNamespace);
        if (Strings.isNullOrEmpty(resourceNamespace)) {
            LOGGER.info("No namespace specified in PipelineRun manifest (v1), using resolved namespace: " + resolvedNamespace);
            logMessage("Using namespace: " + resolvedNamespace);
        }
        enhancePipelineRunSpecWithEnvVars(rootObj, envVars);
        byte[] enhancedBytes = yamlMapper.writeValueAsBytes(rootObj);
        LOGGER.info("Creating PipelineRun (tekton.dev/v1)\n" + new String(enhancedBytes, StandardCharsets.UTF_8));
        Resource<HasMetadata> resource = kc.resource(new ByteArrayInputStream(enhancedBytes));
        HasMetadata created = resource.inNamespace(resolvedNamespace).create();
        if (created == null || created.getMetadata() == null) {
            throw new AbortException("Failed to create PipelineRun (v1): no resource returned.");
        }
        String resourceName = created.getMetadata().getName();
        if (checksPublisher != null) {
            ChecksDetails checkDetails = new ChecksDetails.ChecksDetailsBuilder()
                    .withName("tekton")
                    .withOutput(new ChecksOutput.ChecksOutputBuilder()
                            .withTitle(resourceName)
                            .withSummary("PipelineRun (v1) is running...")
                            .build())
                    .withStartedAt(LocalDateTime.now())
                    .withStatus(ChecksStatus.IN_PROGRESS)
                    .withConclusion(ChecksConclusion.NONE)
                    .build();
            checksPublisher.publish(checkDetails);
        }
        LOGGER.info("PipelineRun (v1) created: " + resourceName + ". Log streaming is not supported for v1; check cluster for status.");
        return resourceName;
    }

    /**
     * Adds Jenkins env vars as params on spec.params for a generic PipelineRun YAML (ObjectNode). Used for v1.
     */
    private void enhancePipelineRunSpecWithEnvVars(ObjectNode specRoot, EnvVars envVars) {
        JsonNode specNode = specRoot.get("spec");
        if (specNode == null || !specNode.isObject()) {
            return;
        }
        ObjectNode spec = (ObjectNode) specNode;
        ArrayNode params = spec.has("params") && spec.get("params").isArray() ? (ArrayNode) spec.get("params") : spec.putArray("params");
        setParamOnSpec(params, "BUILD_ID", envVars.get("BUILD_ID"));
        setParamOnSpec(params, "JOB_NAME", envVars.get("JOB_NAME"));
        setParamOnSpec(params, "PULL_PULL_SHA", envVars.get("GIT_COMMIT"));
        String gitBranch = envVars.get("GIT_BRANCH");
        setParamOnSpec(params, "PULL_BASE_REF", gitBranch != null && gitBranch.contains("/") ? gitBranch.split("/")[gitBranch.split("/").length - 1] : gitBranch);
        setParamOnSpec(params, "REPO_URL", envVars.get("GIT_URL"));
        String gitUrlString = envVars.get("GIT_URL");
        if (StringUtils.isNotEmpty(gitUrlString)) {
            try {
                URL gitUrl = new URL(gitUrlString);
                String[] parts = gitUrl.getPath().split("/");
                setParamOnSpec(params, "REPO_OWNER", parts.length > 1 ? parts[1] : "");
                setParamOnSpec(params, "REPO_NAME", parts.length > 2 ? removeGitSuffix(parts[2]) : "");
            } catch (MalformedURLException e) {
                setParamOnSpec(params, "REPO_OWNER", "");
                setParamOnSpec(params, "REPO_NAME", "");
            }
        }
    }

    private void setParamOnSpec(ArrayNode params, String name, String value) {
        if (value == null) {
            value = "";
        }
        for (int i = 0; i < params.size(); i++) {
            JsonNode p = params.get(i);
            if (p.has("name") && name.equals(p.get("name").asText())) {
                ((ObjectNode) p).put("value", value);
                return;
            }
        }
        ObjectNode param = params.addObject();
        param.put("name", name);
        param.put("value", value);
    }

    private String createPipelineRunV1Beta1(byte[] data, EnvVars envVars) throws Exception {
        if (pipelineRunClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setPipelineRunClient(tc.v1beta1().pipelineRuns());
        }
        String resourceName;
        final PipelineRun pipelineRun = pipelineRunClient.load(new ByteArrayInputStream(data)).get();
        if (pipelineRun == null) {
            throw new AbortException("Failed to load PipelineRun from input: parsed resource is null. Check that the YAML/JSON is valid and contains a PipelineRun.");
        }
        if (pipelineRun.getMetadata() == null) {
            throw new AbortException("PipelineRun manifest is invalid: missing 'metadata'. Ensure your YAML has a metadata section (e.g. name, namespace).");
        }
        if (pipelineRun.getSpec() == null) {
            throw new AbortException("PipelineRun manifest is invalid: missing 'spec'. Ensure your YAML has a spec section.");
        }

        String resourceNamespace = pipelineRun.getMetadata().getNamespace();
        String resolvedNamespace = resolveNamespace(resourceNamespace);

        if (Strings.isNullOrEmpty(resourceNamespace)) {
            LOGGER.info("No namespace specified in PipelineRun manifest, using resolved namespace: " + resolvedNamespace);
            pipelineRun.getMetadata().setNamespace(resolvedNamespace);
            logMessage("Using namespace: " + resolvedNamespace);
        }

        LOGGER.info("Using environment variables " + envVars);
        enhancePipelineRunWithEnvVars(pipelineRun, envVars);
        LOGGER.info("Creating PipelineRun\n" + marshall(pipelineRun));

        PipelineRun updatedPipelineRun = pipelineRunClient.inNamespace(resolvedNamespace).create(pipelineRun);
        resourceName = updatedPipelineRun.getMetadata().getName();

        ChecksDetails checkDetails = new ChecksDetails.ChecksDetailsBuilder()
                .withName("tekton")
                .withOutput(new ChecksOutput.ChecksOutputBuilder()
                        .withTitle(updatedPipelineRun.getMetadata().getName())
                        .withSummary("PipelineRun is running...")
                        .build())
                .withStartedAt(LocalDateTime.now())
                .withStatus(ChecksStatus.IN_PROGRESS)
                .withConclusion(ChecksConclusion.NONE)
                .build();
        checksPublisher.publish(checkDetails);

        streamPipelineRunLogsToConsole(updatedPipelineRun);

        PipelineRun reloaded = pipelineRunClient.inNamespace(resolvedNamespace).withName(resourceName).get();
        List<Condition> conditions = reloaded.getStatus().getConditions();
        Optional<Condition> succeeded = conditions.stream()
                .filter(c -> c.getType().equalsIgnoreCase("Succeeded"))
                .findFirst();

        if (succeeded.isPresent() && succeeded.get().getStatus().equalsIgnoreCase("false")) {
            throw new Exception(succeeded.get().getReason() + ": " + succeeded.get().getMessage());
        }

        return resourceName;
    }

    protected void enhancePipelineRunWithEnvVars(PipelineRun pr, EnvVars envVars) {
        setParamOnPipelineRunSpec(pr.getSpec(), "BUILD_ID", envVars.get("BUILD_ID"));
        setParamOnPipelineRunSpec(pr.getSpec(), "JOB_NAME", envVars.get("JOB_NAME"));
        setParamOnPipelineRunSpec(pr.getSpec(), "PULL_PULL_SHA", envVars.get("GIT_COMMIT"));

        String gitBranch = envVars.get("GIT_BRANCH");
        if (StringUtils.isNotEmpty(gitBranch)) {
            String[] gitBranchParts = gitBranch.split("/");
            setParamOnPipelineRunSpec(pr.getSpec(), "PULL_BASE_REF", gitBranchParts[gitBranchParts.length - 1]);
        }

        String gitUrlString = envVars.get("GIT_URL");
        if (StringUtils.isNotEmpty(gitUrlString)) {
            URL gitUrl = null;
            try {
                gitUrl = new URL(gitUrlString);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            String[] parts = gitUrl.getPath().split("/");
            setParamOnPipelineRunSpec(pr.getSpec(), "REPO_URL", gitUrl.toString());
            setParamOnPipelineRunSpec(pr.getSpec(), "REPO_OWNER", parts[1]);
            setParamOnPipelineRunSpec(pr.getSpec(), "REPO_NAME", removeGitSuffix(parts[2]));
        }
    }

    private String removeGitSuffix(String part) {
        if (part.endsWith(".git")) {
            return part.replaceAll("\\.git$", "");
        }
        return part;
    }

    private void setParamOnPipelineRunSpec(@NonNull PipelineRunSpec spec, String paramName, String paramValue) {
        if (paramValue == null) {
            paramValue = "";
        }
        if (spec.getParams() == null) {
            spec.setParams(new ArrayList<>());
        }
        Optional<Param> param = spec.getParams().stream().filter(p -> p.getName().equals(paramName)).findAny();
        if (param.isPresent()) {
            param.get().setValue(new ArrayOrString(paramValue));
        } else {
            spec.getParams().add(new Param(paramName, new ArrayOrString(paramValue)));
        }
    }

    public void streamTaskRunLogsToConsole(TaskRun taskRun) throws Exception {
        KubernetesClient kc = (KubernetesClient) kubernetesClient;
        TektonClient tc = (TektonClient) tektonClient;
        Thread logWatchTask = null;
        TaskRunLogWatch logWatch = new TaskRunLogWatch(kc, tc, taskRun, consoleLogger);
        logWatchTask = new Thread(logWatch);
        logWatchTask.start();
        logWatchTask.join();
        Exception e = logWatch.getException();
        if (e != null) {
            throw e;
        }
    }

    public void streamPipelineRunLogsToConsole(PipelineRun pipelineRun) throws Exception {
        KubernetesClient kc = (KubernetesClient) kubernetesClient;
        TektonClient tc = (TektonClient) tektonClient;
        Thread logWatchTask;
        PipelineRunLogWatch logWatch = new PipelineRunLogWatch(kc, tc, pipelineRun, consoleLogger);
        logWatchTask = new Thread(logWatch);
        logWatchTask.start();
        logWatchTask.join();
        Exception e = logWatch.getException();
        if (e != null) {
            throw e;
        }
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars envVars, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        consoleLogger = listener.getLogger();

        String clusterName = getClusterName();
        LOGGER.info("connecting using cluster name " + clusterName);

        // lets make sure the clients are not empty
        if (tektonClient == null) {
            setTektonClient(TektonUtils.getTektonClient(clusterName));
            if (this.tektonClient == null) {
                throw new IOException("no tektonClient for cluster " + clusterName);
            }
        }
        if (kubernetesClient == null) {
            setKubernetesClient(TektonUtils.getKubernetesClient(clusterName));
            if (this.kubernetesClient == null) {
                throw new IOException("no kubernetesClient for cluster " + clusterName);
            }
        }

        if (checksPublisher == null) {
            checksPublisher = ChecksPublisherFactory.fromRun(run, listener);
        }

        runCreate(run, workspace, envVars);
    }

    protected String runCreate(Run<?, ?> run, FilePath workspace, EnvVars envVars) {
        URL url = null;
        byte[] data = null;
        String inputData = this.getInput();
        String inputType = this.getInputType();
        String createdResourceName = "";
        TektonResourceType resourceType = null;
        try {
            if (inputType.equals(InputType.URL.toString())) {
                url = new URL(inputData);
                data = Resources.toByteArray(url);
            } else if (inputType.equals(InputType.YAML.toString())) {
                data = inputData.getBytes(StandardCharsets.UTF_8);
            } else if (inputType.equals(InputType.FILE.toString())) {
                FilePath inputFile = workspace.child(inputData);
                LOGGER.info("Reading from " + inputFile + ", exists:" + inputFile.exists());
                data = ByteStreams.toByteArray(inputFile.read());
            }

            if (data != null) {
                LOGGER.info("Got data before enhancement\n" + new String(data, StandardCharsets.UTF_8));
            }

            data = convertTektonData(workspace, envVars, null, data);
            if (data != null) {
                List<TektonResourceType> kind = TektonUtils.getKindFromInputStream(new ByteArrayInputStream(data), this.getInputType());
                if (kind.size() > 1){
                    LOGGER.warning("Multiple Objects in YAML not supported yet");
                    logMessage("Multiple Objects in YAML not supported yet");
                    run.setResult(Result.FAILURE);
                } else {
                    resourceType = kind.get(0);
                    LOGGER.info("creating kind " + resourceType.name());
                    createdResourceName = createWithResourceSpecificClient(resourceType, new ByteArrayInputStream(data), envVars);
                }
            }

            // only recording checks for pipelineruns
            if (resourceType != null && resourceType == TektonResourceType.pipelinerun) {
                ChecksDetails checkDetails = new ChecksDetails.ChecksDetailsBuilder()
                        .withName("tekton")
                        .withOutput(new ChecksOutput.ChecksOutputBuilder()
                                .withTitle(createdResourceName)
                                .withSummary("PipelineRun completed")
                                .build())
                        .withCompletedAt(LocalDateTime.now())
                        .withStatus(ChecksStatus.COMPLETED)
                        .withConclusion(ChecksConclusion.SUCCESS)
                        .build();
                checksPublisher.publish(checkDetails);
            }
        } catch (Throwable e) {
            logMessage("Failed: " + e.getMessage());
            StringWriter buffer = new StringWriter();
            PrintWriter writer = new PrintWriter(buffer);
            e.printStackTrace(writer);
            writer.close();
            logMessage(buffer.toString());

            LOGGER.warning("Caught: " + e.toString());
            e.printStackTrace();

            run.setResult(Result.FAILURE);

            // only recording checks for pipelineruns
            if (resourceType != null && resourceType == TektonResourceType.pipelinerun) {
                ChecksDetails checkDetails = new ChecksDetails.ChecksDetailsBuilder()
                        .withName("tekton")
                        .withStatus(ChecksStatus.COMPLETED)
                        .withConclusion(ChecksConclusion.FAILURE)
                        .withOutput(new ChecksOutput.ChecksOutputBuilder()
                                .withTitle(createdResourceName)
                                .withSummary("PipelineRun Failed")
                                .withText(buffer.toString())
                                .build())
                        .withDetailsURL(DisplayURLProvider.get().getRunURL(run))
                        .withCompletedAt(LocalDateTime.now(ZoneOffset.UTC))
                        .build();

                checksPublisher.publish(checkDetails);
            }
        }
        return createdResourceName;
    }

    protected void logMessage(String text) {
        synchronized (this.consoleLogger) {
            try {
                this.consoleLogger.write((text + "\n").getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                LOGGER.warning("failed to log to console: " + e);
            }
        }
    }

    /**
     * Performs any conversion on the Tekton resources before we apply it to Kubernetes
     */
    private byte[] convertTektonData(FilePath workspace, EnvVars envVars, File inputFile, byte[] data) throws Exception {
        if (enableCatalog) {
            // lets use the workspace relative path
            if (workspace == null) {
                throw new IOException("no workspace");
            }

            // lets work relative to the workspace
            File dir = new File(workspace.getRemote());

            // lets make sure the dir exists
            if (dir.mkdirs()) {
                LOGGER.log(Level.FINE, "created workspace dir " + dir);
            }

            if (inputFile != null) {
                String path = inputFile.getPath();
                inputFile = new File(dir, path);

                // if the workspace is remote then lets make a local copy
                if (workspace.isRemote()) {
                    // lets switch to a temp dir for the local copy
                    dir = Files.createTempDir();
                    inputFile = new File(dir, path);

                    LOGGER.info("Workspace is remote so lets copy the file " + path);

                    VirtualChannel channel = workspace.getChannel();
                    File remotePath = new File(workspace.getRemote(), path);

                    LOGGER.info("Getting the remote file: " + remotePath + " copying to local file " + inputFile);

                    FilePath inputFilePath = new FilePath(inputFile);
                    FilePath parent = inputFilePath.getParent();
                    if (parent != null) {
                        parent.mkdirs();
                    }

                    try {
                        FilePath newFile = new FilePath(channel, remotePath.toString());
                        newFile.copyTo(inputFilePath);
                    } catch (Exception e) {
                        LOGGER.info("Failed to copy remote file locally: " + remotePath);
                        e.printStackTrace();
                        throw new IOException("failed to copy remote file locally: " + remotePath, e);
                    }

                    try {
                        long size = inputFilePath.length();
                        if (size == 0) {
                            LOGGER.warning("failed to find a size ");
                        } else {
                            LOGGER.info("size of new local file is " + size);
                        }
                    } catch (Exception e) {
                        LOGGER.info("failed to find size of local file " + inputFile);
                        e.printStackTrace();
                    }
                }
            }
            LOGGER.info("Processing the tekton catalog at dir " + dir);
            return processTektonCatalog(envVars, dir, inputFile, data);
        }

        if (data == null && inputFile != null) {
            data = Files.toByteArray(inputFile);
        }
        return data;
    }


    /**
     * Lets process any <code>image: uses:sourceURI</code> blocks in the tekton <code>Pipeline</code>,
     * <code>PipelineRun</code>, <code>Task</code> or <code>TaskRun</code> resources so that we can reuse Tasks or Steps
     * from Tekton Catalog or any other git repository.
     *
     * For background see: https://jenkins-x.io/blog/2021/02/25/gitops-pipelines/
     *
     * @param envVars
     * @param file optional file name to process
     * @param data data to process if no file name is given
     * @return the processed data
     * @throws Exception
     */
    private byte[] processTektonCatalog(EnvVars envVars, File dir, File file, byte[] data) throws Exception {
        boolean deleteInputFile = false;
        if (file == null) {
            // the following fails when not running in the controller so lets not use a temp file for now
            //file = File.createTempFile("tekton-input-", ".yaml", dir);
            file = new File(dir, "tekton-input-pipeline.yaml");
            Files.write(data, file);
            LOGGER.info("Saved file: " + file.getPath());
        }

        // the following fails when not running in the controller so lets not use a temp file for now
        //File outputFile = File.createTempFile("tekton-effective-", ".yaml", dir);
        File outputFile = new File(dir, "tekton-effective-pipeline.yaml");

        String filePath = file.getPath();
        String binary = ToolUtils.getJXPipelineBinary(getToolClassLoader());

        LOGGER.info("Using tekton pipeline binary " + binary);

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(binary, "-b", "--add-defaults", "-f", filePath, "-o", outputFile.getPath());
        if (envVars != null) {
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                LOGGER.info("Adding env var " + entry.getKey() + "=" + entry.getValue());
                builder.environment().put(entry.getKey(), entry.getValue());
            }
        }
        Process process = builder.start();
        int exitCode = process.waitFor();

        LogUtils.logStream(process.getInputStream(), LOGGER, false);
        LogUtils.logStream(process.getErrorStream(), LOGGER, true);

        if (exitCode != 0) {
            throw new Exception("failed to apply tekton catalog to file " + filePath);
        }

        LOGGER.info("Generated file: " + outputFile.getPath());

        data = Files.toByteArray(outputFile);

        LOGGER.info("Generated contents:\n" + new String(data, StandardCharsets.UTF_8));

        return data;
    }

    @Symbol("tektonCreateRaw")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public FormValidation doCheckInput(@QueryParameter(value = "input") final String input){
            if (input.length() == 0){
                return FormValidation.error("Input not provided");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillInputTypeItems(@QueryParameter(value = "inputType") final String inputType){
            ListBoxModel items =  new ListBoxModel();
            items.add(InputType.FILE.toString());
            items.add(InputType.URL.toString());
            items.add(InputType.YAML.toString());
            return items;
        }

        public ListBoxModel doFillClusterNameItems(@QueryParameter(value = "clusterName") final String clusterName){
            ListBoxModel items =  new ListBoxModel();
            for (String cn: TektonUtils.getTektonClientMap().keySet()){
                items.add(cn);
            }
            return items;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Tekton : Create Resource (Raw)";
        }
    }

    private String marshall(PipelineRun pipelineRun) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        String output = null;
        try {
            output = mapper.writeValueAsString(pipelineRun);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return output;
    }
}
