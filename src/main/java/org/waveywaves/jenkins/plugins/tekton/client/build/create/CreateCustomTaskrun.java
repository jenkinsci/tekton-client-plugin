package org.waveywaves.jenkins.plugins.tekton.client.build.create;

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
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.*;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Symbol("customCreateTaskrun")
public class CreateCustomTaskrun extends BaseStep {
    private static final Logger logger = Logger.getLogger(CreateCustomTaskrun.class.getName());
    private String clusterName;
    private PrintStream consoleLogger;
    private String kind;
    // ObjectMeta
    private String name;
    private String generateName;
    private String namespace;
    // Spec
    private List<TektonParam> params;
    private List<TektonWorkspaceBind> workspaces;
    private String taskRef;

    @DataBoundConstructor
    public CreateCustomTaskrun(final String name,
                               final String generateName,
                               final String namespace,
                               final String clusterName,
                               final List<TektonWorkspaceBind> workspaces,
                               final List<TektonParam> params,
                               final String taskRef){
        super();
        this.kind = TektonUtils.TektonResourceType.taskrun.toString();
        this.name = name;
        this.generateName = generateName;
        this.namespace = namespace;
        this.taskRef = taskRef;
        this.workspaces = workspaces;
        this.params = params;
        this.clusterName = clusterName;
    }

    public String getKind() { return this.kind; }
    public String getName() { return this.name; }
    public String getNamespace() { return this.namespace; }
    public String getTaskRef() { return this.taskRef; }
    public String getGenerateName() { return this.generateName; }
    public List<TektonWorkspaceBind> getWorkspaces() { return this.workspaces; }
    public List<TektonParam> getParams() { return this.params; }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        consoleLogger = taskListener.getLogger();
        logTektonTaskrun();
        runCreate();
    }

    private void runCreate(){
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(getName());
        metadata.setNamespace(getNamespace());
        metadata.setGenerateName(getGenerateName());

        TaskRef taskRef = new TaskRef();
        taskRef.setKind("Task");
        taskRef.setApiVersion("tekton.dev/v1beta1");
        taskRef.setName(getTaskRef());

        TaskRunSpec spec =  new TaskRunSpec();
        spec.setTaskRef(taskRef);
        spec.setWorkspaces(workspacesToWorkspaceBindingList());
        spec.setParams(paramsToParamList());

        TaskRunBuilder taskRunBuilder = new TaskRunBuilder();
        taskRunBuilder.withApiVersion("tekton.dev/v1beta1");
        taskRunBuilder.withKind("TaskRun");
        taskRunBuilder.withMetadata(metadata);
        taskRunBuilder.withSpec(spec);

        TaskRun taskRun = taskRunBuilder.build();
        if (taskClient == null) {
            TektonClient tc = TektonUtils.getTektonClient(clusterName);
            setTaskRunClient(tc.v1beta1().taskRuns());
        }
        taskRun = taskRunClient.create(taskRun);
        String resourceName = taskRun.getMetadata().getName();

        consoleLogger.print(String.format("Created Task with Name %s", resourceName));
    }

    private List<Param> paramsToParamList() {
        List<Param> paramList = new ArrayList<>();
        for (TektonParam p: this.params) {
            Param param = new Param();
            param.setName(p.getName());

            ArrayOrString s = new ArrayOrString();
            s.setStringVal(p.getValue());
            param.setValue(s);

            paramList.add(param);
        }
        return paramList;
    }

    public List<WorkspaceBinding> workspacesToWorkspaceBindingList() {
        List<WorkspaceBinding> wsbList = new ArrayList<>();
        for (TektonWorkspaceBind w: this.workspaces){
            WorkspaceBinding wsb = new WorkspaceBinding();
            wsb.setName(w.getName());
            wsb.setPersistentVolumeClaim(new PersistentVolumeClaimVolumeSource(w.getClaimName(), false));

            wsbList.add(wsb);
        }
        return wsbList;
    }

    private void logTektonTaskrun() {
        consoleLogger.println("Creating Resource from Custom Config");
        String l = String.format("Kind: %s%n" +
                        "Name: %s%n" +
                        "Namespace: %s%n" +
                        "\tTaskRef: %s %n",
                getKind(), getName(), getNamespace(), getTaskRef());
        consoleLogger.print(l);
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }
        public FormValidation doCheckName(@QueryParameter(value = "name") final String name){
            if (name.length() == 0){
                return FormValidation.error("Name not provided");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckNamespace(@QueryParameter(value = "namespace") final String namespace){
            if (namespace.length() == 0){
                return FormValidation.error("Namespace not provided");
            }
            return FormValidation.ok();
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
            return "Tekton : Create TaskRun";
        }
    }
}
