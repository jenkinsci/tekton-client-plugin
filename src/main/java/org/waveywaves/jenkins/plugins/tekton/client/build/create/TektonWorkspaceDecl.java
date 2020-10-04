package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public final class TektonWorkspaceDecl extends AbstractDescribableImpl<TektonWorkspaceDecl> {
    private final String name;
    private final String description;
    private final String mountPath;

    @DataBoundConstructor
    public TektonWorkspaceDecl(String name, String description, String mountPath) {
        this.name = name;
        this.description = description;
        this.mountPath = mountPath;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMountPath() {
        return mountPath;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TektonWorkspaceDecl> {
        @Override
        public String getDisplayName() {
            return "workspace";
        }
    }
}
