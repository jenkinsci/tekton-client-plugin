package org.waveywaves.jenkins.plugins.tekton.client.logwatch;

import com.google.common.collect.Sets;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ListOptions;
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
    private static final Logger LOGGER = Logger.getLogger(TaskRunLogWatch.class.getName());

    private static final String TASK_RUN_LABEL_NAME = "tekton.dev/taskRun";

    // TODO should be final
    private TaskRun taskRun;

    private KubernetesClient kubernetesClient;
    private TektonClient tektonClient;

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
        ListOptions lo = new ListOptions();
        String selector = String.format("%s=%s", TASK_RUN_LABEL_NAME, taskRun.getMetadata().getName());
        lo.setLabelSelector(selector);
        List<Pod> pods = null;
        for (int i = 0; i < 60; i++) {
            pods = kubernetesClient.pods().inNamespace(ns).list(lo).getItems();
            LOGGER.info("Found " + pods.size() + " pod(s) for taskRun " + taskRun.getMetadata().getName());
            if (pods.size() > 0) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


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

        final String selectedPodName = podName;
        if (!podName.isEmpty() && taskRunPod != null){
            logMessage(String.format("[Tekton] Pod %s/%s", ns, podName));

            LOGGER.info("waiting for pod " + ns + "/" + podName + " to start running...");
            Predicate<Pod> succeededState = i -> (runningPhases.contains(i.getStatus().getPhase()));
            PodResource<Pod> pr = kubernetesClient.pods().inNamespace(ns).withName(podName);
            try {
                pr.waitUntilCondition(succeededState,60, TimeUnit.MINUTES);
            } catch ( InterruptedException e) {
                LOGGER.warning("Interrupted Exception Occurred");
            }
            logMessage(String.format("[Tekton] Pod %s/%s - Running...", ns, podName));
            List<String> taskRunContainerNames = new ArrayList<String>();
            for (Container c : taskRunPod.getSpec().getContainers()) {
                taskRunContainerNames.add(c.getName());
            }

            for (String containerName : taskRunContainerNames) {
                // lets write a little header per container
                logMessage(String.format("[Tekton] Container %s/%s/%s", ns, podName, containerName));

                // wait for the container to start
                LOGGER.info("waiting for pod: " + ns + "/" + podName + " container: " + containerName + " to start:");

                Predicate<Pod> containerRunning = i -> {
                    List<ContainerStatus> statuses = i.getStatus().getContainerStatuses();
                    for (ContainerStatus status : statuses) {
                        if (status.getName().equals(containerName)) {
                            LOGGER.info("Found status " + status + " for container " + containerName);
                            ContainerState state = status.getState();
                            if (state != null) {
                                ContainerStateTerminated terminatedState = state.getTerminated();
                                if (terminatedState != null && terminatedState.getStartedAt() != null) {
                                    if (terminatedState.getExitCode() != null && terminatedState.getExitCode() != 0) {
                                        logMessage(String.format("[Tekton] Container %s/%s/%s - %s", ns, selectedPodName, containerName, terminatedState.getReason()));
                                    } else {
                                        logMessage(String.format("[Tekton] Container %s/%s/%s - Completed", ns, selectedPodName, containerName));
                                    }
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
                    LOGGER.warning("Interrupted Exception Occurred");
                }

                pr.inContainer(containerName).watchLog(this.consoleLogger);
            }
            logPodFailures(pr.get());
        } else {
            String message = "no pod could be found for TaskRun " + ns + "/" + taskRun.getMetadata().getName();
            logMessage("[Tekton] " + message);
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
        if (taskRun.getStatus() != null) {
            List<Condition> conditions = taskRun.getStatus().getConditions();
            if (conditions == null || conditions.size() == 0) {
                logMessage("[Tekton] TaskRun " + name + " has no status conditions");
                return;
            }

            for (Condition condition : conditions) {
                logMessage("[Tekton] TaskRun " + name + " " + condition.getType() + "/" + condition.getReason() + ": " + condition.getMessage());
            }
        } else {
            logMessage("[Tekton] TaskRun " + name + " has no status");
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
        String message = "Pod " + ns + "/" + podName + " Status: " + phase;
        logMessage("[Tekton] " + message);

        // Check if all containers completed successfully
        boolean allContainersSucceeded = true;
        List<ContainerStatus> containerStatuses = status.getContainerStatuses();
        
        if (containerStatuses != null) {
            for (ContainerStatus containerStatus : containerStatuses) {
                ContainerState state = containerStatus.getState();
                if (state != null && state.getTerminated() != null) {
                    ContainerStateTerminated terminated = state.getTerminated();
                    if (terminated.getExitCode() == null || terminated.getExitCode() != 0) {
                        allContainersSucceeded = false;
                        break;
                    }
                } else {
                    // Container hasn't terminated yet, so not complete
                    allContainersSucceeded = false;
                    break;
                }
            }
        } else {
            allContainersSucceeded = false;
        }

        // Only set exception if pod failed OR containers failed, not if pod is just running
        if (phase.equals("Failed") || (!allContainersSucceeded && !phase.equals("Running"))) {
            exception = new Exception(message);
        }
    }


    protected void logMessage(String text) {
        try {
            this.consoleLogger.write((text + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.warning("failed to log to console: " + e);
        }
    }
}
