package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public final class TektonWorkspaceBind extends AbstractDescribableImpl<TektonWorkspaceBind> {
    private final String name;
    private final String claimName;

    @DataBoundConstructor
    public TektonWorkspaceBind(String name, String claimName) {
        this.name = name;
        this.claimName = claimName;
    }

    public String getName() {
        return name;
    }

    public String getClaimName() {
        return claimName;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TektonWorkspaceBind> {
        @Override
        public String getDisplayName() {
            return "workspace";
        }
    }
}
