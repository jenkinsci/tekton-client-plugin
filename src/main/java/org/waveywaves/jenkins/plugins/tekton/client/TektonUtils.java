package org.waveywaves.jenkins.plugins.tekton.client;

import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.Task;

import java.util.List;
import java.util.logging.Logger;

public class TektonUtils {
    private static final Logger logger = Logger.getLogger(GlobalPluginConfiguration.class.getName());

    private static TektonClient tektonClient;

    public synchronized static void initializeTektonClient(String serverUrl) {
        if (serverUrl != null && !serverUrl.isEmpty()) {
            logger.info("ServerUrl has been passed to Tekton Client ");
        }

        tektonClient = new DefaultTektonClient();
        String namespace = tektonClient.getNamespace();

        logger.info("Running in namespace "+namespace);

        List<Task> taskList = tektonClient.v1beta1().tasks().list().getItems();
        List<Pipeline> pipelineList = tektonClient.v1beta1().pipelines().list().getItems();

        for (Task t: taskList) {
            logger.info("Tekton Task "+t.getMetadata().getName());
        }
        for (Pipeline t: pipelineList) {
            logger.info("Tekton Pipeline "+t.getMetadata().getName());
        }
    }

    public synchronized static void shutdownTektonClient() {
        if (tektonClient != null) {
            tektonClient.close();
            tektonClient = null;
        }
    }
}
