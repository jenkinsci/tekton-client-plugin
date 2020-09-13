package org.waveywaves.jenkins.plugins.tekton.client;

import hudson.Extension;
import io.fabric8.kubernetes.client.KubernetesClientException;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

@Extension
public class GlobalPluginConfiguration extends GlobalConfiguration {
    private static final Logger logger = Logger.getLogger(GlobalPluginConfiguration.class.getName());

    private String server;

    @DataBoundConstructor
    public GlobalPluginConfiguration(String server) {
        this.server = server;
    }

    public String getServer() {
        return server;
    }
    public void setServer(String server) {
        this.server = server;
    }

    public GlobalPluginConfiguration(){
        load();
        configChange();
    }

    public static GlobalPluginConfiguration get() {
        return GlobalConfiguration.all().get(GlobalPluginConfiguration.class);
    }

    private synchronized void configChange() {
        logger.info("Tekton Client Plugin processing a newly supplied configuration");
        TektonUtils.shutdownKubeClients();
        try {
            TektonUtils.initializeKubeClients(this.server);
        } catch (KubernetesClientException e){
            Throwable exceptionOrCause = (e.getCause() != null) ? e.getCause() : e;
            logger.log(SEVERE, "Failed to configure Tekton Client Plugin: " + exceptionOrCause);
        }
    }

}
