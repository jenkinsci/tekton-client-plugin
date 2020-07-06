package org.waveywaves.jenkins.plugins.tekton.client;

import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1alpha1.Pipeline;
import io.fabric8.tekton.pipeline.v1alpha1.PipelineList;
import io.fabric8.tekton.pipeline.v1alpha1.Task;
import io.fabric8.tekton.pipeline.v1alpha1.TaskList;

import java.util.List;
import java.util.logging.Logger;

public class TektonUtils {
    private static final Logger logger = Logger.getLogger(GlobalPluginConfiguration.class.getName());

    private static TektonClient tektonClient;

    public synchronized static void initializeTektonClient(String serverUrl) {
        if (serverUrl != null && !serverUrl.isEmpty()) {
            logger.info("ServerUrl has been passed to Tekton Client ");
        }
        String namespace = "jenkins-test";
        tektonClient = new DefaultTektonClient();

        List<Task> taskList = tektonClient.tasks().inNamespace(namespace).list().getItems();
        List<Pipeline> pipelineList = tektonClient.pipelines().inNamespace(namespace).list().getItems();
        for (Task t: taskList){
           logger.info( "Tekton Task found " + t.getMetadata().getName());
        }
        for (Pipeline p: pipelineList){
            logger.info( "Tekton Pipeline found " + p.getMetadata().getName());
        }
    }

    public synchronized static void shutdownTektonClient() {
        if (tektonClient != null) {
            tektonClient.close();
            tektonClient = null;
        }
    }
}
