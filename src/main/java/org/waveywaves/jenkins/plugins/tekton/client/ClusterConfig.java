package org.waveywaves.jenkins.plugins.tekton.client;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.Serializable;

/**
 * Kubernetes Cluster Configuration
 */
public class ClusterConfig extends AbstractDescribableImpl<ClusterConfig> implements Serializable {
    private String name;
    private String apiServer;
    private String serverCertificateAuthority;
    private boolean skipTlsVerify;
    private String defaultNamespace;
    private String credentialsId;

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
    public void setAPIServer(String apiServer) {
        this.apiServer = apiServer;
    }

    // serverCertificateAuthority getter&setter
    public String getServerCertificateAuthority() {
        return this.serverCertificateAuthority;
    }
    @DataBoundSetter
    public void setServerCertificateAuthority(String serverCertificateAuthority) {
        this.serverCertificateAuthority = Util.fixEmptyAndTrim(serverCertificateAuthority);
    }

    // skiptlsverify getter&setter
    public boolean isSkipTlsVerify() {
        return this.skipTlsVerify;
    }
    @DataBoundSetter
    public void setSkipTlsVerify(boolean skipTLSVerify) {
        this.skipTlsVerify = skipTLSVerify;
    }

    // defaultNamespace getter&setter
    public String getDefaultNamespace() {
        return this.defaultNamespace;
    }
    @DataBoundSetter
    public void setDefaultProject(String defaultProject) {
        this.defaultNamespace = Util.fixEmptyAndTrim(defaultNamespace);
    }

    // credentialsID getter&setter
    public String getCredentialsId() {
        return this.credentialsId;
    }
    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
    }

    @Override
    public String toString() {
        return String.format("Kubernetes Cluster [name:%s] [apiServer:%s]", this.name, this.apiServer);
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
