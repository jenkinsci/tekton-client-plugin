package org.waveywaves.jenkins.plugins.tekton.client;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.logging.Logger;

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
    }

    public static GlobalPluginConfiguration get() {
        return GlobalConfiguration.all().get(GlobalPluginConfiguration.class);
    }



}
