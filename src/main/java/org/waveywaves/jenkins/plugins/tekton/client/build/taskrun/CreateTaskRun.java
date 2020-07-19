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
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunList;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

public class CreateTaskRun extends BaseTaskRunStep {
    private static final Logger logger = Logger.getLogger(CreateTaskRun.class.getName());

    @DataBoundConstructor
    public CreateTaskRun(String url) {
        super();
        this.url = url;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        URL taskRunUrl = new URL(this.getUrl());

        TaskRun taskRun = taskRunClient.load(taskRunUrl).get();
        String namespace = tektonClient.getNamespace();

        taskRun = taskRunClient.create(taskRun);
        System.out.println("Created: " + taskRun.getMetadata().getName());

        // List TaskRun
        TaskRunList taskRunList = taskRunClient.inNamespace(namespace).list();
        System.out.println("There are " + taskRunList.getItems().size() + " TaskRun objects in " + namespace);
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public FormValidation doCheckUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0){
                return FormValidation.error("url not provided");
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
