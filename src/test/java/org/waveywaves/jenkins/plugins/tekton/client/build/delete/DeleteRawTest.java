package org.waveywaves.jenkins.plugins.tekton.client.build.delete;

import org.junit.jupiter.api.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.delete.mock.DeleteRawMock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class DeleteRawTest {

    DeleteRaw.DeleteAllBlock deleteAllBlock;

    @Test
    void runDeleteTaskTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.task.toString(), TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
    }

    @Test
    void runDeleteTaskRunTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.taskrun.toString(),TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
    }

    @Test
    void runDeletePipelineTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.pipeline.toString(),TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
    }

    @Test
    void runDeletePipelineRunTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.pipelinerun.toString(),TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
    }
}
