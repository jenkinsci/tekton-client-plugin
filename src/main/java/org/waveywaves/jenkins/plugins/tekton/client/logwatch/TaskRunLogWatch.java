package org.waveywaves.jenkins.plugins.tekton.client.logwatch;

import com.google.common.collect.Sets;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils.TektonResourceType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class TaskRunLogWatch implements Runnable{
    private static final Logger logger = Logger.getLogger(TaskRunLogWatch.class.getName());

    private KubernetesClient kubernetesClient;
    private TektonClient tektonClient;
    private TaskRun taskRun;
    private Exception exception;
    OutputStream consoleLogger;

    public TaskRunLogWatch(KubernetesClient kubernetesClient, TektonClient tektonClient, TaskRun taskRun, OutputStream consoleLogger) {
        this.kubernetesClient = kubernetesClient;
        this.tektonClient = tektonClient;
        this.taskRun = taskRun;
        this.consoleLogger = consoleLogger;
    }

    /**
     * @return the exception if the task run failed to succeed
     */
    public Exception getException() {
        return exception;
    }

    @Override
    public void run() {
        HashSet<String> runningPhases = Sets.newHashSet("Running", "Succeeded", "Failed");
        String ns = taskRun.getMetadata().getNamespace();
        List<Pod> pods = kubernetesClient.pods().inNamespace(ns).list().getItems();
        Pod taskRunPod = null;
        String podName = "";
        for (Pod pod : pods) {
            List<OwnerReference> ownerReferences = pod.getMetadata().getOwnerReferences();
            if (ownerReferences != null && ownerReferences.size() > 0) {
                for (OwnerReference or : ownerReferences) {
                    String orKind = or.getKind();
                    String orName = or.getName();
                    if (orKind.toLowerCase().equals(TektonResourceType.taskrun.toString())
                            && orName.equals(taskRun.getMetadata().getName())){
                        podName = pod.getMetadata().getName();
                        taskRunPod = pod;
                    }
                }
            }
        }

        if (!podName.isEmpty() && taskRunPod != null){
            logMessage("pod " + ns + "/" + podName + ":\n");

            logger.info("waiting for pod " + ns + "/" + podName + " to start running...");
            Predicate<Pod> succeededState = i -> (runningPhases.contains(i.getStatus().getPhase()));
            PodResource<Pod> pr = kubernetesClient.pods().inNamespace(ns).withName(podName);
            try {
                pr.waitUntilCondition(succeededState,60, TimeUnit.MINUTES);
            } catch ( InterruptedException e) {
                logger.warning("Interrupted Exception Occurred");
            }
            logMessage("\npod " + podName + " running:");
            List<String> taskRunContainerNames = new ArrayList<String>();
            for (Container c : taskRunPod.getSpec().getContainers()) {
                taskRunContainerNames.add(c.getName());
            }
            for (String containerName : taskRunContainerNames) {
                // lets write a little header per container
                logMessage("\n\n" + containerName + ":");

                // wait for the container to start
                logger.info("waiting for pod pod: " + ns + "/" + podName + " container: " + containerName + " to start:");

                Predicate<Pod> containerRunning = i -> {
                    List<ContainerStatus> statuses = i.getStatus().getContainerStatuses();
                    for (ContainerStatus status : statuses) {
                        if (status.getName().equals(containerName)) {
                            ContainerState state = status.getState();
                            if (state != null) {
                                ContainerStateTerminated terminatedState = state.getTerminated();
                                if (terminatedState != null && terminatedState.getStartedAt() != null) {
                                    logMessage("container " + containerName + " completed");
                                    return true;
                                }
                            }
                            return false;
                        }
                    }
                    return false;
                };
                try {
                    pr.waitUntilCondition(containerRunning,60, TimeUnit.MINUTES);
                } catch ( InterruptedException e) {
                    logger.warning("Interrupted Exception Occurred");
                }

                pr.inContainer(containerName).watchLog(this.consoleLogger);
            }
            logPodFailures(pr.get());
        } else {
            String message = "no pod could be found for TaskRun " + ns + "/" + taskRun.getMetadata().getName();
            logMessage(message);
            exception = new Exception(message);

            // lets reload to get the latest status
            taskRun = tektonClient.v1beta1().taskRuns().inNamespace(ns).withName(taskRun.getMetadata().getName()).get();
            logTaskRunFailure(taskRun);

        }
    }

    /**
     * Lets log any failures in the task run
     *
     * @param taskRun the task run to log
     */
    protected void logTaskRunFailure(TaskRun taskRun) {
        String name = taskRun.getMetadata().getName();
        List<Condition> conditions = taskRun.getStatus().getConditions();
        if (conditions == null || conditions.size() == 0) {
            logMessage("TaskRun " + name + " has no status conditions");
            return;
        }

        for (Condition condition : conditions) {
            logMessage("TaskRun " + name + " " + condition.getType() + "/" + condition.getReason() + ": " + condition.getMessage());
        }
    }

    /**
     * Lets check if the pod completed successfully otherwise log a failure message
     *
     * @param pod the pod to log
     */
    protected void logPodFailures(Pod pod) {
        String ns = pod.getMetadata().getNamespace();
        String podName = pod.getMetadata().getName();
        PodStatus status = pod.getStatus();
        String phase = status.getPhase();
        String message = "pod " + ns + "/" + podName + " status: " + phase;
        logMessage(message);

        // TODO we could try diagnose more information from the failed pod to log

        if (!phase.equals("Succeeded")) {
            exception = new Exception(message);
        }
    }


    protected void logMessage(String text) {
        try {
            this.consoleLogger.write((text + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.warning("failed to log to console: " + e);
        }
    }
}
