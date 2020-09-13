package org.waveywaves.jenkins.plugins.tekton.client.logwatch;

import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.*;
import net.sf.ezmorph.array.BooleanObjectArrayMorpher;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils.TektonResourceType;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
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
            ListOptions lo = new ListOptions();
            lo.setLabelSelector(String.format("%s=%s",pipelineTaskLabelName, pt.getName()));
            lo.setLabelSelector(String.format("%s=%s", pipelineRunLabelName, pipelineRunName));

            List<TaskRun> taskRunList = tektonClient.v1beta1().taskRuns().list(lo).getItems();
            for (TaskRun tr: taskRunList) {
                List<OwnerReference> ownerReferences = tr.getMetadata().getOwnerReferences();
                for (OwnerReference or : ownerReferences) {
                    if (or.getUid().equals(pipelineRunUid)){
                        logger.info(String.format("Streaming logs for TaskRun %s owned by PipelineRun %s", tr.getMetadata().getName(), pipelineRunName));
                        TaskRunLogWatch logWatch = new TaskRunLogWatch(kubernetesClient, tr, consoleLogger);
                        Thread logWatchTask = new Thread(logWatch);
                        logWatchTask.start();
                        try {
                            logWatchTask.join();
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                        }
                    }
                }

            }
        }
    }
}
