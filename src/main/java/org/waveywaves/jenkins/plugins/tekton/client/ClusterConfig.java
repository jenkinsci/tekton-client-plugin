package org.waveywaves.jenkins.plugins.tekton.client;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Kubernetes Cluster Configuration
 */
public class ClusterConfig extends AbstractDescribableImpl<ClusterConfig> implements Serializable {
    private String name;
    private String apiServer;

    @DataBoundConstructor
    public ClusterConfig(String configName) {
        this.name = configName;
    }
    public String getName() {
        return this.name;
    }

    // API Server URL getter&setter

    public String getAPIServer() {
        return this.apiServer;
    }
    @DataBoundSetter
    public void setAPIServer(String apiServer){
        this.apiServer = apiServer;
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<ClusterConfig> {
        @Override
        public String getDisplayName() {
            return "Kuberentes Cluster";
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckApiServer(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }
    }
}
