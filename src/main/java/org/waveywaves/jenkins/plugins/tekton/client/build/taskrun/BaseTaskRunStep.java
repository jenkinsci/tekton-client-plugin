package org.waveywaves.jenkins.plugins.tekton.client.build.taskrun;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.pipeline.v1beta1.DoneableTaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunList;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;

public abstract class BaseTaskRunStep extends BaseStep {
    protected String input;
    protected String inputType;

    // Resource Specific Client for TaskRun
    protected transient MixedOperation<TaskRun, TaskRunList, DoneableTaskRun, Resource<TaskRun, DoneableTaskRun>>
            resourceSpecificClient;

    protected BaseTaskRunStep() {
        initResourceSpecificClient();
    }

    @Override
    protected void initResourceSpecificClient() {
        this.resourceSpecificClient =  tektonClient.v1beta1().taskRuns();
    }

    protected String getInput() {
        return this.input;
    }

    protected String getInputType() {
        return this.inputType;
    }
}
