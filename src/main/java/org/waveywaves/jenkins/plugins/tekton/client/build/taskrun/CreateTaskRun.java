package org.waveywaves.jenkins.plugins.tekton.client.build.taskrun;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunSpec;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

public class CreateTaskRun extends BaseTaskRunStep {
    private static final Logger logger = Logger.getLogger(CreateTaskRun.class.getName());
    private static final String INPUT_TYPE_URL = "URL";
    private static final String INPUT_TYPE_YAML = "YAML";

    @DataBoundConstructor
    public CreateTaskRun(String input, String inputType) {
        super();
        this.inputType = inputType;
        this.input = input;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        if (getInputType().equals(INPUT_TYPE_URL)) {
            createFromUrl();
        } else if (getInputType().equals(INPUT_TYPE_YAML)) {
            createFromYaml();
        }
    }

    private void createFromUrl() throws MalformedURLException {
        URL taskRunUrl = new URL(this.getInput());
        TaskRun taskRun = resourceSpecificClient.load(taskRunUrl).get();
        taskRun = resourceSpecificClient.create(taskRun);
        logger.info("Created TaskRun from Url: " + taskRun.getMetadata().getName());
    }

    private void createFromYaml() {
        String yamlInput = this.getInput();
        InputStream yamlStream = new ByteArrayInputStream(yamlInput.getBytes());

        TaskRun taskRun = resourceSpecificClient.load(yamlStream).get();
        taskRun = resourceSpecificClient.create(taskRun);
        logger.info("Created TaskRun from Yaml: " + taskRun.getMetadata().getName());
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public FormValidation doCheckInput(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0){
                return FormValidation.error("Input not provided");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Tekton : Create TaskRun";
        }
    }
}
