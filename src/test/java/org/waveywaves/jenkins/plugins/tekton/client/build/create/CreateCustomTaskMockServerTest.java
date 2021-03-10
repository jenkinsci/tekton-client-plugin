package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.tekton.pipeline.v1beta1.*;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CreateCustomTaskMockServerTest {
    @Rule
    public KubernetesServer server = new KubernetesServer();

    @Test
    public void testCreateCustomTaskTest() {
        // Given
        KubernetesClient client = server.getClient();
        InputStream crdAsInputStream = getClass().getResourceAsStream("/task-crd.yaml");
        CustomResourceDefinition taskCrd = client.apiextensions().v1beta1().customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<Task, TaskList, Resource<Task>> taskClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(taskCrd), Task.class, TaskList.class);

        String taskName = "taskName";
        String taskNamespace = "test";
        String taskDesc = "taskDesc";

        String paramName = "paramName";
        String paramDesc = "paramDesc";
        String paramDefault = "paramDefault";
        List<ParamSpec> paramSpecList = new ArrayList<>();
        ParamSpec paramSpec = new ParamSpec();
        paramSpec.setName(paramName);
        paramSpec.setDescription(paramDesc);
        paramSpec.setType("string");
        paramSpecList.add(paramSpec);

        String resultName = "resultName";
        String resultDesc = "resultDesc";
        List<TaskResult> resultList = new ArrayList<>();
        TaskResult result = new TaskResult();
        result.setName(resultName);
        result.setDescription(resultDesc);
        resultList.add(result);

        String workspaceName = "workspaceName";
        String workspaceDesc = "workspaceDesc";
        String workspaceMountPath = "workspaceMountPath";
        Boolean workspaceReadOnly = false;
        List<WorkspaceDeclaration> workspaceList = new ArrayList<>();
        WorkspaceDeclaration workspace = new WorkspaceDeclaration();
        workspace.setName(workspaceName);
        workspace.setDescription(workspaceDesc);
        workspace.setMountPath(workspaceMountPath);
        workspace.setReadOnly(workspaceReadOnly);

        List<Step> stepsList = new ArrayList<>();
        String stepName = "stepName";
        String stepImage = "bash:latest";
        Step step = new Step();
        step.setName(stepName);
        step.setImage(stepImage);
        stepsList.add(step);

        TaskSpec taskSpec = new TaskSpec();
        taskSpec.setResults(resultList);
        taskSpec.setParams(paramSpecList);
        taskSpec.setWorkspaces(workspaceList);
        taskSpec.setSteps(stepsList);

        // Mocked responses
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata()
                .withName(taskName)
                .endMetadata()
                .withSpec(taskSpec);
        List<Task> tList = new ArrayList<Task>();
        Task expectedTask = taskBuilder.build();
        tList.add(expectedTask);
        TaskList expectedTaskList = new TaskList();
        expectedTaskList.setItems(tList);

        server.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_CREATED, expectedTask).once();
        server.expect().get().withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_OK, expectedTaskList).once();

        // When
        List<TektonStringParamSpec> tektonTestParams = new ArrayList<>();
        tektonTestParams.add(new TektonStringParamSpec(paramName, paramDesc, paramDefault));

        List<TektonTaskResult> testResults = new ArrayList<>();
        testResults.add(new TektonTaskResult(resultName, resultDesc));

        List<TektonWorkspaceDecl> testWorkspaces = new ArrayList<>();
        testWorkspaces.add(new TektonWorkspaceDecl(workspaceName, workspaceDesc, workspaceMountPath, workspaceReadOnly));

        List<TektonStep> testSteps = new ArrayList<>();
        testSteps.add(new TektonStep(
                        stepName, stepImage, null,
                        null,
                        null,
                        null,
                        null,
                        null));

        CreateCustomTask testCustomTask = new CreateCustomTask(
                taskName,
                taskNamespace,
                taskDesc,
                tektonTestParams,
                testResults,
                testWorkspaces,
                testSteps);


        testCustomTask.setTektonClient(client);
        testCustomTask.setTaskClient(taskClient);

        // Then
        Task testTask = testCustomTask.runCreate();
        TaskList testTaskList = taskClient.list();
        assertEquals(testTask, expectedTask);
        assertEquals(1, testTaskList.getItems().size());
    }
}
