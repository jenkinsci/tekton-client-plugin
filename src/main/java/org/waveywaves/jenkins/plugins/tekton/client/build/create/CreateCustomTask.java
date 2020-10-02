package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jnr.ffi.Struct;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
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
    private List<TektonParam> params;
    private List<TektonWorkspace> workspaces;

    @DataBoundConstructor
    public CreateCustomTask(final String name,
                            final String namespace,
                            final String description,
                            final List<TektonParam> params,
                            final List<TektonWorkspace> workspaces,
                            final List<TektonStep> steps){
        super();
        this.kind = TektonUtils.TektonResourceType.task.toString();
        this.name = name;
        this.namespace = namespace;
        this.description = description;
        this.params = params;
        this.steps = steps;
        this.workspaces = workspaces;
    }

    public String getKind() { return this.kind; }
    public String getName() { return this.name; }
    public String getNamespace() { return this.namespace; }
    public String getDescription() { return this.description; }
    public List<TektonParam> getParams() { return this.params; }
    public List<TektonStep> getSteps() { return this.steps; }
    public List<TektonWorkspace> getWorkspaces() { return this.workspaces; }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        consoleLogger = taskListener.getLogger();
        consoleLogger.println("Creating Resource from Custom Config");
        String l = String.format("Kind: %s\n" +
                "Name: %s\n" +
                "Namespace: %s\n" +
                "Description: %s\n" +
                "Param: %s\n", getKind(), getName(), getNamespace(), getDescription(), paramsToString());
        consoleLogger.print(l);
    }

    private String paramsToString(){
        StringBuilder sb = new StringBuilder();

        for (TektonParam p: params) {
            TektonParam.Param param = p.getParam();
            String s = String.format("Param Name: %s\n" +
                    "Param Description: %s\n" +
                    "Param Type: %s\n",p.getName(), p.getDescription(), param.getClass());
            sb.append(s);

            if (param.getClass().equals(TektonParam.StringParam.class)){
                sb.append(String.format("Param Value: %s\n", ((TektonParam.StringParam) param).getValue()));
            }
            if (param.getClass().equals(TektonParam.ArrayParam.class)){

            }
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
            return "Tekton : Create Custom Task";
        }
    }
}
