package org.waveywaves.jenkins.plugins.tekton.client.global;

import hudson.Extension;
import io.fabric8.kubernetes.client.KubernetesClientException;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

@Extension
public class TektonGlobalConfiguration extends GlobalConfiguration {
    private static final Logger logger = Logger.getLogger(TektonGlobalConfiguration.class.getName());
    private List<ClusterConfig> clusterConfigs = new ArrayList<>();

    public TektonGlobalConfiguration(){
        load();
        configChange();
    }

    public List<ClusterConfig> getClusterConfigs() {
        return this.clusterConfigs;
    }

    public void setClusterConfigs(List<ClusterConfig> clusterConfigs) {
        this.clusterConfigs = clusterConfigs;
    }

    public static TektonGlobalConfiguration get() {
        return GlobalConfiguration.all().get(TektonGlobalConfiguration.class);
    }

    @Override
    public boolean configure(final StaplerRequest req, final JSONObject formData) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        setClusterConfigs(req.bindJSONToList(ClusterConfig.class, formData.get("configurations")));
        save();
        return false;
    }

    public synchronized void configChange() {
        logger.info("Tekton Client Plugin processing a newly supplied configuration");

        TektonUtils.shutdownKubeClients();
        try {
            TektonUtils.initializeKubeClients(this.clusterConfigs);
        } catch (KubernetesClientException e){
            Throwable exceptionOrCause = (e.getCause() != null) ? e.getCause() : e;
            logger.log(SEVERE, "Failed to configure Tekton Client Plugin: " + exceptionOrCause);
        }
    }


}