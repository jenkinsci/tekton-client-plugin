package org.waveywaves.jenkins.plugins.tekton.client.logwatch;

import com.google.common.base.Strings;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PipelineRunLogWatch implements Runnable {
    private static final Logger logger = Logger.getLogger(PipelineRunLogWatch.class.getName());

    private KubernetesClient kubernetesClient;
    private TektonClient tektonClient;
    private PipelineRun pipelineRun;
    private Exception exception;
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

    /**
     * @return the exception if the pipeline failed to succeed
     */
    public Exception getException() {
        return exception;
    }

    @Override
    public void run() {
        String pipelineRunName = pipelineRun.getMetadata().getName();
        String pipelineRunUid = pipelineRun.getMetadata().getUid();
        String ns = pipelineRun.getMetadata().getNamespace();

        List<PipelineTask> pipelineTasks = pipelineRun.getSpec().getPipelineSpec().getTasks();

        for (PipelineTask pt: pipelineTasks){
            String pipelineTaskName = pt.getName();
            logger.info("streaming logs for PipelineTask " + ns + "/" + pipelineRunName + "/" + pipelineTaskName);
            ListOptions lo = new ListOptions();
            String selector = String.format("%s=%s,%s=%s", pipelineTaskLabelName, pipelineTaskName, pipelineRunLabelName, pipelineRunName);
            lo.setLabelSelector(selector);

            // the tekton operator may not have created the TasksRuns yet so lets wait a little bit for them to show up
            for (int i = 0; i < 60; i++) {
                boolean taskComplete = false;
                List<TaskRun> taskRunList = tektonClient.v1beta1().taskRuns().inNamespace(ns).list(lo).getItems();
                for (TaskRun tr : taskRunList) {
                    String trName = tr.getMetadata().getName();
                    if (Strings.isNullOrEmpty(tr.getMetadata().getNamespace())) {
                        tr.getMetadata().setNamespace(ns);
                    }
                    logger.info("streaming logs for TaskRun " + trName);

                    List<OwnerReference> ownerReferences = tr.getMetadata().getOwnerReferences();
                    for (OwnerReference or : ownerReferences) {
                        if (or.getUid().equals(pipelineRunUid)) {
                            logger.info(String.format("Streaming logs for TaskRun %s/%s owned by PipelineRun %s with selector %s", ns, trName, pipelineRunName, selector));
                            TaskRunLogWatch logWatch = new TaskRunLogWatch(kubernetesClient, tektonClient, tr, consoleLogger);
                            Thread logWatchTask = new Thread(logWatch);
                            logWatchTask.start();
                            try {
                                logWatchTask.join();
                            } catch (InterruptedException exception) {
                                exception.printStackTrace();
                            }
                            Exception e = logWatch.getException();
                            if (e != null) {
                                logger.info("TaskRun " + trName + " failed");
                                if (exception == null) {
                                    exception = e;
                                }
                            } else {
                                logger.info("TaskRun " + trName + " completed");
                            }
                            taskComplete = true;
                        }
                    }
                }
                if (taskComplete) {
                    logMessage("completed PipelineTask " + pipelineTaskName);
                    break;
                } else {
                    logMessage("could not find OwnerReference for " + pipelineRunUid);
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

    protected void logMessage(String text) {
        try {
            this.consoleLogger.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.warning("failed to log to console: " + e);
        }
    }
}
