package org.waveywaves.jenkins.plugins.tekton.client.build.delete;

import com.google.common.base.Strings;
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
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils.TektonResourceType;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

@Symbol("tektonDeleteStep")
public class DeleteRaw extends BaseStep {
    private static final Logger logger = Logger.getLogger(DeleteRaw.class.getName());
    private String resourceType;
    private String resourceName;
    private String clusterName;
    private String nameSpace;

    @DataBoundConstructor
    public DeleteRaw(String resourceType, String clusterName, DeleteAllBlock deleteAllStatus) {
        super();
        this.resourceType = resourceType;
        this.resourceName = deleteAllStatus != null ? deleteAllStatus.resourceName : null;
        this.clusterName = clusterName;
        setTektonClient(TektonUtils.getTektonClient(getClusterName()));
    }

    public static class DeleteAllBlock {
        private String resourceName;

        @DataBoundConstructor
        public DeleteAllBlock(String resourceName) {
            this.resourceName = resourceName;
        }
    }

    public String getClusterName() {
        if (Strings.isNullOrEmpty(clusterName)) {
            clusterName = TektonUtils.DEFAULT_CLIENT_KEY;
        }
        return clusterName;
    }
    public String getResourceType(){
        return this.resourceType;
    }
    public String getResourceName(){
        return this.resourceName;
    }
    public String getNameSpace(){
        return this.nameSpace;
    }

    @DataBoundSetter
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
        setTektonClient(TektonUtils.getTektonClient(getClusterName()));
    }

    @DataBoundSetter
    protected void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @DataBoundSetter
    protected void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @DataBoundSetter
    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    private TektonResourceType getTypedResourceType(){
        return TektonResourceType.valueOf(getResourceType());
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        runDelete();
    }

    protected boolean runDelete(){
        return deleteWithResourceSpecificClient(this.getTypedResourceType());
    }

    private boolean deleteWithResourceSpecificClient(TektonResourceType resourceType) {
        switch (resourceType) {
            case task:
                return deleteTask();
            case taskrun:
                return deleteTaskRun();
            case pipeline:
                return deletePipeline();
            case pipelinerun:
                return deletePipelineRun();
            default:
                return false;
        }
    }

    public Boolean deleteTask() {
        if (taskClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setTaskClient(tc.v1beta1().tasks());
        }
        List<Task> taskList = Strings.isNullOrEmpty(nameSpace)
            ? taskClient.list().getItems()
                : taskClient.inNamespace(nameSpace).list().getItems();
        Boolean isDeleted = false;
        if (this.getResourceName() == null) {
            return Strings.isNullOrEmpty(nameSpace)
                    ? taskClient.delete(taskList)
                    : taskClient.inNamespace(nameSpace).delete(taskList);
        }
        for (Task task : taskList) {
            String taskName = task.getMetadata().getName();
            if (taskName.equals(this.getResourceName())) {
                isDeleted = Strings.isNullOrEmpty(nameSpace)
                        ? taskClient.delete(task)
                        : taskClient.inNamespace(nameSpace).delete(task);
                break;
            }
        }
        return isDeleted;
    }

    public Boolean deleteTaskRun() {
        if (taskRunClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setTaskRunClient(tc.v1beta1().taskRuns());
        }
        List<TaskRun> taskRunList = Strings.isNullOrEmpty(nameSpace)
                ? taskRunClient.list().getItems()
                : taskRunClient.inNamespace(nameSpace).list().getItems();
        Boolean isDeleted = false;
        if (this.getResourceName() == null) {
            return Strings.isNullOrEmpty(nameSpace)
                    ? taskRunClient.delete(taskRunList)
                    : taskRunClient.inNamespace(nameSpace).delete(taskRunList);
        }
        for (TaskRun taskRun : taskRunList) {
            String taskRunName = taskRun.getMetadata().getName();
            if (taskRunName.equals(this.getResourceName())) {
                isDeleted = Strings.isNullOrEmpty(nameSpace)
                        ? taskRunClient.delete(taskRun)
                        : taskRunClient.inNamespace(nameSpace).delete(taskRun);
                break;
            }
        }
        return isDeleted;
    }

    public Boolean deletePipeline() {
        if (pipelineClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setPipelineClient(tc.v1beta1().pipelines());
        }
        List<Pipeline> pipelineList = Strings.isNullOrEmpty(nameSpace)
                ? pipelineClient.list().getItems()
                : pipelineClient.inNamespace(nameSpace).list().getItems();
        Boolean isDeleted = false;
        if (this.getResourceName() == null) {
            return Strings.isNullOrEmpty(nameSpace)
                    ? pipelineClient.delete(pipelineList)
                    : pipelineClient.inNamespace(nameSpace).delete(pipelineList);
        }
        for (Pipeline pipeline : pipelineList) {
            String pipelineName = pipeline.getMetadata().getName();
            if (pipelineName.equals(this.getResourceName())) {
                isDeleted = Strings.isNullOrEmpty(nameSpace)
                        ? pipelineClient.delete(pipeline)
                        : pipelineClient.inNamespace(nameSpace).delete(pipeline);
                break;
            }
        }
        return isDeleted;
    }

    public Boolean deletePipelineRun() {
        if (pipelineRunClient == null) {
            TektonClient tc = (TektonClient) tektonClient;
            setPipelineRunClient(tc.v1beta1().pipelineRuns());
        }
        List<PipelineRun> pipelineRunList = Strings.isNullOrEmpty(nameSpace)
                ? pipelineRunClient.list().getItems()
                : pipelineRunClient.inNamespace(nameSpace).list().getItems();
        Boolean isDeleted = false;
        if (this.getResourceName() == null) {
            return Strings.isNullOrEmpty(nameSpace)
                    ? pipelineRunClient.delete(pipelineRunList)
                    : pipelineRunClient.inNamespace(nameSpace).delete(pipelineRunList);
        }
        for (PipelineRun pipelineRun : pipelineRunList) {
            String pipelineRunName = pipelineRun.getMetadata().getName();
            if (pipelineRunName.equals(this.getResourceName())) {
                isDeleted = Strings.isNullOrEmpty(nameSpace)
                        ? pipelineRunClient.delete(pipelineRun)
                        : pipelineRunClient.inNamespace(nameSpace).delete(pipelineRun);
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

        public FormValidation doCheckNameSpace(@QueryParameter(value = "nameSpace") final String nameSpace) {
            if (nameSpace != null && nameSpace.trim().length() > 0 && !nameSpace.matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?")) {
                return FormValidation.error("NameSpace must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillResourceTypeItems(@QueryParameter(value = "input") final String input){
            ListBoxModel items =  new ListBoxModel();
            items.add(TektonResourceType.task.toString());
            items.add(TektonResourceType.taskrun.toString());
            items.add(TektonResourceType.pipeline.toString());
            items.add(TektonResourceType.pipelinerun.toString());
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
            return "Tekton : Delete Resource (Raw)";
        }
    }
}
