package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBinding;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CreateCustomTaskrunTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getKind() {
    }

    @Test
    public void getName() {
    }
    @Test
    public void getNamespace() {
    }

    @Test
    public void getTaskRef() {
    }

    @Test
    public void getGenerateName() {
    }

    @Test
    public void getWorkspaces() {
    }

    @Test
    public void getParams() {
    }

    @Test
    public void perform() {
    }

    @Test
    public void workspacesToWorkspaceBindingList() {
        // Test with no workspaces provided (should use default workspace)
        CreateCustomTaskrun customTaskrunDefaultWorkspace = new CreateCustomTaskrun("testTask", "testGenerateName", "namespace", "clusterName", null, null, "testTaskRef");
        List<WorkspaceBinding> workspaceBindingListDefaultWorkspace = customTaskrunDefaultWorkspace.workspacesToWorkspaceBindingList();
        // Check for default workspace if there is no workspace provided
        assertEquals(1, workspaceBindingListDefaultWorkspace.size());
        assertEquals("default-workspace", workspaceBindingListDefaultWorkspace.get(0).getName());

        // Test with a list of workspaces provided
        List<TektonWorkspaceBind> wsbList = new ArrayList<>();
        wsbList.add(new TektonWorkspaceBind("workspace1", "claimWorkspace1"));
        wsbList.add(new TektonWorkspaceBind("workspace2", "claimWorkspace2"));
        CreateCustomTaskrun customTaskrunWithWorkspaces = new CreateCustomTaskrun("testTask", "testGenerateName", "namespace", "clusterName", wsbList, null, "testTaskRef");
        List<WorkspaceBinding> workspaceBindingListWithWorkspaces = customTaskrunWithWorkspaces.workspacesToWorkspaceBindingList();
        assertEquals(2, workspaceBindingListWithWorkspaces.size());
        assertEquals("workspace1", workspaceBindingListWithWorkspaces.get(0).getName());
        assertEquals("workspace2", workspaceBindingListWithWorkspaces.get(1).getName());
    }
}
