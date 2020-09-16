package org.waveywaves.jenkins.plugins.tekton.client.build.delete.mock;

import org.waveywaves.jenkins.plugins.tekton.client.build.delete.DeleteStep;


public class DeleteStepMock extends DeleteStep {
    public DeleteStepMock(String resourceType, DeleteAllBlock deleteAllBlock) {
        super(resourceType, deleteAllBlock);
    }

    @Override
    public Boolean deleteTask() {
        return true;
    }

    @Override
    public Boolean deleteTaskRun() {
        return true;
    }

    @Override
    public Boolean deletePipeline() {
        return true;
    }

    @Override
    public Boolean deletePipelineRun() {
        return true;
    }

    @Override
    public Boolean deletePipelineResource() {
        return true;
    }
}
