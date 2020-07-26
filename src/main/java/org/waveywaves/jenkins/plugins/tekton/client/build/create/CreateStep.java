package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.pipeline.v1beta1.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils.TektonResourceType;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URL;
import java.nio.channels.Pipe;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CreateStep extends BaseStep {
    private static final Logger logger = Logger.getLogger(CreateStep.class.getName());
    private List<TektonResourceType> kind = new ArrayList<TektonResourceType>();

    protected enum InputType {
        URL,
        YAML,
        Interactive
    }

    @DataBoundConstructor
    public CreateStep(String input, String inputType) {
        super();
        this.inputType = inputType;
        this.input = input;
    }

    protected void createWithResourceSpecificClient(TektonResourceType resourceType, InputStream inputStream) {
        String resourceName = "";
        switch (resourceType) {
//            case task:
//                taskRunClient = tektonClient.v1beta1().taskRuns();
//                TaskRun taskrun = taskRunClient.load(inputStream).get();
//                resourceName = taskrun.getMetadata().getName();
//                break;
            case taskrun:
                TaskRun taskrun = tektonClient.v1beta1().taskRuns().load(inputStream).get();
                taskrun = tektonClient.v1beta1().taskRuns().create(taskrun);
                resourceName = taskrun.getMetadata().getName();
                break;
//            case pipeline:
//                this.resourceSpecificClient = tektonClient.v1beta1().pipelines();
//                break;
//            case pipelinerun:
//                this.resourceSpecificClient = tektonClient.v1beta1().pipelineRuns();
//                break;
            default:
                logger.warning("Tekton ResourceSpecificClient not created");
        }
        logger.info("Created Tekton "+resourceType+" of name: "+resourceName);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        runCreate();
    }

    private void runCreate() throws java.io.IOException {
        InputStream inputStreamForKind = null;
        InputStream inputStreamForData = null;
        String inputData = this.getInput();
        String inputType = this.getInputType();

        if (inputType.equals(InputType.URL.toString())) {
            URL url = new URL(inputData);
            inputStreamForKind = TektonUtils.urlToByteArrayStream(url);
            inputStreamForData = url.openStream();
        } else if (inputType.equals(InputType.YAML.toString())) {
            inputStreamForKind = new ByteArrayInputStream(inputData.getBytes());
            inputStreamForData = new ByteArrayInputStream(inputData.getBytes());
        }

        kind = TektonUtils.getKindFromInputStream(inputStreamForKind, this.getInputType());
        if (kind.size() > 1){
            logger.info("Multiple Objects in YAML not supported yet");
            return;
        } else {
            createWithResourceSpecificClient(kind.get(0), inputStreamForData);
        }
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
            return "Tekton : Create Resource(s)";
        }
    }
}
