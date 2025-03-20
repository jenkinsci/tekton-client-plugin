package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class TektonParam extends AbstractDescribableImpl<TektonParam> {
    
    private final String name;
    private final String value;

    @DataBoundConstructor
    public TektonParam(@NonNull final String name, @NonNull final String value) {
        this.name = name;
        this.value = value;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getValue() {
        return value;
    }

    @Extension
    @Symbol("tektonParam")
    public static class DescriptorImpl extends Descriptor<TektonParam> {
        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Tekton Parameter";
        }
    }
}
