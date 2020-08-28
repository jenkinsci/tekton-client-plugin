package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunList;
import io.fabric8.tekton.pipeline.v1beta1.DoneableTaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class CreateStepTest {
    @Rule
    public KubernetesServer server = new KubernetesServer();

    @Test
    public void testTaskRunCreate() {
        // Given
        String testTaskRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: TaskRun\n" +
                "metadata:\n" +
                "  generateName: home-is-set-\n" +
                "spec:\n" +
                "  taskSpec:\n" +
                "    steps:\n" +
                "    - image: ubuntu\n" +
                "      script: |\n" +
                "        #!/usr/bin/env bash\n" +
                "        [[ $HOME == /tekton/home ]]";

        KubernetesClient client = server.getClient();
        InputStream crdAsInputStream = getClass().getResourceAsStream("/taskrun-crd.yaml");
        CustomResourceDefinition taskRunCrd = client.customResourceDefinitions().load(crdAsInputStream).get();
        MixedOperation<TaskRun, TaskRunList, DoneableTaskRun, Resource<TaskRun, DoneableTaskRun>> taskRunClient = client
                .customResources(CustomResourceDefinitionContext.fromCrd(taskRunCrd), TaskRun.class, TaskRunList.class, DoneableTaskRun.class);

        TaskRunBuilder taskRunBuilder = new TaskRunBuilder()
                .withNewMetadata().withName("home-is-set-1234").endMetadata();
        List<TaskRun> trList = new ArrayList<TaskRun>();
        trList.add(taskRunBuilder.build());
        TaskRunList taskRunList = new TaskRunList();
        taskRunList.setItems(trList);

        server.expect().post().withPath("/apis/tekton.dev/v1/namespaces/test/taskruns")
                .andReturn(HttpURLConnection.HTTP_CREATED, taskRunBuilder.build()).once();
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
}