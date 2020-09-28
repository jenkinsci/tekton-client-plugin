package org.waveywaves.jenkins.plugins.tekton.client.resourcedesc;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public final class TektonArrayParamI extends AbstractDescribableImpl<TektonArrayParamI> {
    private final String value;

    @DataBoundConstructor
    public TektonArrayParamI(String text) {
        this.value = text;
    }

    public String getValue() {
        return value;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TektonArrayParamI> {
        @Override
        public String getDisplayName() {
            return "";
        }
    }
}
