package org.waveywaves.jenkins.plugins.tekton.client.pipeline;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class StartPipeline extends Builder implements SimpleBuildStep {
    private static final Logger logger = Logger.getLogger(StartPipeline.class.getName());
    private final String pipelineName;

    @DataBoundConstructor
    public StartPipeline(String pipelineName) {
        this.pipelineName = pipelineName.trim();
    }

    public String getPipelineName() {
        return this.pipelineName.trim();
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        TektonClient tkn = TektonUtils.getTektonClient();
        List<Pipeline> pipelinesList = tkn.v1beta1().pipelines().list().getItems();
        boolean pipelineExists = false;

        for (Pipeline p: pipelinesList){
            String pName = p.getMetadata().getName();
            logger.info("Checking '"+pipelineName+"' against '"+pName+"'");
            if (pName.equals(pipelineName)) {
                pipelineExists = true;
            }
        }
        if (pipelineExists) {
            logger.info("Pipeline '"+pipelineName+"' exists");
        } else {
            logger.info("Pipeline '"+pipelineName+"' not found");
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public FormValidation doCheckPipelineName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0){
                return FormValidation.error("Please enter the Pipeline Name");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Tekton : Start Pipeline";
        }
    }
}
