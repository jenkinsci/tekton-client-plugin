package org.waveywaves.jenkins.plugins.tekton.client.build.delete;

import org.junit.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.delete.mock.DeleteStepMock;

public class DeleteStepTest {

    DeleteStep.DeleteAllBlock deleteAllBlock;

    @Test
    public void runDeleteTaskTest(){
        deleteAllBlock = new DeleteStep.DeleteAllBlock("test");
        DeleteStep deleteStep = new DeleteStepMock(TektonUtils.TektonResourceType.task.toString(), deleteAllBlock);
        Boolean isDeleted = deleteStep.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeleteTaskRunTest(){
        deleteAllBlock = new DeleteStep.DeleteAllBlock("test");
        DeleteStep deleteStep = new DeleteStepMock(TektonUtils.TektonResourceType.taskrun.toString(), deleteAllBlock);
        Boolean isDeleted = deleteStep.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeletePipelineTest(){
        deleteAllBlock = new DeleteStep.DeleteAllBlock("test");
        DeleteStep deleteStep = new DeleteStepMock(TektonUtils.TektonResourceType.pipeline.toString(), deleteAllBlock);
        Boolean isDeleted = deleteStep.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeletePipelineRunTest(){
        deleteAllBlock = new DeleteStep.DeleteAllBlock("test");
        DeleteStep deleteStep = new DeleteStepMock(TektonUtils.TektonResourceType.pipelinerun.toString(), deleteAllBlock);
        Boolean isDeleted = deleteStep.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeletePipelineResourceTest(){
        DeleteStep deleteStep = new DeleteStepMock(TektonUtils.TektonResourceType.pipelineresource.toString(), deleteAllBlock);
        Boolean isDeleted = deleteStep.runDelete();
        assert isDeleted.equals(true);
    }
}
