package org.waveywaves.jenkins.plugins.tekton.client;

import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.Task;

import java.util.List;
import java.util.logging.Logger;

public class TektonUtils {
    private static final Logger logger = Logger.getLogger(TektonUtils.class.getName());

    private static TektonClient tektonClient;

    public synchronized static void initializeTektonClient(String serverUrl) {
        if (serverUrl != null && !serverUrl.isEmpty()) {
            logger.info("ServerUrl has been passed to Tekton Client ");
        }
        tektonClient = new DefaultTektonClient();
        String namespace = tektonClient.getNamespace();
        logger.info("Running in namespace "+namespace);
    }

    public synchronized static void shutdownTektonClient() {
        if (tektonClient != null) {
            tektonClient.close();
            tektonClient = null;
        }
    }

    public synchronized static TektonClient getTektonClient(){
        return tektonClient;
    }
}
