package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import com.google.common.io.Files;
import com.google.common.io.LineReader;
import com.google.common.io.Resources;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.*;
import io.fabric8.tekton.resource.v1alpha1.PipelineResource;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.LogUtils;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils.TektonResourceType;
import org.waveywaves.jenkins.plugins.tekton.client.ToolUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.waveywaves.jenkins.plugins.tekton.client.logwatch.PipelineRunLogWatch;
import org.waveywaves.jenkins.plugins.tekton.client.logwatch.TaskRunLogWatch;

@Symbol("createStep")
public class CreateRaw extends BaseStep {
    private static final Logger logger = Logger.getLogger(CreateRaw.class.getName());

    private String input;
    private String inputType;
    private boolean enableCatalog;
    private PrintStream consoleLogger;

    @DataBoundConstructor
    public CreateRaw(String input, String inputType, boolean enableCatalog) {
        super();
        this.inputType = inputType;
        this.input = input;
        this.enableCatalog = enableCatalog;

        setKubernetesClient(TektonUtils.getKubernetesClient());
        setTektonClient(TektonUtils.getTektonClient());
    }

    protected String getInput(){
        return this.input;
    }
    protected String getInputType(){
        return this.inputType;
    }

    protected boolean isEnableCatalog() {
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
            case pipelineresource:
                return createPipelineResource(inputStream);
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

    public String createPipelineResource(InputStream inputStream) {
        if (pipelineResourceClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setPipelineResourceClient(tc.v1alpha1().pipelineResources());
        }
        String resourceName;
        PipelineResource pipelineRes = pipelineResourceClient.load(inputStream).get();
        pipelineRes = pipelineResourceClient.create(pipelineRes);
        resourceName = pipelineRes.getMetadata().getName();
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
        runCreate();
    }

    protected String runCreate() {
        URL url = null;
        byte[] data = null;
        String inputData = this.getInput();
        String inputType = this.getInputType();
        String createdResourceName = "";
        try {
            if (inputType.equals(InputType.URL.toString())) {
                url = new URL(inputData);
                data = Resources.toByteArray(url);

            } else if (inputType.equals(InputType.YAML.toString())) {
                data = inputData.getBytes(StandardCharsets.UTF_8);
            }
            if (data != null) {
                List<TektonResourceType> kind = TektonUtils.getKindFromInputStream(new ByteArrayInputStream(data), this.getInputType());
                if (kind.size() > 1){
                    logger.info("Multiple Objects in YAML not supported yet");
                } else {
                    if (enableCatalog) {
                        logger.info("processing the tekton catalog");
                        data = processTektonCatalog(data);
                    }

                    createdResourceName = createWithResourceSpecificClient(kind.get(0), new ByteArrayInputStream(data));
                }
            }
        } catch (Exception e) {
            logger.warning("possible URL related Exception has occurred " + e.toString());
        }
        return createdResourceName;
    }

    private byte[] processTektonCatalog(byte[] data) throws Exception {
        File dir = Files.createTempDir();
        File file = new File(dir, "input.yaml");
        File outputFile = new File(dir, "output.yaml");
        Files.write(data, file);

        logger.info("saved file: " + file.getPath());


        String binary = ToolUtils.getJXPipelineBinary();
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(binary, "-b", "-f", file.getPath(), "-o", outputFile.getPath());
        Process process = builder.start();
        int exitCode = process.waitFor();

        LogUtils.logStream(process.getInputStream(), logger, false);
        LogUtils.logStream(process.getErrorStream(), logger, true);
        if (exitCode != 0) {
            throw new Exception("failed to apply tekton catalog to file " + file.getPath());
        }

        logger.info("generated file: " + outputFile.getPath());

        data = Files.toByteArray(outputFile);

        // TODO lets remove the temporary files....
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
