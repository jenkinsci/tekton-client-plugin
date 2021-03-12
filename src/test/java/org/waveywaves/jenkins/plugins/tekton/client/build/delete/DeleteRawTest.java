package org.waveywaves.jenkins.plugins.tekton.client.build.delete;

import org.junit.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.delete.mock.DeleteRawMock;

public class DeleteRawTest {

    DeleteRaw.DeleteAllBlock deleteAllBlock;

    @Test
    public void runDeleteTaskTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.task.toString(), deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeleteTaskRunTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.taskrun.toString(), deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeletePipelineTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.pipeline.toString(), deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assert isDeleted.equals(true);
    }

    @Test
    public void runDeletePipelineRunTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.pipelinerun.toString(), deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assert isDeleted.equals(true);
    }
}
