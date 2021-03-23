package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.resource.v1alpha1.PipelineResource;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.LogUtils;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils.TektonResourceType;
import org.waveywaves.jenkins.plugins.tekton.client.ToolUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;
import org.waveywaves.jenkins.plugins.tekton.client.logwatch.PipelineRunLogWatch;
import org.waveywaves.jenkins.plugins.tekton.client.logwatch.TaskRunLogWatch;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Symbol("createStep")
public class CreateRaw extends BaseStep {
    private static final Logger logger = Logger.getLogger(CreateRaw.class.getName());

    private String input;
    private String inputType;
    private boolean enableCatalog;
    private transient PrintStream consoleLogger;
    private transient ClassLoader toolClassLoader;

    @DataBoundConstructor
    public CreateRaw(String input, String inputType, boolean enableCatalog) {
        super();
        this.inputType = inputType;
        this.input = input;
        this.enableCatalog = enableCatalog;

        setKubernetesClient(TektonUtils.getKubernetesClient());
        setTektonClient(TektonUtils.getTektonClient());
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

    public String getInput() {
        return this.input;
    }

    public String getInputType() {
        return this.inputType;
    }

    public boolean isEnableCatalog() {
        return enableCatalog;
    }

    protected String createWithResourceSpecificClient(TektonResourceType resourceType, InputStream inputStream) {
        switch (resourceType) {
            case task:
                return createTask(inputStream);
            case taskrun:
                return createTaskRun(inputStream);
            case pipeline:
                return createPipeline(inputStream);
            case pipelinerun:
                return createPipelineRun(inputStream);
            default:
                return "";
        }
    }

    public String createTaskRun(InputStream inputStream) {
        if (taskRunClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setTaskRunClient(tc.v1beta1().taskRuns());
        }
        String resourceName;
        TaskRun taskrun = taskRunClient.load(inputStream).get();
        taskrun = taskRunClient.create(taskrun);
        resourceName = taskrun.getMetadata().getName();

        streamTaskRunLogsToConsole(taskrun);
        return resourceName;
    }

    public String createTask(InputStream inputStream) {
        if (taskClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setTaskClient(tc.v1beta1().tasks());
        }
        String resourceName;
        Task task = taskClient.load(inputStream).get();
        task = taskClient.create(task);
        resourceName = task.getMetadata().getName();
        return resourceName;
    }

    public String createPipeline(InputStream inputStream) {
        if (pipelineClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setPipelineClient(tc.v1beta1().pipelines());
        }
        String resourceName;
        Pipeline pipeline = pipelineClient.load(inputStream).get();
        pipeline = pipelineClient.create(pipeline);
        resourceName = pipeline.getMetadata().getName();
        return resourceName;
    }

    public String createPipelineRun(InputStream inputStream) {
        if (pipelineRunClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setPipelineRunClient(tc.v1beta1().pipelineRuns());
        }
        String resourceName;
        PipelineRun pipelineRun = pipelineRunClient.load(inputStream).get();
        pipelineRun = pipelineRunClient.create(pipelineRun);
        resourceName = pipelineRun.getMetadata().getName();

        streamPipelineRunLogsToConsole(pipelineRun);
        return resourceName;
    }

    public void streamTaskRunLogsToConsole(TaskRun taskRun) {
        synchronized (consoleLogger) {
            KubernetesClient kc = (KubernetesClient) kubernetesClient;
            Thread logWatchTask = null;
            try {
                TaskRunLogWatch logWatch = new TaskRunLogWatch(kc, taskRun, consoleLogger);
                logWatchTask = new Thread(logWatch);
                logWatchTask.start();
                logWatchTask.join();
            } catch (Exception e) {
                logger.warning("Exception occurred "+e.toString());
            }
        }
    }

    public void streamPipelineRunLogsToConsole(PipelineRun pipelineRun) {
        KubernetesClient kc = (KubernetesClient) kubernetesClient;
        TektonClient tc = (TektonClient) tektonClient;
        Thread logWatchTask;
        try {
            PipelineRunLogWatch logWatch = new PipelineRunLogWatch(kc, tc, pipelineRun, consoleLogger);
            logWatchTask = new Thread(logWatch);
            logWatchTask.start();
            logWatchTask.join();
        } catch (Exception e) {
            logger.warning("Exception occurred "+e.toString());
        }
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        consoleLogger = listener.getLogger();

        // lets make sure the tekton client is not empty
        if (tektonClient == null) {
            setTektonClient(TektonUtils.getTektonClient());
            if (tektonClient == null) {
                throw new IOException("no tektonClient");
            }
        }
        if (kubernetesClient == null) {
            setKubernetesClient(TektonUtils.getKubernetesClient());
            if (kubernetesClient == null) {
                throw new IOException("no kubernetesClient");
            }
        }
        EnvVars envVars = run.getEnvironment(listener);
        runCreate(workspace, envVars);
    }

    protected String runCreate(FilePath workspace, EnvVars envVars) {
        URL url = null;
        byte[] data = null;
        File inputFile = null;
        String inputData = this.getInput();
        String inputType = this.getInputType();
        String createdResourceName = "";
        try {
            if (inputType.equals(InputType.URL.toString())) {
                url = new URL(inputData);
                data = Resources.toByteArray(url);
            } else if (inputType.equals(InputType.YAML.toString())) {
                data = inputData.getBytes(StandardCharsets.UTF_8);
            } else if (inputType.equals(InputType.FILE.toString())) {
                inputFile = new File(inputData);
            }
            data = convertTektonData(workspace ,envVars, inputFile, data);
            if (data != null) {
                List<TektonResourceType> kind = TektonUtils.getKindFromInputStream(new ByteArrayInputStream(data), this.getInputType());
                if (kind.size() > 1){
                    logger.info("Multiple Objects in YAML not supported yet");
                } else {
                    TektonResourceType resourceType = kind.get(0);
                    logger.info("creating kind " + resourceType.name());
                    createdResourceName = createWithResourceSpecificClient(resourceType, new ByteArrayInputStream(data));
                }
            }
        } catch (Exception e) {
            logger.warning("possible URL related Exception has occurred " + e.toString());
            e.printStackTrace();
        }
        return createdResourceName;
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
                logger.log(Level.FINE, "created workspace dir " + dir);
            }

            if (inputFile != null) {
                inputFile = new File(dir, inputFile.getPath());
            }
            logger.info("processing the tekton catalog");
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
            file = File.createTempFile("tekton-input-", ".yaml", dir);
            Files.write(data, file);
            logger.info("saved file: " + file.getPath());
        }

