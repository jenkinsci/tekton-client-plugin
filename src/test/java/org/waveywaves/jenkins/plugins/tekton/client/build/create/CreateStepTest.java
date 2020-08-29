package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

import io.fabric8.tekton.pipeline.v1beta1.*;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class CreateStepTest {
    @Rule
    public KubernetesServer server = new KubernetesServer();

    @Test
    public void testTaskCreate() {
        // Given
        String testTaskYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Task\n" +
                "metadata:\n" +
                "  name: testTask\n";

        KubernetesClient client = server.getClient();
        InputStream crdAsInputStream = getClass().getResourceAsStream("/task-crd.yaml");
        CustomResourceDefinition taskCrd = client.customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<Task, TaskList, DoneableTask, Resource<Task, DoneableTask>> taskClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(taskCrd), Task.class, TaskList.class, DoneableTask.class);

        // Mocked Responses
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata().withName("testTask").endMetadata();
        List<Task> tList = new ArrayList<Task>();
        Task testTask = taskBuilder.build();
        tList.add(testTask);
        TaskList taskList = new TaskList();
        taskList.setItems(tList);

        server.expect().post().withPath("/apis/tekton.dev/v1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_CREATED, testTask).once();
        server.expect().get().withPath("/apis/tekton.dev/v1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_OK, taskList).once();

        // When
        CreateStep createStep = new CreateStep(CreateStep.InputType.YAML.toString(), testTaskYaml);
        createStep.setTektonClient(client);
        createStep.setTaskClient(taskClient);
        String createdTaskName = createStep.createTask(
                new ByteArrayInputStream(testTaskYaml.getBytes(StandardCharsets.UTF_8)));

        // Then
        TaskList testTaskList = taskClient.list();
        assert createdTaskName.equals("testTask");
        assert testTaskList.getItems().size() == 1;
    }

    @Test
    public void testTaskRunCreate() {
        // Given
        String testTaskRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: TaskRun\n" +
                "metadata:\n" +
                "  generateName: home-is-set-\n";

        KubernetesClient client = server.getClient();
        InputStream crdAsInputStream = getClass().getResourceAsStream("/taskrun-crd.yaml");
        CustomResourceDefinition taskRunCrd = client.customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<TaskRun, TaskRunList, DoneableTaskRun, Resource<TaskRun, DoneableTaskRun>> taskRunClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(taskRunCrd), TaskRun.class, TaskRunList.class, DoneableTaskRun.class);

        // Mocked Responses
        TaskRunBuilder taskRunBuilder = new TaskRunBuilder()
                .withNewMetadata().withName("home-is-set-1234").endMetadata();
        List<TaskRun> trList = new ArrayList<TaskRun>();
        TaskRun testTaskRun = taskRunBuilder.build();
        trList.add(testTaskRun);
        TaskRunList taskRunList = new TaskRunList();
        taskRunList.setItems(trList);

        server.expect().post().withPath("/apis/tekton.dev/v1/namespaces/test/taskruns")
                .andReturn(HttpURLConnection.HTTP_CREATED, testTaskRun).once();
        server.expect().get().withPath("/apis/tekton.dev/v1/namespaces/test/taskruns")
                .andReturn(HttpURLConnection.HTTP_OK, taskRunList).once();

        // When
        CreateStep createStep = new CreateStep(CreateStep.InputType.YAML.toString(), testTaskRunYaml);
        createStep.setTektonClient(client);
        createStep.setTaskRunClient(taskRunClient);
        String createdTaskRunName = createStep.createTaskRun(
                new ByteArrayInputStream(testTaskRunYaml.getBytes(StandardCharsets.UTF_8)));

        // Then
        TaskRunList testTaskRunList = taskRunClient.list();
        assert createdTaskRunName.equals("home-is-set-1234");
        assert testTaskRunList.getItems().size() == 1;
    }

    @Test
    public void testPipelineCreate() {
        // Given
        String testPipelineYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Pipeline\n" +
                "metadata:\n" +
                "  name: testPipeline\n";

        KubernetesClient client = server.getClient();
        InputStream crdAsInputStream = getClass().getResourceAsStream("/pipeline-crd.yaml");
        CustomResourceDefinition pipelineCrd = client.customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<Pipeline, PipelineList, DoneablePipeline, Resource<Pipeline, DoneablePipeline>> pipelineClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(pipelineCrd), Pipeline.class, PipelineList.class, DoneablePipeline.class);

        // Mocked Responses
        PipelineBuilder pipelineBuilder = new PipelineBuilder()
                .withNewMetadata().withName("testPipeline").endMetadata();
        List<Pipeline> pList = new ArrayList<Pipeline>();
        Pipeline testPipeline = pipelineBuilder.build();
        pList.add(testPipeline);
        PipelineList pipelineList = new PipelineList();
        pipelineList.setItems(pList);

        server.expect().post().withPath("/apis/tekton.dev/v1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_CREATED, testPipeline).once();
        server.expect().get().withPath("/apis/tekton.dev/v1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_OK, pipelineList).once();

        // When
        CreateStep createStep = new CreateStep(CreateStep.InputType.YAML.toString(), testPipelineYaml);
        createStep.setTektonClient(client);
        createStep.setPipelineClient(pipelineClient);
        String createdPipelineName = createStep.createPipeline(
                new ByteArrayInputStream(testPipelineYaml.getBytes(StandardCharsets.UTF_8)));

        // Then
        PipelineList testPipelineList = pipelineClient.list();
        assert createdPipelineName.equals("testPipeline");
        assert testPipelineList.getItems().size() == 1;
    }

    @Test
    public void testPipelineRunCreate() {
        // Given
        String testPipelineRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: PipelineRun\n" +
                "metadata:\n" +
                "  name: testPipelineRun\n";

        KubernetesClient client = server.getClient();
        InputStream crdAsInputStream = getClass().getResourceAsStream("/pipeline-crd.yaml");
        CustomResourceDefinition pipelineRunCrd = client.customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<PipelineRun, PipelineRunList, DoneablePipelineRun, Resource<PipelineRun, DoneablePipelineRun>> pipelineRunClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(pipelineRunCrd), PipelineRun.class, PipelineRunList.class, DoneablePipelineRun.class);

        // Mocked Responses
        PipelineRunBuilder pipelineRunBuilder = new PipelineRunBuilder()
                .withNewMetadata().withName("testPipelineRun").endMetadata();
        List<PipelineRun> prList = new ArrayList<PipelineRun>();
        PipelineRun testPipelineRun = pipelineRunBuilder.build();
        prList.add(testPipelineRun);
        PipelineRunList pipelineRunList = new PipelineRunList();
        pipelineRunList.setItems(prList);

        server.expect().post().withPath("/apis/tekton.dev/v1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_CREATED, testPipelineRun).once();
        server.expect().get().withPath("/apis/tekton.dev/v1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunList).once();

        // When
        CreateStep createStep = new CreateStep(CreateStep.InputType.YAML.toString(), testPipelineRunYaml);
        createStep.setTektonClient(client);
        createStep.setPipelineRunClient(pipelineRunClient);
        String createdPipelineName = createStep.createPipelineRun(
                new ByteArrayInputStream(testPipelineRunYaml.getBytes(StandardCharsets.UTF_8)));

        // Then
        PipelineRunList testPipelineRunList = pipelineRunClient.list();
        assert createdPipelineName.equals("testPipelineRun");
        assert testPipelineRunList.getItems().size() == 1;
    }
}