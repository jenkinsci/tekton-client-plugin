package org.waveywaves.jenkins.plugins.tekton.client.resourcedesc;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public final class TektonParam extends AbstractDescribableImpl<TektonParam> {
    private final String name;
    private final String description;
    private final String type;
    private final String defaultValueString;
    private final List<TektonArrayParamI> defaultValueArray;


    @DataBoundConstructor
    public TektonParam(String name, String description, String type, String defaultValueString, List<TektonArrayParamI> defaultValueArray) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.defaultValueString = defaultValueString;
        this.defaultValueArray = defaultValueArray;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getDefaultValueString() {
        return defaultValueString;
    }

    public List<TektonArrayParamI> getDefaultValueArray() {
        return defaultValueArray;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TektonParam> {
        @Override
        public String getDisplayName() {
            return "param";
        }
    }


}
