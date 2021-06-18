package org.waveywaves.jenkins.plugins.tekton.client.global;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Main;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Node;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.util.XStream2;
import io.fabric8.tekton.client.TektonClient;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;


/**
 * Tekton cloud provider.
 *
 */
public class TektonCloud extends Cloud {

    private String serverUrl;
    @CheckForNull
    private String serverCertificate;
    private boolean skipTlsVerify;
    private String namespace;
    private String jenkinsUrl;

    public TektonCloud(String name) {
        super(name);
    }

    /**
     * Copy constructor.
     * Allows to create copies of the original kubernetes cloud. Since it's a singleton
     * by design, this method also allows specifying a new name.
     * @param name Name of the cloud to be created
     * @param source Source Kubernetes cloud implementation
     * @since 0.13
     */
    public TektonCloud(@NonNull String name, @NonNull TektonCloud source) {
        super(name);
        XStream2 xs = new XStream2();
        xs.omitField(Cloud.class, "name");
        xs.unmarshal(XStream2.getDefaultDriver().createReader(new StringReader(xs.toXML(source))), this);
    }

    @DataBoundConstructor
    public TektonCloud(String name, String serverUrl, String namespace) {
        this(name);

        setServerUrl(serverUrl);
        setNamespace(namespace);
        setJenkinsUrl(jenkinsUrl);
//        setContainerCapStr(containerCapStr);
//        setRetentionTimeout(retentionTimeout);
//        setConnectTimeout(connectTimeout);
//        setReadTimeout(readTimeout);

    }

    @Override
    public @Nonnull String getDisplayName() {
        return "Tekton Cloud";
    }

    @SuppressWarnings({"unused", "deprecation", "ConstantConditions"})
    private Object readResolve() {
        return this;
    }

    public String getServerUrl() {
        return this.serverUrl;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getServerCertificate() {
        return this.serverCertificate;
    }

    public Boolean isSkipTlsVerify() {
        return this.skipTlsVerify;
    }

    @CheckForNull
    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    @DataBoundSetter
    public void setServerUrl(@Nonnull String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @DataBoundSetter
    public void setServerCertificate(@Nonnull String serverCertificate) {
        this.serverCertificate = serverCertificate;
    }

    @DataBoundSetter
    public void setSkipTLSVerify(boolean skipTlsVerify) {
        this.skipTlsVerify = skipTlsVerify;
    }

    @DataBoundSetter
    public void setNamespace(String namespace) {
        this.namespace = Util.fixEmpty(namespace);
    }

    @DataBoundSetter
    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = Util.fixEmptyAndTrim(jenkinsUrl);
    }

    public TektonClient connect() throws IOException {
        TektonClient client = TektonClientProvider.createClient(this);
        return client;
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(@NonNull final Cloud.CloudState state, final int excessWorkload){
        return Collections.emptyList();
    }

    @Override
    public boolean canProvision(@NonNull Cloud.CloudState state) {
        return true;
    }

    @Initializer(after = InitMilestone.SYSTEM_CONFIG_LOADED)
    public static void hpiRunInit() {
        if (Main.isDevelopmentMode) {
            Jenkins jenkins = Jenkins.get();
            String hostAddress = System.getProperty("jenkins.host.address");
            if (hostAddress != null && jenkins.clouds.getAll(TektonCloud.class).isEmpty()) {
                TektonCloud cloud = new TektonCloud("tekton");
                cloud.setJenkinsUrl("http://" + hostAddress + ":8080/jenkins/");
                jenkins.clouds.add(cloud);
            }
        }
    }

}
