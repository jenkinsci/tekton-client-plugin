package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public class TektonStep extends AbstractDescribableImpl<TektonStep> {
    private final String name;
    private final String image;

    private List<TektonArg> args;
    private List<TektonCommandI> command;
    private List<TektonEnv> envs;
    private String script;
    private Boolean tty;
    private String workingDir;
    private String namespace;

    @DataBoundConstructor
    public TektonStep(String name, String image, List<TektonArg> args,
                      List<TektonCommandI> command, List<TektonEnv> envs,
                      String script, Boolean tty, String workingDir,
                      String namespace) {
        this.name = name;
        this.image = image;
        this.args = args;
        this.command = command;
        this.envs = envs;
        this.script = script;
        this.tty = tty;
        this.workingDir = workingDir;
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public List<TektonArg> getArgs() {
        return args;
    }

    public List<TektonCommandI> getCommand() {
        return command;
    }

    public String getScript() {
        return script;
    }

    public Boolean getTty() {
        return tty;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public List<TektonEnv> getEnvs() {
        return envs;
    }
    public String getNamespace() {
        return namespace;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TektonStep> {
        @Override public String getDisplayName() {
            return "step";
        }
    }
}
