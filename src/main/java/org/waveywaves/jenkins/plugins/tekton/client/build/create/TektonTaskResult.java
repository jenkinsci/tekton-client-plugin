package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class TektonTaskResult extends AbstractDescribableImpl<TektonTaskResult> {
    private final String name;
    private final String description;

    @DataBoundConstructor
    public TektonTaskResult(final String name,
                            final String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }



    @Extension
    public static class DescriptorImpl extends Descriptor<TektonTaskResult> {
        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "result";
        }
    }

}
