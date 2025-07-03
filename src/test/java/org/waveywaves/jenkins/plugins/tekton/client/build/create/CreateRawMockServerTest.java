package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import java.util.Collections;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.FakeChecksPublisher;

import hudson.EnvVars;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.CoreMatchers.is;

@EnableKubernetesMockClient(crud = false)
public class CreateRawMockServerTest {

        KubernetesClient kubernetesClient;
        KubernetesMockServer server;

        boolean enableCatalog = false;
        private final String namespace = "test";
        private FakeChecksPublisher checksPublisher;

        @BeforeEach
        void before() {
                TektonUtils.initializeKubeClients(kubernetesClient.getConfiguration());
                checksPublisher = new FakeChecksPublisher();
        }

        @AfterEach
        void after() {
                checksPublisher.validate();
        }

        @Test
        void testTaskCreate() {
                String testTaskYaml = "apiVersion: tekton.dev/v1beta1\n" +
                                "kind: Task\n" +
                                "metadata:\n" +
                                "  name: testTask\n" +
                                "  namespace: test\n";

                Task expectedTask = new TaskBuilder()
                                .withNewMetadata()
                                .withName("testTask")
                                .withNamespace(namespace)
                                .endMetadata()
                                .build();

                TaskList expectedList = new TaskListBuilder()
                                .addToItems(expectedTask)
                                .build();

                server.expect()
                                .post()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                                .andReturn(HTTP_CREATED, expectedTask)
                                .once();

                server.expect()
                                .get()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                                .andReturn(HTTP_OK, expectedList)
                                .once();

                CreateRaw createRaw = new CreateRaw(CreateRaw.InputType.YAML.toString(), testTaskYaml);
                createRaw.setNamespace(namespace);
                createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
                createRaw.setEnableCatalog(false);
                createRaw.setTektonClient(TektonUtils.getTektonClient(TektonUtils.DEFAULT_CLIENT_KEY));

                String createdName = createRaw.createTask(
                                new ByteArrayInputStream(testTaskYaml.getBytes(StandardCharsets.UTF_8)));

                assertThat(createdName, is("testTask"));
        }

        @Test
        void testTaskRunCreate() throws Exception {
                String testTaskRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                                "kind: TaskRun\n" +
                                "metadata:\n" +
                                "  generateName: home-is-set-\n" +
                                "  namespace: test\n";

                TaskRun createdTaskRun = new TaskRunBuilder()
                                .withNewMetadata()
                                .withName("home-is-set-1234")
                                .withNamespace(namespace)
                                .endMetadata()
                                .build();

                server.expect()
                                .post()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/taskruns")
                                .andReturn(HTTP_CREATED, createdTaskRun)
                                .once();

                TaskRunList taskRunList = new TaskRunListBuilder()
                                .addToItems(createdTaskRun)
                                .build();

                server.expect()
                                .get()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/taskruns")
                                .andReturn(HTTP_OK, taskRunList)
                                .once();
                CreateRaw createRaw = new CreateRaw(CreateRaw.InputType.YAML.toString(), testTaskRunYaml) {
                        @Override
                        public void streamTaskRunLogsToConsole(TaskRun taskRun) {
                        }
                };

                createRaw.setNamespace(namespace);
                createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
                createRaw.setEnableCatalog(false);
                createRaw.setTektonClient(TektonUtils.getTektonClient(TektonUtils.DEFAULT_CLIENT_KEY));

                String createdName = createRaw.createTaskRun(
                                new ByteArrayInputStream(testTaskRunYaml.getBytes(StandardCharsets.UTF_8)));

                assertThat(createdName, is("home-is-set-1234"));
        }

        @Test
        void testPipelineCreate() throws Exception {
                String testPipelineYaml = "apiVersion: tekton.dev/v1beta1\n" +
                                "kind: Pipeline\n" +
                                "metadata:\n" +
                                "  name: my-pipeline\n" +
                                "  namespace: test\n";

                Pipeline expectedPipeline = new PipelineBuilder()
                                .withNewMetadata()
                                .withName("my-pipeline")
                                .withNamespace(namespace)
                                .endMetadata()
                                .build();

                PipelineList expectedPipelineList = new PipelineListBuilder()
                                .addToItems(expectedPipeline)
                                .build();

                server.expect()
                                .post()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelines")
                                .andReturn(HTTP_CREATED, expectedPipeline)
                                .once();

                server.expect()
                                .get()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelines")
                                .andReturn(HTTP_OK, expectedPipelineList)
                                .once();

                CreateRaw createRaw = new CreateRaw(CreateRaw.InputType.YAML.toString(), testPipelineYaml);
                createRaw.setNamespace(namespace);
                createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
                createRaw.setEnableCatalog(false);
                createRaw.setTektonClient(TektonUtils.getTektonClient(TektonUtils.DEFAULT_CLIENT_KEY));

                String createdName = createRaw.createPipeline(
                                new ByteArrayInputStream(testPipelineYaml.getBytes(StandardCharsets.UTF_8)));

                assertThat(createdName, is("my-pipeline"));
        }

