package org.waveywaves.jenkins.plugins.tekton.client;

import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;

import java.util.logging.Logger;

public class TektonUtils {
    private static final Logger logger = Logger.getLogger(GlobalPluginConfiguration.class.getName());

    private static TektonClient tektonClient;

    public synchronized static void initializeTektonClient(String serverUrl) {
        if (serverUrl != null && !serverUrl.isEmpty()) {
            logger.info("ServerUrl has been passed to Tekton Client ");
        }
        tektonClient = new DefaultTektonClient();
        logger.info("Tekton Client Master URL" + tektonClient.getMasterUrl().toString());
    }

    public synchronized static void shutdownTektonClient() {
        if (tektonClient != null) {
            tektonClient.close();
            tektonClient = null;
        }
    }
}
