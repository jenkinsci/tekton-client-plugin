package org.waveywaves.jenkins.plugins.tekton.client.build.delete;

import org.junit.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.CreateStep;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.mock.CreateStepMock;
import org.waveywaves.jenkins.plugins.tekton.client.build.delete.mock.DeleteStepMock;

public class DeleteStepTest {

    @Test
    public void runDeleteTaskTest(){
        DeleteStep deleteStep = new DeleteStepMock("test", TektonUtils.TektonResourceType.task.toString());
        Boolean isDeleted = deleteStep.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeleteTaskRunTest(){
        DeleteStep deleteStep = new DeleteStepMock("test", TektonUtils.TektonResourceType.taskrun.toString());
        Boolean isDeleted = deleteStep.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeletePipelineTest(){
        DeleteStep deleteStep = new DeleteStepMock("test", TektonUtils.TektonResourceType.pipeline.toString());
        Boolean isDeleted = deleteStep.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeletePipelineRunTest(){
        DeleteStep deleteStep = new DeleteStepMock("test", TektonUtils.TektonResourceType.pipelinerun.toString());
        Boolean isDeleted = deleteStep.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeletePipelineResourceTest(){
        DeleteStep deleteStep = new DeleteStepMock("test", TektonUtils.TektonResourceType.pipelineresource.toString());
        Boolean isDeleted = deleteStep.runDelete();
        assert isDeleted.equals(true);
    }
}
