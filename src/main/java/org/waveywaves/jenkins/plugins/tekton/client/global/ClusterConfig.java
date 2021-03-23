package org.waveywaves.jenkins.plugins.tekton.client.global;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public final class ClusterConfig extends AbstractDescribableImpl<ClusterConfig> {
    private final String name;
    private final String masterUrl;
    private final String defaultNamespace;

    @DataBoundConstructor
    public ClusterConfig(final String name,
                         final String masterUrl,
                         final String defaultNamespace) {
        this.name = name;
        this.masterUrl = masterUrl;
        this.defaultNamespace = defaultNamespace;
    }

    public String getMasterUrl() {
        return this.masterUrl;
    }

    public String getDefaultNamespace() {
        return this.defaultNamespace;
    }

    public String getName() {
        return name;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ClusterConfig> {
        @Override
        public String getDisplayName() {
            return "k8s cluster with Tekton";
        }
    }
}
