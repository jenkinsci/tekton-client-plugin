package org.waveywaves.jenkins.plugins.tekton.client.logwatch;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.*;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PipelineRunLogWatch implements Runnable {
    private static final Logger logger = Logger.getLogger(PipelineRunLogWatch.class.getName());

    private KubernetesClient kubernetesClient;
    private TektonClient tektonClient;
    private PipelineRun pipelineRun;
    OutputStream consoleLogger;

    ConcurrentHashMap<String, TaskRun> taskRunsOnWatch = new ConcurrentHashMap<String, TaskRun>();
    ConcurrentHashMap<String, Boolean> taskRunsWatchDone = new ConcurrentHashMap<String, Boolean>();

    private final String pipelineTaskLabelName = "tekton.dev/pipelineTask";
    private final String pipelineRunLabelName = "tekton.dev/pipelineRun";

    public PipelineRunLogWatch(KubernetesClient kubernetesClient, TektonClient tektonClient, PipelineRun pipelineRun, OutputStream consoleLogger) {
        this.kubernetesClient = kubernetesClient;
        this.tektonClient = tektonClient;
        this.pipelineRun = pipelineRun;
        this.consoleLogger = consoleLogger;
    }

    @Override
    public void run() {
        String pipelineRunName = pipelineRun.getMetadata().getName();
        String pipelineRunUid = pipelineRun.getMetadata().getUid();
        List<PipelineTask> pipelineTasks = pipelineRun.getSpec().getPipelineSpec().getTasks();

        for (PipelineTask pt: pipelineTasks){
            String pipelineTaskName = pt.getName();
            logger.info("streaming logs for PipelineTask " + pipelineRunName + "/" + pipelineTaskName);
            ListOptions lo = new ListOptions();
            String selector = String.format("%s=%s,%s=%s", pipelineTaskLabelName, pipelineTaskName, pipelineRunLabelName, pipelineRunName);
            lo.setLabelSelector(selector);

            // the tekton operator may not have created the TasksRuns yet so lets wait a little bit for them to show up
            for (int i = 0; i < 60; i++) {
                boolean taskComplete = false;
                List<TaskRun> taskRunList = tektonClient.v1beta1().taskRuns().list(lo).getItems();
                for (TaskRun tr : taskRunList) {
                    String trName = tr.getMetadata().getName();
                    logger.info("streaming logs for TaskRun " + trName);

                    List<OwnerReference> ownerReferences = tr.getMetadata().getOwnerReferences();
                    for (OwnerReference or : ownerReferences) {
                        if (or.getUid().equals(pipelineRunUid)) {
                            logger.info(String.format("Streaming logs for TaskRun %s owned by PipelineRun %s with selector %s", trName, pipelineRunName, selector));
                            TaskRunLogWatch logWatch = new TaskRunLogWatch(kubernetesClient, tr, consoleLogger);
                            Thread logWatchTask = new Thread(logWatch);
                            logWatchTask.start();
                            try {
                                logWatchTask.join();
                            } catch (InterruptedException exception) {
                                exception.printStackTrace();
                            }
                            logger.info("TaskRun " + trName + " completed");
                            taskComplete = true;
                        }
                    }
                }
                if (taskComplete) {
                    logger.info("completed PipelineTask " + pipelineTaskName);
                    break;
                } else {
                    logger.info("could not find OwnerReference for " + pipelineRunUid);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                    e.printStackTrace();
                }
            }
        }
    }
}
