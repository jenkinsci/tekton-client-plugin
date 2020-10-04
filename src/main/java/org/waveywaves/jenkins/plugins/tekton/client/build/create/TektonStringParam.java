package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class TektonStringParam extends AbstractDescribableImpl<TektonStringParam> {
    private final String name;
    private final String description;
    private final String defaultValue;

    @DataBoundConstructor
    public TektonStringParam(final String name,
                       final String description,
                       final String defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<TektonStringParam> {
        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "param";
        }
    }


}