        File outputFile = File.createTempFile("tekton-effective-", ".yaml", dir);

        String filePath = file.getPath();
        String binary = ToolUtils.getJXPipelineBinary(getToolClassLoader());

        logger.info("using tekton pipeline binary " + binary);

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(binary, "-b", "--add-defaults", "-f", filePath, "-o", outputFile.getPath());
        if (envVars != null) {
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                builder.environment().put(entry.getKey(), entry.getValue());
            }
        }
        Process process = builder.start();
        int exitCode = process.waitFor();

        LogUtils.logStream(process.getInputStream(), logger, false);
        LogUtils.logStream(process.getErrorStream(), logger, true);
        if (exitCode != 0) {
            throw new Exception("failed to apply tekton catalog to file " + filePath);
        }

        logger.info("generated file: " + outputFile.getPath());

        data = Files.toByteArray(outputFile);
        return data;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public FormValidation doCheckInput(@QueryParameter(value = "input") final String input){
            if (input.length() == 0){
                return FormValidation.error("Input not provided");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillInputTypeItems(@QueryParameter(value = "input") final String input){
            ListBoxModel items =  new ListBoxModel();
            items.add(InputType.FILE.toString());
            items.add(InputType.URL.toString());
            items.add(InputType.YAML.toString());
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
}
