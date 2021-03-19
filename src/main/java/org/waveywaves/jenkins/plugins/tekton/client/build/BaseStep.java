package org.waveywaves.jenkins.plugins.tekton.client.build;

import hudson.tasks.Builder;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.pipeline.v1beta1.*;
import io.fabric8.tekton.resource.v1alpha1.PipelineResource;
import io.fabric8.tekton.resource.v1alpha1.PipelineResourceList;
import jenkins.tasks.SimpleBuildStep;

public abstract class BaseStep extends Builder implements SimpleBuildStep {
    protected transient Client tektonClient;
    protected transient Client kubernetesClient;

    protected MixedOperation<TaskRun, TaskRunList, Resource<TaskRun>>
            taskRunClient;
    protected MixedOperation<Task, TaskList, Resource<Task>>
            taskClient;
    protected MixedOperation<Pipeline, PipelineList, Resource<Pipeline>>
            pipelineClient;
    protected MixedOperation<PipelineRun, PipelineRunList, Resource<PipelineRun>>
            pipelineRunClient;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    protected MixedOperation<PipelineResource, PipelineResourceList, Resource<PipelineResource>>
            pipelineResourceClient;

    public enum InputType {
        URL,
        YAML,
        FILE,
        Interactive
    }

    public void setKubernetesClient(Client kc) {
        this.kubernetesClient = kc;
    }

    public void setTektonClient(Client tc) {
        this.tektonClient = tc;
    }

    public void setTaskRunClient(
            MixedOperation<TaskRun, TaskRunList, Resource<TaskRun>> trc){
        this.taskRunClient = trc;
    }

    public void setTaskClient(
            MixedOperation<Task, TaskList, Resource<Task>> tc){
        this.taskClient = tc;
    }

    public void setPipelineClient(
            MixedOperation<Pipeline, PipelineList, Resource<Pipeline>> pc){
        this.pipelineClient = pc;
    }

    public void setPipelineRunClient(
            MixedOperation<PipelineRun, PipelineRunList, Resource<PipelineRun>> prc){
        this.pipelineRunClient = prc;
    }

    public void setPipelineResourceClient(
            MixedOperation<PipelineResource, PipelineResourceList, Resource<PipelineResource>> presc){
        this.pipelineResourceClient = presc;
    }
}
