package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public final class TektonWorkspace extends AbstractDescribableImpl<TektonWorkspace> {
    private final String name;
    private final String description;

    @DataBoundConstructor
    public TektonWorkspace(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TektonWorkspace> {
        @Override
        public String getDisplayName() {
            return "workspace";
        }
    }
}
