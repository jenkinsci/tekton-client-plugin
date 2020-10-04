package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class TektonParam extends AbstractDescribableImpl<TektonParam> {
    private final String name;
    private final String value;

    @DataBoundConstructor
    public TektonParam(final String name,
                       final String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TektonParam> {
        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "param";
        }
    }


}
