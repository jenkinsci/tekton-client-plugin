package org.waveywaves.jenkins.plugins.tekton.client.build.delete.mock;

import org.waveywaves.jenkins.plugins.tekton.client.build.delete.DeleteRaw;


public class DeleteRawMock extends DeleteRaw {
    public DeleteRawMock(String resourceType, String clusterName, DeleteAllBlock deleteAllBlock) {
        super(resourceType, clusterName, deleteAllBlock);
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
}
