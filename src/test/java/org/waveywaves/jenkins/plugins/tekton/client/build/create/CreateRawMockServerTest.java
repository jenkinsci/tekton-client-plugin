package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.EnvVars;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

import io.fabric8.tekton.pipeline.v1beta1.*;
import org.junit.Rule;
import org.junit.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.waveywaves.jenkins.plugins.tekton.client.build.FakeChecksPublisher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.assertj.core.api.Assertions.fail;


public class CreateRawMockServerTest {

    private boolean enableCatalog = false;
    private String namespace;

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
        CustomResourceDefinition taskCrd = client.apiextensions().v1beta1().customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<Task, TaskList, Resource<Task>> taskClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(taskCrd), Task.class, TaskList.class);

        // Mocked Responses
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata()
                    .withName("testTask")
                .endMetadata();
        TaskList taskList = new TaskListBuilder()
                .addToItems(taskBuilder.build())
                .build();

        server.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_CREATED, taskBuilder.build()).once();
        server.expect().get().withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_OK, taskList).once();

        // When
        CreateRaw createRaw = new CreateRaw(CreateRaw.InputType.YAML.toString(), testTaskYaml);
        createRaw.setNamespace(namespace);
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setEnableCatalog(enableCatalog);

        createRaw.setTektonClient(client);
        createRaw.setTaskClient(taskClient);
        String createdTaskName = createRaw.createTask(
                new ByteArrayInputStream(testTaskYaml.getBytes(StandardCharsets.UTF_8)));

        // Then
        TaskList testTaskList = taskClient.list();
        assertThat(createdTaskName, is("testTask"));
        assertThat(testTaskList.getItems().size(), is(1));
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
        CustomResourceDefinition taskRunCrd = client.apiextensions().v1beta1().customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<TaskRun, TaskRunList, Resource<TaskRun>> taskRunClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(taskRunCrd), TaskRun.class, TaskRunList.class);

        // Mocked Responses
        TaskRunBuilder taskRunBuilder = new TaskRunBuilder()
                .withNewMetadata()
                    .withName("home-is-set-1234")
                .endMetadata();
        TaskRunList taskRunList = new TaskRunListBuilder()
                .addToItems(taskRunBuilder.build())
                .build();

        server.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/taskruns")
                .andReturn(HttpURLConnection.HTTP_CREATED, taskRunBuilder.build()).once();
        server.expect().get().withPath("/apis/tekton.dev/v1beta1/namespaces/test/taskruns")
                .andReturn(HttpURLConnection.HTTP_OK, taskRunList).once();

        // When
        CreateRaw createRaw = new CreateRaw(testTaskRunYaml, CreateRaw.InputType.YAML.toString()) {
            @Override
            public void streamTaskRunLogsToConsole(TaskRun taskRun) {
                return;
            }
        };
        createRaw.setNamespace(namespace);
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setEnableCatalog(enableCatalog);
        createRaw.setTektonClient(client);
        createRaw.setTaskRunClient(taskRunClient);

        String createdTaskRunName = "";

        try {
            createdTaskRunName = createRaw.createTaskRun(
                    new ByteArrayInputStream(testTaskRunYaml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        // Then
        TaskRunList testTaskRunList = taskRunClient.list();
        assertThat(createdTaskRunName, is("home-is-set-1234"));
        assertThat(testTaskRunList.getItems().size(), is(1));
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
        CustomResourceDefinition pipelineCrd = client.apiextensions().v1beta1().customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<Pipeline, PipelineList, Resource<Pipeline>> pipelineClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(pipelineCrd), Pipeline.class, PipelineList.class);

        // Mocked Responses
        PipelineBuilder pipelineBuilder = new PipelineBuilder()
                .withNewMetadata()
                    .withName("testPipeline")
                .endMetadata();
        PipelineList pipelineList = new PipelineListBuilder()
                .addToItems(pipelineBuilder.build())
                .build();

        server.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_CREATED, pipelineBuilder.build()).once();
        server.expect().get().withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_OK, pipelineList).once();

        // When
        CreateRaw createRaw = new CreateRaw(testPipelineYaml, CreateRaw.InputType.YAML.toString());
        createRaw.setNamespace(namespace);
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setEnableCatalog(enableCatalog);
        createRaw.setTektonClient(client);
        createRaw.setPipelineClient(pipelineClient);
        String createdPipelineName = createRaw.createPipeline(
                new ByteArrayInputStream(testPipelineYaml.getBytes(StandardCharsets.UTF_8)));

        // Then
        PipelineList testPipelineList = pipelineClient.list();
        assertThat(createdPipelineName, is("testPipeline"));
        assertThat(testPipelineList.getItems().size(), is(1));
    }

    @Test
    public void testPipelineRunCreate() {
        // Given
        String testPipelineRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: PipelineRun\n" +
                "metadata:\n" +
                "  name: testPipelineRun\n" +
                "spec:\n" +
                "  params: []\n";

        KubernetesClient client = server.getClient();
        InputStream crdAsInputStream = getClass().getResourceAsStream("/pipeline-crd.yaml");
        CustomResourceDefinition pipelineRunCrd = client.apiextensions().v1beta1().customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<PipelineRun, PipelineRunList, Resource<PipelineRun>> pipelineRunClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(pipelineRunCrd), PipelineRun.class, PipelineRunList.class);

        // Mocked Responses
        PipelineRunBuilder pipelineRunBuilder = new PipelineRunBuilder()
                .withNewMetadata()
                    .withName("testPipelineRun")
                .endMetadata()
                .withNewSpec()
                    .withParams(new Param("param", new ArrayOrString("value")))
                .endSpec();

        PipelineRunList pipelineRunList = new PipelineRunListBuilder()
                .addToItems(pipelineRunBuilder.build())
                .build();

        server.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_CREATED, pipelineRunBuilder.build()).once();
        server.expect().get().withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelines")
                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunList).once();

        // When
        CreateRaw createRaw = new CreateRaw(testPipelineRunYaml, CreateRaw.InputType.YAML.toString()) {
            @Override
            public void streamPipelineRunLogsToConsole(PipelineRun pipelineRun) {
                return;
            }
        };
        createRaw.setNamespace(namespace);
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setEnableCatalog(enableCatalog);
        createRaw.setTektonClient(client);
        createRaw.setPipelineRunClient(pipelineRunClient);

        FakeChecksPublisher checksPublisher = new FakeChecksPublisher();
        createRaw.setChecksPublisher(checksPublisher);

        String createdPipelineName = "";
        try {
            createdPipelineName = createRaw.createPipelineRun(
                    new ByteArrayInputStream(testPipelineRunYaml.getBytes(StandardCharsets.UTF_8)), new EnvVars());
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        assertThat(checksPublisher.getCounter(), is(1));

        // Then
        PipelineRunList testPipelineRunList = pipelineRunClient.list();
        assertThat(createdPipelineName, is("testPipelineRun"));
        assertThat(testPipelineRunList.getItems().size(), is(1));
    }
}