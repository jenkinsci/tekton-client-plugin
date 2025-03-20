package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import org.jenkinsci.Symbol;

public final class TektonArg extends AbstractDescribableImpl<TektonArg> {
    private final String value;

    @DataBoundConstructor
    public TektonArg(@NonNull String value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    @Exported
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "TektonArg{" + "value='" + value + '\'' + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TektonArg tektonArg = (TektonArg) obj;
        return Objects.equals(value, tektonArg.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Extension
    @Symbol("tektonArg")
    public static class DescriptorImpl extends Descriptor<TektonArg> {
        @Override
        public String getDisplayName() {
            return "Tekton Argument";
        }
    }
}
