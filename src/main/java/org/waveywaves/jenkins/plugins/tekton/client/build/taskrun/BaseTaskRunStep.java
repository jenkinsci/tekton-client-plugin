package org.waveywaves.jenkins.plugins.tekton.client.build.taskrun;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.pipeline.v1beta1.DoneableTaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunList;
import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;

public abstract class BaseTaskRunStep extends BaseStep {
    protected String url;
    protected transient MixedOperation<TaskRun, TaskRunList, DoneableTaskRun, Resource<TaskRun, DoneableTaskRun>> taskRunClient;
    protected String TEKTON_RESOURCE_TYPE = "TaskRun";


    protected BaseTaskRunStep() {
        taskRunClient = tektonClient.v1beta1().taskRuns();
    }

    public String getUrl() {
        return this.url;
    }
}
