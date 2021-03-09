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
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.*;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Symbol("customCreateTask")
public class CreateCustomTask extends BaseStep {
    private static final Logger logger = Logger.getLogger(CreateCustomTask.class.getName());

    private PrintStream consoleLogger;
    private String kind;
    // ObjectMeta
    private String name;
    private String namespace;
    // Spec
    private String description;
    private List<TektonStep> steps;
    private List<TektonStringParamSpec> params;
    private List<TektonTaskResult> results;
    private List<TektonWorkspaceDecl> workspaces;
    private List<TektonEnv> envs;

    @DataBoundConstructor
    public CreateCustomTask(final String name,
                            final String namespace,
                            final String description,
                            final List<TektonStringParamSpec> params,
                            final List<TektonTaskResult> results,
                            final List<TektonWorkspaceDecl> workspaces,
                            final List<TektonStep> steps,
                            final List<TektonEnv> envs){
        super();
        this.kind = TektonUtils.TektonResourceType.task.toString();
        this.name = name;
        this.namespace = namespace;
        this.description = description;
        this.params = params;
        this.results = results;
        this.steps = steps;
        this.workspaces = workspaces;
        this.envs = envs;
    }

    public String getKind() { return this.kind; }
    public String getName() { return this.name; }
    public String getNamespace() { return this.namespace; }
    public String getDescription() { return this.description; }
    public List<TektonStringParamSpec> getParams() { return this.params; }
    public List<TektonTaskResult> getResults() { return this.results; }
    public List<TektonStep> getSteps() { return this.steps; }
    public List<TektonWorkspaceDecl> getWorkspaces() { return this.workspaces; }
    public List<TektonEnv> getEnvs() { return this.envs; }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        consoleLogger = taskListener.getLogger();
        logTektonTask();
        runCreate();
    }

    private void runCreate(){
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(getName());
        metadata.setNamespace(getNamespace());

        TaskSpec spec =  new TaskSpec();
        spec.setDescription(getDescription());
        if(this.params != null) spec.setParams(paramsToParamSpecList());
        if(this.results != null) spec.setResults(resultsToResultList());
        if(this.workspaces != null) spec.setWorkspaces(workspacesToWorkspaceDeclarationList());
        if(this.steps != null) spec.setSteps(stepsToStepList());

        TaskBuilder taskBuilder = new TaskBuilder();
        taskBuilder.withApiVersion("tekton.dev/v1beta1");
        taskBuilder.withKind("Task");
        taskBuilder.withMetadata(metadata);
        taskBuilder.withSpec(spec);

        Task task = taskBuilder.build();
        if (taskClient == null) {
            TektonClient tc = TektonUtils.getTektonClient();
            setTaskClient(tc.v1beta1().tasks());
        }
        task = taskClient.create(task);
        String resourceName = task.getMetadata().getName();

        consoleLogger.print(String.format("Created Task with Name %s", resourceName));
    }

    private List<ParamSpec> paramsToParamSpecList() {
        List<ParamSpec> paramList = new ArrayList<>();
        if(params != null) {
            for (TektonStringParamSpec p: params) {
                ParamSpec paramSpec = new ParamSpec();
                paramSpec.setType("string");
                paramSpec.setName(p.getName());
                String desc = p.getDescription();
                if (!desc.isEmpty()) {
                    paramSpec.setDescription(desc);
                }
                String defaultValue = p.getDefaultValue();
                if (!defaultValue.isEmpty()) {
                    ArrayOrString s = new ArrayOrString();
                    s.setStringVal(defaultValue);
                    paramSpec.setDefault(s);
                }
                paramList.add(paramSpec);
            }
        }
        return paramList;
    }

    private List<TaskResult> resultsToResultList() {
        List<TaskResult> taskResults = new ArrayList<>();
        for (TektonTaskResult r: this.results){
            TaskResult tr = new TaskResult();
            tr.setName(r.getName());
            tr.setDescription(r.getDescription());
        }
        return taskResults;
    }

    public List<WorkspaceDeclaration> workspacesToWorkspaceDeclarationList() {
        List<WorkspaceDeclaration> wsdList = new ArrayList<>();
        for (TektonWorkspaceDecl w: this.workspaces){
            WorkspaceDeclaration wsd = new WorkspaceDeclaration();
            wsd.setName(w.getName());
            wsd.setMountPath(w.getMountPath());
            wsd.setDescription(w.getDescription());
            wsd.setReadOnly(w.getReadOnly());
            wsdList.add(wsd);
        }
        return wsdList;
    }

    public List<Step> stepsToStepList(){
        List<Step> stepList = new ArrayList<>();
        for (TektonStep s: this.steps) {
            Step step = new Step();
            step.setName(s.getName());
            step.setImage(s.getImage());
            if(s.getWorkingDir() != null) step.setWorkingDir(s.getWorkingDir());
            if(s.getScript() != null) step.setScript(s.getScript());
            List<EnvVar> envVarList = new ArrayList<>();
            if(s.getEnvs() != null ) {
                for (TektonEnv e : s.getEnvs()){
                    EnvVar envVar = new EnvVar();
                    if(e.getName() != null) envVar.setName(e.getName());
                    if(e.getValue() != null) envVar.setValue(e.getValue());
                    envVarList.add(envVar);
                }
            }
            step.setEnv(envVarList);
            stepList.add(step);
        }
        return stepList;
    }

    private void logTektonTask() {
        consoleLogger.println("Creating Resource from Custom Config");
        String l = String.format("Kind: %s%n" +
                "Name: %s%n" +
                "Namespace: %s%n" +
                "Description: %s%n" +
                "\tWorkspaces: %s %n"+
                "\tParam: %s%n" +
                "\tSteps: %s%n",
                getKind(), getName(), getNamespace(), getDescription(), workspacesToString(), paramsToString(), stepsToString());
        consoleLogger.print(l);
    }

    private String paramsToString(){
        StringBuilder sb = new StringBuilder();
        if(params != null) {
            for (TektonStringParamSpec p : params) {
                String s = String.format("%n\t\tParam Name: %s%n" +
                        "\t\tParam Description: %s%n" +
                        "\t\tParam Type: %s%n", p.getName(), p.getDescription(), p.getDefaultValue());
                sb.append(s);
            }
        }
        return sb.toString();
    }

    private String workspacesToString(){
        StringBuilder sb = new StringBuilder();
        consoleLogger.println((this.workspaces == null));
        if(this.workspaces != null) {
            for (TektonWorkspaceDecl w : this.workspaces) {
                String s = String.format("%n\t\tWorkspace Name: %s%n" +
                        "\t\tWorkspace Description: %s%n" +
                        "\t\tWorkspace MountPath: %s%n", w.getName(), w.getDescription(), w.getMountPath());
                sb.append(s);
            }
        }
        return sb.toString();
    }

    private String stepsToString(){
        StringBuilder sb = new StringBuilder();
        for (TektonStep s: steps) {
            StringBuilder argsSb = new StringBuilder();
            StringBuilder commandSb = new StringBuilder();
            StringBuilder envsSb = new StringBuilder();
            for (TektonEnv e: s.getEnvs()){
                String str = String.format("%n\t\t\tEnv Name: %s%n" +
                        "\t\t\tEnv Value: %s%n", e.getName(), e.getValue());
                envsSb.append(str);
            }
            String stepString = String.format("%n" +
                    "\t\tStep Name: %s%n" +
                    "\t\tStep Image: %s%n" +
                    "\t\tStep Args: %s%n" +
                    "\t\tStep Command: %s%n" +
                    "\t\tStep Envs: %s%n", s.getName(), s.getImage(), argsSb.toString(), commandSb.toString(), envsSb.toString());
            sb.append(stepString);
        }
        return sb.toString();
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

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Tekton : Create Task";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            req.bindJSON(this, formData); // Use stapler request to bind
            save();
            return true;
        }
    }
}
