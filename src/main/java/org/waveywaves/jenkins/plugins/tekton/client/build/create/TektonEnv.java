package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public final class TektonEnv extends AbstractDescribableImpl<TektonEnv> {
    private final String name;
    private final String value;

    @DataBoundConstructor
    public TektonEnv(String name,String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TektonEnv> {
        @Override
        public String getDisplayName() {
            return "env";
        }
    }
}
