package org.waveywaves.jenkins.plugins.tekton.client.build.create.mock;

import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.CreateStep;

import java.io.InputStream;

public class CreateStepMock extends CreateStep {
    public CreateStepMock(String input, String inputType) {
        super(input, inputType);
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
    public String createPipelineRun(InputStream inputStream) {
        return TektonUtils.TektonResourceType.pipelinerun.toString();
    }

    @Override
    public String createPipelineResource(InputStream inputStream) {
        return TektonUtils.TektonResourceType.pipelineresource.toString();
    }
}
