package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.tekton.pipeline.v1beta1.Step;
import io.fabric8.tekton.pipeline.v1beta1.TaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskSpec;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils.TektonResourceType;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;
import org.waveywaves.jenkins.plugins.tekton.client.resourcedesc.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Symbol("customCreateStep")
public class CreateCustomStep extends BaseStep {
    private static final Logger logger = Logger.getLogger(CreateCustomStep.class.getName());

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
    public CreateCustomStep(final String kind,
                            final String name,
                            final String namespace,
                            final String description,
                            List<TektonParam> params,
                            List<TektonWorkspace> workspaces,
                            final List<TektonStep> steps){
        super();

        // ObjectMeta
        this.kind = kind;
        this.name = name;
        this.namespace = namespace;

        // Task
        this.description = description;
        this.params = params;
        this.steps = steps;
        this.workspaces = workspaces;
    }

    public String getKind() {
        return this.kind;
    }

    public String getName() {
        return this.name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getDescription() {
        return this.description;
    }

    public List<TektonParam> getParams() {
        return params;
    }

    public List<TektonStep> getSteps(){
        return this.steps;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        consoleLogger = taskListener.getLogger();
        consoleLogger.println("Creating Resource from Custom Config");
        String l = String.format("Kind: %s\n" +
                "Name: %s\n" +
                "Namespace: %s\n",getKind(), getName(), getNamespace());
        consoleLogger.print(l);
    }

    public List<TektonWorkspace> getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(List<TektonWorkspace> workspaces) {
        this.workspaces = workspaces;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
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
            return "Tekton : Create Custom Resource";
        }
    }
}
