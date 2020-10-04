package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public final class TektonArg extends AbstractDescribableImpl<TektonArg> {
    private final String value;

    @DataBoundConstructor
    public TektonArg(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<TektonArg> {
        @Override
        public String getDisplayName() {
            return "arg";
        }
    }
}