        @Test
        void testPipelineRunCreate() throws Exception {
                String testPipelineRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                                "kind: PipelineRun\n" +
                                "metadata:\n" +
                                "  name: testPipelineRun\n" +
                                "  namespace: test\n" +
                                "spec:\n" +
                                "  params:\n" +
                                "    - name: param\n" +
                                "      value: value\n";

                // Build expected PipelineRun
                PipelineRun mockedPipelineRun = new PipelineRunBuilder()
                                .withNewMetadata()
                                .withName("testPipelineRun")
                                .withNamespace(namespace)
                                .endMetadata()
                                .withNewSpec()
                                .addNewParam()
                                .withName("param")
                                .withValue(new ArrayOrString("value"))
                                .endParam()
                                .endSpec()
                                .withStatus(new PipelineRunStatusBuilder()
                                                .withConditions(Collections.singletonList(
                                                                new Condition(
                                                                                null,
                                                                                "PipelineRun succeeded",
                                                                                "Completed",
                                                                                null,
                                                                                "True",
                                                                                "Succeeded")))
                                                .build())
                                .build();

                PipelineRunList runList = new PipelineRunListBuilder()
                                .addToItems(mockedPipelineRun)
                                .build();

                server.expect()
                                .post()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns")
                                .andReturn(HTTP_CREATED, mockedPipelineRun)
                                .once();

                server.expect()
                                .get()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns")
                                .andReturn(HTTP_OK, runList)
                                .once();

                server.expect()
                                .get()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns/testPipelineRun")
                                .andReturn(HTTP_OK, mockedPipelineRun)
                                .once();

                // CreateRaw object setup
                CreateRaw createRaw = new CreateRaw(testPipelineRunYaml, CreateRaw.InputType.YAML.toString()) {
                        @Override
                        public void streamPipelineRunLogsToConsole(PipelineRun pipelineRun) {
                                // Skip streaming
                        }
                };

                createRaw.setNamespace(namespace);
                createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
                createRaw.setEnableCatalog(enableCatalog);
                createRaw.setTektonClient(TektonUtils.getTektonClient(TektonUtils.DEFAULT_CLIENT_KEY));
                createRaw.setPipelineRunClient(
                                TektonUtils.getTektonClient(TektonUtils.DEFAULT_CLIENT_KEY)
                                                .v1beta1()
                                                .pipelineRuns());
                createRaw.setChecksPublisher(checksPublisher);

                // When
                String createdName = createRaw.createPipelineRun(
                                new ByteArrayInputStream(testPipelineRunYaml.getBytes(StandardCharsets.UTF_8)),
                                new EnvVars());

                // Then
                assertThat(createdName, is("testPipelineRun"));
                assertThat(checksPublisher.getCounter(), is(1));

                TektonClient tektonClient = TektonUtils.getTektonClient(TektonUtils.DEFAULT_CLIENT_KEY);
                PipelineRunList resultList = tektonClient
                                .v1beta1()
                                .pipelineRuns()
                                .inNamespace(namespace)
                                .list();

                assertThat(resultList.getItems().size(), is(1));
        }

        @Test
        void testPipelineRunCreateWithFailingPod() throws Exception {
                // Given
                String testPipelineRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                                "kind: PipelineRun\n" +
                                "metadata:\n" +
                                "  name: testPipelineRun\n" +
                                "  namespace: test\n" +
                                "spec:\n" +
                                "  params:\n" +
                                "    - name: param\n" +
                                "      value: value\n";

                PipelineRun mockedPipelineRun = new PipelineRunBuilder()
                                .withNewMetadata()
                                .withName("testPipelineRun")
                                .withNamespace(namespace)
                                .endMetadata()
                                .withNewSpec()
                                .addNewParam()
                                .withName("param")
                                .withValue(new ArrayOrString("value"))
                                .endParam()
                                .endSpec()
                                .withStatus(new PipelineRunStatusBuilder()
                                                .withConditions(Collections.singletonList(
                                                                new Condition(
                                                                                null, 
                                                                                "This is an error message", 
                                                                                "ParameterMissing", 
                                                                                null, 
                                                                                "False", 
                                                                                "Succeeded"
                                                                )))
                                                .build())
                                .build();

                PipelineRunList runList = new PipelineRunListBuilder()
                                .addToItems(mockedPipelineRun)
                                .build();

                // Mock server expectations
                server.expect()
                                .post()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns")
                                .andReturn(HTTP_CREATED, mockedPipelineRun)
                                .once();

                server.expect()
                                .get()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns")
                                .andReturn(HTTP_OK, runList)
                                .once();

                server.expect()
                                .get()
                                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns/testPipelineRun")
                                .andReturn(HTTP_OK, mockedPipelineRun)
                                .once();

                // When
                CreateRaw createRaw = new CreateRaw(testPipelineRunYaml, CreateRaw.InputType.YAML.toString()) {
                        @Override
                        public void streamPipelineRunLogsToConsole(PipelineRun pipelineRun) {
                                // Skip streaming
                        }
                };

                createRaw.setNamespace(namespace);
                createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
                createRaw.setEnableCatalog(enableCatalog);
                createRaw.setTektonClient(TektonUtils.getTektonClient(TektonUtils.DEFAULT_CLIENT_KEY));
                createRaw.setPipelineRunClient(
                                TektonUtils.getTektonClient(TektonUtils.DEFAULT_CLIENT_KEY)
                                                .v1beta1()
                                                .pipelineRuns());
                createRaw.setChecksPublisher(checksPublisher);

                // Then
                Exception thrown = assertThrows(Exception.class, () -> createRaw.createPipelineRun(
                                new ByteArrayInputStream(testPipelineRunYaml.getBytes(StandardCharsets.UTF_8)),
                                new EnvVars()));
                assertThat(thrown.getMessage(), is("ParameterMissing: This is an error message"));

                assertThat(checksPublisher.getCounter(), is(1));
        }

}
