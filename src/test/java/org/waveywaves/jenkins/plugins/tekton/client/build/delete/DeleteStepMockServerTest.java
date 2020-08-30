package org.waveywaves.jenkins.plugins.tekton.client.build.delete;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.tekton.pipeline.v1beta1.*;
import org.junit.Rule;
import org.junit.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.CreateStep;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class DeleteStepMockServerTest {

    @Rule
    public KubernetesServer server = new KubernetesServer();

    @Test
    public void testTaskDelete() {
        // Given
        String testTaskYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Task\n" +
                "metadata:\n" +
                "  name: testTask\n";
        String TEST_TASK = "testTask";

        KubernetesClient client = server.getClient();
        InputStream crdAsInputStream = getClass().getResourceAsStream("/task-crd.yaml");
        CustomResourceDefinition taskCrd = client.customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<Task, TaskList, DoneableTask, Resource<Task, DoneableTask>> taskClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(taskCrd), Task.class, TaskList.class, DoneableTask.class);

        // Mocked Responses
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata().withName(TEST_TASK).endMetadata();
        List<Task> tList = new ArrayList<Task>();
        Task testTask = taskBuilder.build();
        tList.add(testTask);
        TaskList taskList = new TaskList();
        taskList.setItems(tList);

        server.expect().post().withPath("/apis/tekton.dev/v1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_CREATED, testTask).once();
        server.expect().get().withPath("/apis/tekton.dev/v1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_OK, taskList).once();
        server.expect().delete().withPath("/apis/tekton.dev/v1/namespaces/test/tasks/"+TEST_TASK)
                .andReturn(HttpURLConnection.HTTP_OK, testTask).once();
        server.expect().get().withPath("/apis/tekton.dev/v1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_OK, new TaskList()).once();


        // When
        CreateStep createStep = new CreateStep(CreateStep.InputType.YAML.toString(), testTaskYaml);
        createStep.setTektonClient(client);
        createStep.setTaskClient(taskClient);
        createStep.createTask(new ByteArrayInputStream(testTaskYaml.getBytes(StandardCharsets.UTF_8)));

        DeleteStep deleteStep = new DeleteStep(TEST_TASK, TektonUtils.TektonResourceType.task.toString());
        deleteStep.setTektonClient(client);
        deleteStep.setTaskClient(taskClient);
        Boolean isTaskDeleted = deleteStep.deleteTask();

        // Then
        TaskList testTaskList = taskClient.list();
        assert isTaskDeleted.equals(true);
        assert testTaskList.getItems().size() == 0;
    }

    @Test
    public void testTaskRunDelete() {
        // Given
        String testTaskRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: TaskRun\n" +
                "metadata:\n" +
                "  generateName: home-is-set-\n";
        String TEST_TASKRUN = "home-is-set-1234";

        KubernetesClient client = server.getClient();
        InputStream crdAsInputStream = getClass().getResourceAsStream("/taskrun-crd.yaml");
        CustomResourceDefinition taskRunCrd = client.customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<TaskRun, TaskRunList, DoneableTaskRun, Resource<TaskRun, DoneableTaskRun>> taskRunClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(taskRunCrd), TaskRun.class, TaskRunList.class, DoneableTaskRun.class);

        // Mocked Responses
        TaskRunBuilder taskRunBuilder = new TaskRunBuilder()
                .withNewMetadata().withName(TEST_TASKRUN).endMetadata();
        List<TaskRun> trList = new ArrayList<TaskRun>();
        TaskRun testTaskRun = taskRunBuilder.build();
        trList.add(testTaskRun);
        TaskRunList taskRunList = new TaskRunList();
        taskRunList.setItems(trList);

        server.expect().post().withPath("/apis/tekton.dev/v1/namespaces/test/taskruns")
                .andReturn(HttpURLConnection.HTTP_CREATED, testTaskRun).once();
        server.expect().get().withPath("/apis/tekton.dev/v1/namespaces/test/taskruns")
                .andReturn(HttpURLConnection.HTTP_OK, taskRunList).once();
        server.expect().delete().withPath("/apis/tekton.dev/v1/namespaces/test/taskruns/"+TEST_TASKRUN)
                .andReturn(HttpURLConnection.HTTP_OK, testTaskRun).once();
        server.expect().get().withPath("/apis/tekton.dev/v1/namespaces/test/taskruns")
                .andReturn(HttpURLConnection.HTTP_OK, new TaskList()).once();


        // When
        CreateStep createStep = new CreateStep(CreateStep.InputType.YAML.toString(), testTaskRunYaml);
        createStep.setTektonClient(client);
        createStep.setTaskRunClient(taskRunClient);
        createStep.createTaskRun(new ByteArrayInputStream(testTaskRunYaml.getBytes(StandardCharsets.UTF_8)));

        DeleteStep deleteStep = new DeleteStep(TEST_TASKRUN, TektonUtils.TektonResourceType.taskrun.toString());
        deleteStep.setTektonClient(client);
        deleteStep.setTaskRunClient(taskRunClient);
        Boolean isTaskRunDeleted = deleteStep.deleteTaskRun();

        // Then
        TaskRunList testTaskRunList = taskRunClient.list();
        assert isTaskRunDeleted.equals(true);
        assert testTaskRunList.getItems().size() == 0;
    }

    @Test
    public void testPipelineDelete() {
        // Given
        String testPipelineYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Pipeline\n" +
                "metadata:\n" +
                "  name: testPipeline\n";
        String TEST_PIPELINE = "testPipeline";

        KubernetesClient client = server.getClient();
        InputStream crdAsInputStream = getClass().getResourceAsStream("/pipeline-crd.yaml");
        CustomResourceDefinition pipelineCrd = client.customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<Pipeline, PipelineList, DoneablePipeline, Resource<Pipeline, DoneablePipeline>> pipelineClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(pipelineCrd), Pipeline.class, PipelineList.class, DoneablePipeline.class);

        // Mocked Responses
        PipelineBuilder pipelineBuilder = new PipelineBuilder()
                .withNewMetadata().withName(TEST_PIPELINE).endMetadata();
        List<Pipeline> pList = new ArrayList<Pipeline>();
        Pipeline testPipeline = pipelineBuilder.build();
        pList.add(testPipeline);
        PipelineList pipelineList = new PipelineList();
        pipelineList.setItems(pList);

        server.expect().post().withPath("/apis/tekton.dev/v1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_CREATED, testPipeline).once();
        server.expect().get().withPath("/apis/tekton.dev/v1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_OK, pipelineList).once();
        server.expect().delete().withPath("/apis/tekton.dev/v1/namespaces/test/pipelines/"+TEST_PIPELINE)
                .andReturn(HttpURLConnection.HTTP_OK, testPipeline).once();
        server.expect().get().withPath("/apis/tekton.dev/v1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_OK, new PipelineList()).once();


        // When
        CreateStep createStep = new CreateStep(CreateStep.InputType.YAML.toString(), testPipelineYaml);
        createStep.setTektonClient(client);
        createStep.setPipelineClient(pipelineClient);
        createStep.createPipeline(new ByteArrayInputStream(testPipelineYaml.getBytes(StandardCharsets.UTF_8)));

        DeleteStep deleteStep = new DeleteStep(TEST_PIPELINE, TektonUtils.TektonResourceType.pipeline.toString());
        deleteStep.setTektonClient(client);
        deleteStep.setPipelineClient(pipelineClient);
        Boolean isPipelineDeleted = deleteStep.deletePipeline();

        // Then
        PipelineList testPipelineList = pipelineClient.list();
        assert isPipelineDeleted.equals(true);
        assert testPipelineList.getItems().size() == 0;
    }
}