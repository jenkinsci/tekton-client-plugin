package org.waveywaves.jenkins.plugins.tekton.client.build.delete;

import org.junit.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.delete.mock.DeleteRawMock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DeleteRawTest {

    DeleteRaw.DeleteAllBlock deleteAllBlock;

    @Test
    public void runDeleteTaskTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.task.toString(), TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
    }

    @Test
    public void runDeleteTaskRunTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.taskrun.toString(),TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
    }

    @Test
    public void runDeletePipelineTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.pipeline.toString(),TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
    }

    @Test
    public void runDeletePipelineRunTest(){
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.pipelinerun.toString(),TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
    }
    @Test
    public void runDeleteTaskWithNamespaceTest() {
        String TEST_NAMESPACE = "test-ns"; // Explicit namespace
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.task.toString(), TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        deleteRaw.setNameSpace(TEST_NAMESPACE); // Set namespace
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
        assertThat(deleteRaw.getNameSpace(), is(TEST_NAMESPACE)); // Verify namespace was set
    }

    @Test
    public void runDeleteTaskRunWithNamespaceTest() {
        String TEST_NAMESPACE = "test-ns";
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.taskrun.toString(), TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        deleteRaw.setNameSpace(TEST_NAMESPACE);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
        assertThat(deleteRaw.getNameSpace(), is(TEST_NAMESPACE));
    }

    @Test
    public void runDeletePipelineWithNamespaceTest() {
        String TEST_NAMESPACE = "test-ns";
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.pipeline.toString(), TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        deleteRaw.setNameSpace(TEST_NAMESPACE);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
        assertThat(deleteRaw.getNameSpace(), is(TEST_NAMESPACE));
    }

    @Test
    public void runDeletePipelineRunWithNamespaceTest() {
        String TEST_NAMESPACE = "test-ns";
        deleteAllBlock = new DeleteRaw.DeleteAllBlock("test");
        DeleteRaw deleteRaw = new DeleteRawMock(TektonUtils.TektonResourceType.pipelinerun.toString(), TektonUtils.DEFAULT_CLIENT_KEY, deleteAllBlock);
        deleteRaw.setNameSpace(TEST_NAMESPACE);
        Boolean isDeleted = deleteRaw.runDelete();
        assertThat(isDeleted, is(true));
        assertThat(deleteRaw.getNameSpace(), is(TEST_NAMESPACE));
    }
}
