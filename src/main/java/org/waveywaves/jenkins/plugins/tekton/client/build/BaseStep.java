package org.waveywaves.jenkins.plugins.tekton.client.build;

import hudson.tasks.Builder;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.*;
import io.fabric8.tekton.resource.v1alpha1.DoneablePipelineResource;
import io.fabric8.tekton.resource.v1alpha1.PipelineResource;
import io.fabric8.tekton.resource.v1alpha1.PipelineResourceList;
import jenkins.tasks.SimpleBuildStep;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;

public abstract class BaseStep extends Builder implements SimpleBuildStep {
    protected transient Client tektonClient;

    protected MixedOperation<TaskRun, TaskRunList, DoneableTaskRun, Resource<TaskRun, DoneableTaskRun>>
            taskRunClient;
    protected MixedOperation<Task, TaskList, DoneableTask, Resource<Task, DoneableTask>>
            taskClient;
    protected MixedOperation<Pipeline, PipelineList, DoneablePipeline, Resource<Pipeline, DoneablePipeline>>
            pipelineClient;
    protected MixedOperation<PipelineRun, PipelineRunList, DoneablePipelineRun, Resource<PipelineRun, DoneablePipelineRun>>
            pipelineRunClient;
    protected MixedOperation<PipelineResource, PipelineResourceList, DoneablePipelineResource, Resource<PipelineResource, DoneablePipelineResource>>
            pipelineResourceClient;

    public enum InputType {
        URL,
        YAML,
        Interactive
    }

    public void setTektonClient(Client tc) {
        this.tektonClient = tc;
    }

    public void setTaskRunClient(
            MixedOperation<TaskRun, TaskRunList, DoneableTaskRun, Resource<TaskRun, DoneableTaskRun>> trc){
        this.taskRunClient = trc;
    }

    public void setTaskClient(
            MixedOperation<Task, TaskList, DoneableTask, Resource<Task, DoneableTask>> tc){
        this.taskClient = tc;
    }

    public void setPipelineClient(
            MixedOperation<Pipeline, PipelineList, DoneablePipeline, Resource<Pipeline, DoneablePipeline>> pc){
        this.pipelineClient = pc;
    }

    public void setPipelineRunClient(
            MixedOperation<PipelineRun, PipelineRunList, DoneablePipelineRun, Resource<PipelineRun, DoneablePipelineRun>> prc){
        this.pipelineRunClient = prc;
    }

    public void setPipelineResourceClient(
            MixedOperation<PipelineResource, PipelineResourceList, DoneablePipelineResource, Resource<PipelineResource, DoneablePipelineResource>> presc){
        this.pipelineResourceClient = presc;
    }
}
