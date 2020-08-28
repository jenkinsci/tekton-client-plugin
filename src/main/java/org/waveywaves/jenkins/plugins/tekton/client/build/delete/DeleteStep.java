package org.waveywaves.jenkins.plugins.tekton.client.build.delete;

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
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.resource.v1alpha1.PipelineResource;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils.TektonResourceType;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class DeleteStep extends BaseStep {
    private static final Logger logger = Logger.getLogger(DeleteStep.class.getName());
    private String resourceType;
    private String resourceName;

    @DataBoundConstructor
    public DeleteStep(String resourceName, String resourceType) {
        super();
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        setTektonClient(TektonUtils.getTektonClient());
    }

    private String getResourceType(){
        return this.resourceType;
    }
    private String getResourceName(){
        return this.resourceName;
    }
    private TektonResourceType getTypedResourceType(){
        return TektonResourceType.valueOf(getResourceType());
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        runDelete();
    }

    protected void runDelete(){
        deleteWithResourceSpecificClient(this.getTypedResourceType(), this.getResourceName());
    }

    private void deleteWithResourceSpecificClient(TektonResourceType resourceType, String resourceName) {
        Boolean isDeleted = false;
        switch (resourceType) {
            case task:
                isDeleted = deleteTask(resourceName);
                break;
            case taskrun:
                isDeleted = deleteTaskRun(resourceName);
                break;
            case pipeline:
                isDeleted = deletePipeline(resourceName);
                break;
            case pipelinerun:
                isDeleted = deletePipelineRun(resourceName);
                break;
            case pipelineresource:
                isDeleted = deletePipelineResource(resourceName);
                break;
            default:
                logger.warning("Tekton Resource Type not supported");
        }
        if (isDeleted){
            logger.info("Deleted Tekton "+resourceType+" of name: "+resourceName);
        } else {
            logger.info("Unable to delete Tekton "+resourceType+" of name: "+resourceName);
        }
    }

    private Boolean deleteTask(String resourceName) {
        TektonClient tc = (TektonClient) tektonClient;
        List<Task> taskList = tc.v1beta1().tasks().list().getItems();
        Boolean isDeleted = false;
        for(int i = 0; i < taskList.size(); i++){
            Task task = taskList.get(i);
            String taskName = task.getMetadata().getName();
            if (taskName.equals(this.getResourceName())){
                isDeleted = tc.v1beta1().tasks().delete(task);
                break;
            }
        }
        return isDeleted;
    }

    private Boolean deleteTaskRun(String resourceName) {
        TektonClient tc = (TektonClient) tektonClient;
        List<TaskRun> taskRunList = tc.v1beta1().taskRuns().list().getItems();
        Boolean isDeleted = false;
        for(int i = 0; i < taskRunList.size(); i++){
            TaskRun taskRun = taskRunList.get(i);
            String taskRunName = taskRun.getMetadata().getName();
            if (taskRunName.equals(this.getResourceName())){
                isDeleted = tc.v1beta1().taskRuns().delete(taskRun);
                break;
            }
        }
        return isDeleted;
    }

    private Boolean deletePipeline(String resourceName) {
        TektonClient tc = (TektonClient) tektonClient;
        List<Pipeline> pipelineList = tc.v1beta1().pipelines().list().getItems();
        Boolean isDeleted = false;
        for(int i = 0; i < pipelineList.size(); i++){
            Pipeline pipeline = pipelineList.get(i);
            String pipelineName = pipeline.getMetadata().getName();
            if (pipelineName.equals(this.getResourceName())){
                isDeleted = tc.v1beta1().pipelines().delete(pipeline);
                break;
            }
        }
        return isDeleted;
    }

    private Boolean deletePipelineRun(String resourceName) {
        TektonClient tc = (TektonClient) tektonClient;
        List<PipelineRun> pipelineRunList = tc.v1beta1().pipelineRuns().list().getItems();
        Boolean isDeleted = false;
        for(int i = 0; i < pipelineRunList.size(); i++){
            PipelineRun pipelineRun = pipelineRunList.get(i);
            String pipelineRunName = pipelineRun.getMetadata().getName();
            if (pipelineRunName.equals(this.getResourceName())){
                isDeleted = tc.v1beta1().pipelineRuns().delete(pipelineRun);
                break;
            }
        }
        return isDeleted;
    }

    private Boolean deletePipelineResource(String resourceName) {
        TektonClient tc = (TektonClient) tektonClient;
        List<PipelineResource> pipelineResourceList = tc.v1alpha1().pipelineResources().list().getItems();
        Boolean isDeleted = false;
        for(int i = 0; i < pipelineResourceList.size(); i++){
            PipelineResource pipelineResource = pipelineResourceList.get(i);
            String pipelineResourceName = pipelineResource.getMetadata().getName();
            if (pipelineResourceName.equals(this.getResourceName())){
                isDeleted = tc.v1alpha1().pipelineResources().delete(pipelineResource);
                break;
            }
        }
        return isDeleted;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public FormValidation doCheckResourceName(@QueryParameter(value = "resourceName") final String resourceName){
            if (resourceName.length() == 0){
                return FormValidation.error("Resource Name not provided");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillResourceTypeItems(@QueryParameter(value = "input") final String input){
            ListBoxModel items =  new ListBoxModel();
            items.add(TektonResourceType.task.toString());
            items.add(TektonResourceType.taskrun.toString());
            items.add(TektonResourceType.pipeline.toString());
            items.add(TektonResourceType.pipelinerun.toString());
            items.add(TektonResourceType.pipelineresource.toString());
            return items;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Tekton : Delete Resource(s)";
        }
    }
}
