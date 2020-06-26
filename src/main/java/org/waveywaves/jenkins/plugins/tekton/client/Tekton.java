package org.waveywaves.jenkins.plugins.tekton.client;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *
 */
public class Tekton extends AbstractDescribableImpl<Tekton> {
    public static final String DEFAULT_LOGLEVEL = "0";

    @Extension
    public static class DescriptorImpl extends Descriptor<Tekton> {
        public List<ClusterConfig> clusterConfigs;

        /**
         * Set the displayName for the instance of clusterConfig
         * @return
         */
        @Override
        public String getDisplayName() {
            return "Kubernetes Configuration";
        }

        /**
         * Add a new Kubernetes Cluster Configuration
         * @param clusterConfig
         */
        public void addClusterConfig(ClusterConfig clusterConfig) {
            if (clusterConfigs == null) {
                clusterConfigs = new ArrayList<>(1);
            }
            clusterConfigs.add(clusterConfig);
        }

        /**
         * Remove existing Kubernetes Cluster Configuration
         * @param clusterConfig
         * @throws IllegalArgumentException
         */
        public void removeClusterConfig(ClusterConfig clusterConfig) throws IllegalArgumentException {
            if (clusterConfigs == null || clusterConfigs.size() <= 0) {
                throw new IllegalArgumentException("ClusterConfigs is null or empty");
            }
            clusterConfigs.remove(clusterConfig);
        }

        public List<ClusterConfig> getClusterConfigs() {
            if (clusterConfigs == null) {
                return new ArrayList<>(0);
            }
            return Collections.unmodifiableList(clusterConfigs);
        }

        public ClusterConfig getClusterConfig(String name) {
            if (clusterConfigs == null) {
                return null;
            }
            name = Util.fixEmptyAndTrim(name);
            for (ClusterConfig cc : clusterConfigs) {
                if (cc.getName().equalsIgnoreCase(name)) {
                    return cc;
                }
            }
            return null;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            /**
             * If all cluster configurations are deleted in the UI and saved,
             * binJSON does not set the list. So clear out the list before bind.
             */
            clusterConfigs = null;

            req.bindJSON(this, json.getJSONObject("tektonClientConfig"));
            save();
            return true;
        }
    }
}
