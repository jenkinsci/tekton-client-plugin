package org.waveywaves.jenkins.plugins.tekton.client.build.create;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskList;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunList;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineList;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunList;


@Symbol("tektonCreateRaw")
public class CreateRaw extends BaseStep {
    private static final Logger LOGGER = Logger.getLogger(CreateRaw.class.getName());

    private final String input;
    private final String inputType;
    private String namespace;
    private String clusterName;
    private boolean enableCatalog;

    private transient PrintStream consoleLogger;
    private transient ClassLoader toolClassLoader;
    private transient ChecksPublisher checksPublisher;

    private MixedOperation<Task, TaskList, Resource<Task>> taskClient;
    private MixedOperation<TaskRun, TaskRunList, Resource<TaskRun>> taskRunClient;
    private MixedOperation<Pipeline, PipelineList, Resource<Pipeline>> pipelineClient;
    private MixedOperation<PipelineRun, PipelineRunList, Resource<PipelineRun>> pipelineRunClient;


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
        if (taskRunClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setTaskRunClient(tc.v1beta1().taskRuns());
        }
        if (taskRunClient == null) {
            throw new IllegalStateException("taskRunClient is still null after setup.");
        }


// Explicit second check to satisfy static analyzers
        if (taskRunClient == null) {
            throw new IllegalStateException("taskRunClient is still null after setup.");
        }

        String resourceName;
        TaskRun taskrun = taskRunClient.load(inputStream).get();
        if (!Strings.isNullOrEmpty(namespace) && Strings.isNullOrEmpty(taskrun.getMetadata().getNamespace())) {
            taskrun.getMetadata().setNamespace(namespace);
        }
        String ns = taskrun.getMetadata().getNamespace();
        if (Strings.isNullOrEmpty(ns)) {
            taskrun = taskRunClient.create(taskrun);
        } else {
            taskrun = taskRunClient.inNamespace(ns).create(taskrun);
        }
        resourceName = taskrun.getMetadata().getName();

        streamTaskRunLogsToConsole(taskrun);
        return resourceName;
    }

    public String createTask(InputStream inputStream) {
//        if (taskClient == null) {
//            TektonClient tc = (TektonClient) tektonClient;
//            setTaskClient(tc.v1beta1().tasks());
//        }
        if(taskClient == null)throw new IllegalStateException("taskClient is still null after setup");

        String resourceName;
        Task task = taskClient.load(inputStream).get();
        if (!Strings.isNullOrEmpty(namespace) && Strings.isNullOrEmpty(task.getMetadata().getNamespace())) {
            task.getMetadata().setNamespace(namespace);
        }
        String ns = task.getMetadata().getNamespace();
        if (Strings.isNullOrEmpty(ns)) {
            task = taskClient.create(task);
        } else {
            task = taskClient.inNamespace(ns).create(task);
        }
        resourceName = task.getMetadata().getName();
        return resourceName;
    }

    public String createPipeline(InputStream inputStream) {
        
        if (pipelineClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setPipelineClient(tc.v1beta1().pipelines());
        }
        if (pipelineClient == null) {
            throw new IllegalStateException("pipelineClient is still null after setup.");
        }
        String resourceName;
        Pipeline pipeline = pipelineClient.load(inputStream).get();
        Objects.requireNonNull(pipelineRunClient, "pipelineRunClient is null");

        if (!Strings.isNullOrEmpty(namespace) && Strings.isNullOrEmpty(pipeline.getMetadata().getNamespace())) {
            pipeline.getMetadata().setNamespace(namespace);
        }
        String ns = pipeline.getMetadata().getNamespace();
        if (Strings.isNullOrEmpty(ns)) {
            pipeline = pipelineClient.create(pipeline);
        } else {
            pipeline = pipelineClient.inNamespace(ns).create(pipeline);
        }
        resourceName = pipeline.getMetadata().getName();
        return resourceName;
    }

    public String createPipelineRun(InputStream inputStream, EnvVars envVars) throws Exception {
        if (pipelineRunClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setPipelineRunClient(tc.v1beta1().pipelineRuns());
        }
        PipelineRun pipelineRun = pipelineRunClient.load(inputStream).get();

        String resourceName;
        final PipelineRun PipelineRun = pipelineRunClient.load(inputStream).get();
        if (!Strings.isNullOrEmpty(namespace) && Strings.isNullOrEmpty(pipelineRun.getMetadata().getNamespace())) {
            pipelineRun.getMetadata().setNamespace(namespace);
        }

        LOGGER.info("Using environment variables " + envVars);

        enhancePipelineRunWithEnvVars(pipelineRun, envVars);

        String ns = pipelineRun.getMetadata().getNamespace();

        LOGGER.info("Creating PipelineRun\n" + marshall(pipelineRun));

        PipelineRun updatedPipelineRun = Strings.isNullOrEmpty(ns) ?
                pipelineRunClient.create(pipelineRun) :
                pipelineRunClient.inNamespace(ns).create(pipelineRun);

        try {
            String namespace = Strings.isNullOrEmpty(ns) ? "default" : ns;
            CoreV1Api coreV1Api = new CoreV1Api();
            V1PodList podList = coreV1Api.listNamespacedPod(namespace, null, null, null, null,
                    "tekton.dev/pipelineRun=" + updatedPipelineRun.getMetadata().getName(),
                    null, null, null, null, null);

            for (V1Pod pod : podList.getItems()) {
                System.out.println("---- Logs for Pod: " + pod.getMetadata().getName() + " ----");
                ProcessBuilder processBuilder = new ProcessBuilder("kubectl", "logs", pod.getMetadata().getName(), "-n", namespace);
                Process process = processBuilder.start();
                InputStream iNputStream = process.getInputStream();
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().forEach(System.out::println);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        PipelineRun reloaded = pipelineRunClient.inNamespace(ns).withName(resourceName).get();
        List<Condition> conditions = reloaded
                .getStatus()
                .getConditions();
        Optional<Condition> succeeded = conditions
                .stream()
                .filter(c -> c.getType().equalsIgnoreCase("Succeeded"))
                .findFirst();

        if (succeeded.isPresent() && succeeded.get().getStatus().equalsIgnoreCase("false")) {
            // pass the error message
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
