package org.waveywaves.jenkins.plugins.tekton.client.build.create.mock;

import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.CreateRaw;

import java.io.InputStream;

public class CreateRawMock extends CreateRaw {
    public CreateRawMock(String input, String inputType, boolean enableCatalog) {
        super(input, inputType, enableCatalog);
    }

    @Override
    public String createTaskRun(InputStream inputStream) {
        return TektonUtils.TektonResourceType.taskrun.toString();
    }

    @Override
    public String createTask(InputStream inputStream) {
        return TektonUtils.TektonResourceType.task.toString();
    }

    @Override
    public String createPipeline(InputStream inputStream) {
        return TektonUtils.TektonResourceType.pipeline.toString();
    }

    @Override
    public String createPipelineRun(InputStream inputStream) { return TektonUtils.TektonResourceType.pipelinerun.toString(); }

    @Override
    public void streamTaskRunLogsToConsole(TaskRun taskRun) {
        return;
    }

    @Override
    public void streamPipelineRunLogsToConsole(PipelineRun pipelineRun) {
        return;
    }
}
