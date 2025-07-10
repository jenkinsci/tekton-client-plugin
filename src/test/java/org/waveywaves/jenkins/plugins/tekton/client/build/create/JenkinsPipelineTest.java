package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.model.Result;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.tekton.pipeline.v1beta1.TaskBuilder;
import io.fabric8.tekton.client.TektonClient;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import io.fabric8.kubernetes.api.model.OwnerReference;

import java.net.URL;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
@WithJenkins
public class JenkinsPipelineTest {

    private KubernetesServer kubernetesServer;

    @BeforeEach
    void before() {
        kubernetesServer = new KubernetesServer(false, true);
        kubernetesServer.before();

        Config config = kubernetesServer.getClient().getConfiguration();
        config.setNamespace("test");

        TektonUtils.shutdownKubeClients();

        Map<String, KubernetesClient> k8sMap = TektonUtils.getKubernetesClientMap();
        Map<String, TektonClient> tektonMap = TektonUtils.getTektonClientMap();

        k8sMap.clear();
        tektonMap.clear();

        KubernetesClient mockK8sClient = kubernetesServer.getClient();
        TektonClient mockTektonClient = mockK8sClient.adapt(TektonClient.class);

        k8sMap.put("default", mockK8sClient);
        tektonMap.put("default", mockTektonClient);

        System.setProperty("KUBERNETES_NAMESPACE", "test");
    }

    private void ensureMockClientsInjected() {
        Map<String, KubernetesClient> k8sMap = TektonUtils.getKubernetesClientMap();
        Map<String, TektonClient> tektonMap = TektonUtils.getTektonClientMap();

        KubernetesClient mockK8sClient = kubernetesServer.getClient();
        TektonClient mockTektonClient = mockK8sClient.adapt(TektonClient.class);

        KubernetesClient currentClient = TektonUtils.getKubernetesClient("default");
        if (currentClient == null || !currentClient.getConfiguration().getMasterUrl().contains("localhost")) {
            k8sMap.put("default", mockK8sClient);
            tektonMap.put("default", mockTektonClient);
        }

        assertThat("Must use mock server URL after injection",
                TektonUtils.getKubernetesClient("default").getConfiguration().getMasterUrl(),
                containsString("localhost"));
    }

    @AfterEach
    void after() {
        try {
            TektonUtils.shutdownKubeClients();
            
            Map<String, KubernetesClient> k8sMap = TektonUtils.getKubernetesClientMap();
            Map<String, TektonClient> tektonMap = TektonUtils.getTektonClientMap();
            
            if (k8sMap != null) {
                k8sMap.clear();
            }
            
            if (tektonMap != null) {
                tektonMap.clear();
            }
            
            System.clearProperty("KUBERNETES_NAMESPACE");
            
            if (kubernetesServer != null) {
                kubernetesServer.after();
            }
            
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
            e.printStackTrace();
        } finally {
            kubernetesServer = null;
        }
    }

    @Test
    public void testScriptedPipelineWithFileInput_Task(JenkinsRule jenkins) throws Exception {
        jenkins.jenkins.getExtensionList(hudson.tasks.BuildStepDescriptor.class).add(new CreateRaw.DescriptorImpl());
        ensureMockClientsInjected();
        
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata()
                .withName("testTask")
                .endMetadata();

        kubernetesServer.expect()
                .post()
                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(200, taskBuilder.build())
                .once();

        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "p");
        URL zipFile = getClass().getResource("/org/waveywaves/jenkins/plugins/tekton/client/build/create/tekton-test-project.zip");
        assertThat(zipFile, is(notNullValue()));

        job.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "  unzip '" + zipFile.getPath() + "'\n" +
                "  tektonCreateRaw(inputType: 'FILE', input: '.tekton/task.yaml')\n" +
                "}", true));

        WorkflowRun run = jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());

        assertThat(kubernetesServer.getMockServer().getRequestCount(), is(1));

        String log = JenkinsRule.getLog(run);

        assertThat(log, containsString("Extracting: .tekton/task.yaml"));
        assertThat(log, containsString("[Pipeline] tektonCreateRaw"));
        assertThat(log, not(containsString(".tekton/task.yaml (No such file or directory)")));
    }

//    @Test
//    public void testDeclarativePipelineWithYamlInput_Task() throws Exception {
//        TaskBuilder taskBuilder = new TaskBuilder()
//                .withNewMetadata()
//                    .withName("testTask")
//                .endMetadata();
//
//        kubernetesRule.expect()
//                .post()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
//                .andReturn(HttpURLConnection.HTTP_OK, taskBuilder.build()).once();
//
//        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
//        URL zipFile = getClass().getResource("tekton-test-project.zip");
//        assertThat(zipFile, is(notNullValue()));
//
//        p.setDefinition(new CpsFlowDefinition("pipeline { \n"
//                                              + "  agent any\n"
//                                              + "  stages {\n"
//                                              + "    stage('Stage') {\n"
//                                              + "      steps {\n"
//                                              + "        unzip '" + zipFile.getPath() + "'\n"
//                                              + "        tektonCreateRaw(inputType: 'YAML', input: \"\"\"apiVersion: tekton.dev/v1beta1\n"
//                                              + "kind: Task\n"
//                                              + "metadata:\n"
//                                              + "  name: testTask\n"
//                                              + "\"\"\")\n"
//                                              + "      }\n"
//                                              + "    }\n"
//                                              + "  }\n"
//                                              + "}\n", true));
//
//        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());
//
//        assertThat(kubernetesRule.getMockServer().getRequestCount(), is(1));
//
//        String log = JenkinsRule.getLog(b);
//        System.out.println(log);
//
//        assertThat(log, containsString("Extracting: .tekton/task.yaml"));
//        assertThat(log, containsString("[Pipeline] tektonCreateRaw"));
//        assertThat(log, not(containsString(".tekton/task.yaml (No such file or directory)")));
//    }


//    @Test
//    public void testDeclarativePipelineWithYamlInput_PipelineRun() throws Exception {
//        ToolUtils.getJXPipelineBinary(ToolUtils.class.getClassLoader());
//
//        PipelineRunBuilder pipelineRunBuilder = new PipelineRunBuilder()
//                .withNewMetadata()
//                .withName("release")
//                .withNamespace("test")
//                .withUid("pipeline-run-uid")
//                .endMetadata()
//                .withNewSpec()
//                .withNewPipelineSpec()
//                .addNewTask()
//                .withName("pipelineTaskName")
//                .endTask()
//                .endPipelineSpec()
//                .endSpec()
//                .withNewStatus()
//                .withConditions(new Condition("lastTransitionTime","","","","True","Succeeded"))
//                .endStatus();
//
//        kubernetesRule.expect()
//                .post()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns")
//                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunBuilder.build())
//                .once();
//
//        kubernetesRule.expect()
//                .get()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns/release")
//                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunBuilder.build())
//                .once();
//
//        TaskRunList taskRunList = new TaskRunListBuilder()
//                .addToItems(
//                        new TaskRunBuilder()
//                                .withNewMetadata()
//                                .withName("testTaskRun")
//                                .withOwnerReferences(ownerReference("pipeline-run-uid"))
//                                .endMetadata()
//                                .build())
//                .build();
//
//        kubernetesRule.expect()
//                .get()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/taskruns?labelSelector=tekton.dev%2FpipelineTask%3DpipelineTaskName%2Ctekton.dev%2FpipelineRun%3Drelease")
//                .andReturn(HttpURLConnection.HTTP_OK, taskRunList)
//                .once();
//
//        Pod pod = new PodBuilder()
//                .withNewMetadata()
//                .withName("hello-world-pod")
//                .withNamespace("test")
//                .withOwnerReferences(ownerReference("TaskRun","testTaskRun"))
//                .endMetadata()
//                .withNewSpec()
//                .withContainers(
//                        new ContainerBuilder()
//                                .withName("hello-world-container")
//                                .build()
//                )
//                .endSpec()
//                .withNewStatus()
//                .withPhase("Succeeded")
//                .withContainerStatuses(
//                        new ContainerStatusBuilder()
//                                .withName("hello-world-container")
//                                .withState(
//                                        new ContainerStateBuilder()
//                                                .withTerminated(new ContainerStateTerminatedBuilder().withStartedAt("timestamp").build())
//                                                .build()
//                                )
//                                .build())
//                .endStatus()
//                .build();
//
//        PodList podList = new PodListBuilder()
//                .addToItems(pod)
//                .build();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/test/pods?labelSelector=tekton.dev%2FtaskRun%3DtestTaskRun")
//                .andReturn(HttpURLConnection.HTTP_OK, podList).once();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/test/pods/hello-world-pod")
//                .andReturn(HttpURLConnection.HTTP_OK, pod).always();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/test/pods/hello-world-pod/log?pretty=false&container=hello-world-container&follow=true")
//                .andReturn(HttpURLConnection.HTTP_OK, "Whoop! This is the pod log").once();
//
//        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
//        URL zipFile = getClass().getResource("tekton-test-project.zip");
//        assertThat(zipFile, is(notNullValue()));
//
//        p.setDefinition(new CpsFlowDefinition("pipeline { \n"
//                                              + "  agent any\n"
//                                              + "  stages {\n"
//                                              + "    stage('Stage') {\n"
//                                              + "      steps {\n"
//                                              + "        unzip '" + zipFile.getPath() + "'\n"
//                                              + "        tektonCreateRaw(inputType: 'YAML', input: \"\"\"apiVersion: tekton.dev/v1beta1\n"
//                                              + "kind: PipelineRun\n"
//                                              + "metadata:\n"
//                                              + "  name: release\n"
//                                              + "spec:\n"
//                                              + "  params:\n"
//                                              + "\"\"\")\n"
//                                              + "      }\n"
//                                              + "    }\n"
//                                              + "  }\n"
//                                              + "}\n", true));
//
//        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());
//
//        String log = JenkinsRule.getLog(b);
//        System.out.println(log);
//
//        assertThat(log, containsString("[Pipeline] tektonCreateRaw"));
//        assertThat(log, containsString("[Tekton] Pod test/hello-world-pod"));
//        assertThat(log, containsString("[Tekton] Pod test/hello-world-pod - Running..."));
//        assertThat(log, containsString("[Tekton] Container test/hello-world-pod/hello-world-container"));
//        assertThat(log, containsString("[Tekton] Container test/hello-world-pod/hello-world-container - Completed"));
//        assertThat(log, containsString("Whoop! This is the pod log"));
//
//        assertThat(kubernetesRule.getMockServer().getRequestCount(), is(9));
//    }

//    @Test
//    public void testDeclarativePipelineWithYamlInput_PipelineRun_DifferentNamespace() throws Exception {
//        ToolUtils.getJXPipelineBinary(ToolUtils.class.getClassLoader());
//
//        PipelineRunBuilder pipelineRunBuilder = new PipelineRunBuilder()
//                .withNewMetadata()
//                    .withName("release")
//                    .withNamespace("tekton-pipelines")
//                    .withUid("pipeline-run-uid")
//                .endMetadata()
//                .withNewSpec()
//                    .withNewPipelineSpec()
//                        .addNewTask()
//                            .withName("pipelineTaskName")
//                        .endTask()
//                    .endPipelineSpec()
//                .endSpec()
//                    .withNewStatus()
//                    .withConditions(new Condition("lastTransitionTime","","","","True","Succeeded"))
//                .endStatus();
//
//        kubernetesRule.expect()
//                .post()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/tekton-pipelines/pipelineruns")
//                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunBuilder.build())
//                .once();
//
//        kubernetesRule.expect()
//                .get()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/tekton-pipelines/pipelineruns/release")
//                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunBuilder.build())
//                .once();
//
//        TaskRunList taskRunList = new TaskRunListBuilder()
//                .addToItems(
//                        new TaskRunBuilder()
//                                .withNewMetadata()
//                                    .withName("testTaskRun")
//                                    .withOwnerReferences(ownerReference("pipeline-run-uid"))
//                                .endMetadata()
//                                .build())
//                .build();
//
//        kubernetesRule.expect()
//                .get()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/tekton-pipelines/taskruns?labelSelector=tekton.dev%2FpipelineTask%3DpipelineTaskName%2Ctekton.dev%2FpipelineRun%3Drelease")
//                .andReturn(HttpURLConnection.HTTP_OK, taskRunList)
//                .once();
//
//        Pod pod = new PodBuilder()
//                .withNewMetadata()
//                    .withName("hello-world-pod")
//                    .withNamespace("tekton-pipelines")
//                    .withOwnerReferences(ownerReference("TaskRun","testTaskRun"))
//                .endMetadata()
//                .withNewSpec()
//                .withContainers(
//                        new ContainerBuilder()
//                                .withName("hello-world-container")
//                                .build()
//                )
//                .endSpec()
//                .withNewStatus()
//                .withPhase("Succeeded")
//                .withContainerStatuses(
//                        new ContainerStatusBuilder()
//                                .withName("hello-world-container")
//                                .withState(
//                                        new ContainerStateBuilder()
//                                                .withTerminated(new ContainerStateTerminatedBuilder().withStartedAt("timestamp").build())
//                                                .build()
//                                )
//                                .build())
//                .endStatus()
//                .build();
//
//        PodList podList = new PodListBuilder()
//                .addToItems(pod)
//                .build();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/tekton-pipelines/pods?labelSelector=tekton.dev%2FtaskRun%3DtestTaskRun")
//                .andReturn(HttpURLConnection.HTTP_OK, podList).once();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/tekton-pipelines/pods/hello-world-pod")
//                .andReturn(HttpURLConnection.HTTP_OK, pod).always();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/tekton-pipelines/pods/hello-world-pod/log?pretty=false&container=hello-world-container&follow=true")
//                .andReturn(HttpURLConnection.HTTP_OK, "Whoop! This is the pod log").once();
//
//        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
//        URL zipFile = getClass().getResource("tekton-test-project.zip");
//        assertThat(zipFile, is(notNullValue()));
//
//        p.setDefinition(new CpsFlowDefinition("pipeline { \n"
//                                              + "  agent any\n"
//                                              + "  stages {\n"
//                                              + "    stage('Stage') {\n"
//                                              + "      steps {\n"
//                                              + "        unzip '" + zipFile.getPath() + "'\n"
//                                              + "        tektonCreateRaw(inputType: 'YAML', input: \"\"\"apiVersion: tekton.dev/v1beta1\n"
//                                              + "kind: PipelineRun\n"
//                                              + "metadata:\n"
//                                              + "  name: release\n"
//                                              + "spec:\n"
//                                              + "  params:\n"
//                                              + "\"\"\", namespace: 'tekton-pipelines')\n"
//                                              + "      }\n"
//                                              + "    }\n"
//                                              + "  }\n"
//                                              + "}\n", true));
//
//        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());
//
//        String log = JenkinsRule.getLog(b);
//        System.out.println(log);
//
//        assertThat(log, containsString("[Pipeline] tektonCreateRaw"));
//        assertThat(log, containsString("[Tekton] Pod tekton-pipelines/hello-world-pod"));
//        assertThat(log, containsString("[Tekton] Pod tekton-pipelines/hello-world-pod - Running..."));
//        assertThat(log, containsString("[Tekton] Container tekton-pipelines/hello-world-pod/hello-world-container"));
//        assertThat(log, containsString("[Tekton] Container tekton-pipelines/hello-world-pod/hello-world-container - Completed"));
//        assertThat(log, containsString("Whoop! This is the pod log"));
//
//        assertThat(kubernetesRule.getMockServer().getRequestCount(), is(9));
//    }

//    @Test
//    public void testDeclarativePipelineWithYamlInput_PipelineRun_FailingContainer() throws Exception {
//        ToolUtils.getJXPipelineBinary(ToolUtils.class.getClassLoader());
//
//        PipelineRunBuilder pipelineRunBuilder = new PipelineRunBuilder()
//                .withNewMetadata()
//                    .withName("release")
//                    .withNamespace("tekton-pipelines")
//                    .withUid("pipeline-run-uid")
//                .endMetadata()
//                .withNewSpec()
//                    .withNewPipelineSpec()
//                        .addNewTask()
//                            .withName("pipelineTaskName")
//                        .endTask()
//                    .endPipelineSpec()
//                .endSpec()
//                    .withNewStatus()
//                    .withConditions(new Condition("lastTransitionTime","","","","True","Succeeded"))
//                .endStatus();
//
//        kubernetesRule.expect()
//                .post()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/tekton-pipelines/pipelineruns")
//                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunBuilder.build())
//                .once();
//
//        kubernetesRule.expect()
//                .get()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/tekton-pipelines/pipelineruns/release")
//                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunBuilder.build())
//                .once();
//
//        TaskRunList taskRunList = new TaskRunListBuilder()
//                .addToItems(
//                        new TaskRunBuilder()
//                                .withNewMetadata()
//                                    .withName("testTaskRun")
//                                    .withOwnerReferences(ownerReference("pipeline-run-uid"))
//                                .endMetadata()
//                                .build())
//                .build();
//
//        kubernetesRule.expect()
//                .get()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/tekton-pipelines/taskruns?labelSelector=tekton.dev%2FpipelineTask%3DpipelineTaskName%2Ctekton.dev%2FpipelineRun%3Drelease")
//                .andReturn(HttpURLConnection.HTTP_OK, taskRunList)
//                .once();
//
//        Pod pod = new PodBuilder()
//                .withNewMetadata()
//                    .withName("hello-world-pod")
//                    .withNamespace("tekton-pipelines")
//                    .withOwnerReferences(ownerReference("TaskRun","testTaskRun"))
//                .endMetadata()
//                .withNewSpec()
//                .withContainers(
//                        new ContainerBuilder()
//                                .withName("hello-world-container")
//                                .build()
//                )
//                .endSpec()
//                .withNewStatus()
//                .withPhase("Failed")
//                .withContainerStatuses(
//                        new ContainerStatusBuilder()
//                                .withName("hello-world-container")
//                                .withState(
//                                        new ContainerStateBuilder()
//                                                .withTerminated(
//                                                        new ContainerStateTerminatedBuilder()
//                                                                .withStartedAt("timestamp")
//                                                                .withMessage("Failure Message")
//                                                                .withReason("Error")
//                                                                .withExitCode(1)
//                                                                .build())
//                                                .build()
//                                )
//                                .build())
//                .endStatus()
//                .build();
//
//        PodList podList = new PodListBuilder()
//                .addToItems(pod)
//                .build();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/tekton-pipelines/pods?labelSelector=tekton.dev%2FtaskRun%3DtestTaskRun")
//                .andReturn(HttpURLConnection.HTTP_OK, podList).once();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/tekton-pipelines/pods/hello-world-pod")
//                .andReturn(HttpURLConnection.HTTP_OK, pod).always();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/tekton-pipelines/pods/hello-world-pod/log?pretty=false&container=hello-world-container&follow=true")
//                .andReturn(HttpURLConnection.HTTP_OK, "Whoop! This is the pod log").once();
//
//        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
//        URL zipFile = getClass().getResource("tekton-test-project.zip");
//        assertThat(zipFile, is(notNullValue()));
//
//        p.setDefinition(new CpsFlowDefinition("pipeline { \n"
//                                              + "  agent any\n"
//                                              + "  stages {\n"
//                                              + "    stage('Stage') {\n"
//                                              + "      steps {\n"
//                                              + "        unzip '" + zipFile.getPath() + "'\n"
//                                              + "        tektonCreateRaw(inputType: 'YAML', input: \"\"\"apiVersion: tekton.dev/v1beta1\n"
//                                              + "kind: PipelineRun\n"
//                                              + "metadata:\n"
//                                              + "  name: release\n"
//                                              + "spec:\n"
//                                              + "  params:\n"
//                                              + "\"\"\", namespace: 'tekton-pipelines')\n"
//                                              + "      }\n"
//                                              + "    }\n"
//                                              + "  }\n"
//                                              + "}\n", true));
//
//        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
//
//        String log = JenkinsRule.getLog(b);
//        System.out.println(log);
//
//        assertThat(log, containsString("[Pipeline] tektonCreateRaw"));
//        assertThat(log, containsString("[Tekton] Pod tekton-pipelines/hello-world-pod"));
//        assertThat(log, containsString("[Tekton] Pod tekton-pipelines/hello-world-pod - Running..."));
//        assertThat(log, containsString("[Tekton] Container tekton-pipelines/hello-world-pod/hello-world-container"));
//        assertThat(log, containsString("[Tekton] Container tekton-pipelines/hello-world-pod/hello-world-container - Error"));
//        assertThat(log, containsString("[Tekton] Pod tekton-pipelines/hello-world-pod Status: Failed"));
//        assertThat(log, containsString("Whoop! This is the pod log"));
//
//        assertThat(kubernetesRule.getMockServer().getRequestCount(), is(8));
//    }

//    @Test
//    public void testDeclarativePipelineWithYamlInput_PipelineRun_FailingPipelineRun() throws Exception {
//        ToolUtils.getJXPipelineBinary(ToolUtils.class.getClassLoader());
//
//        PipelineRunBuilder pipelineRunBuilder = new PipelineRunBuilder()
//                .withNewMetadata()
//                    .withName("release")
//                    .withNamespace("tekton-pipelines")
//                    .withUid("pipeline-run-uid")
//                .endMetadata()
//                .withNewSpec()
//                    .withNewPipelineSpec()
//                        .addNewTask()
//                            .withName("pipelineTaskName")
//                        .endTask()
//                    .endPipelineSpec()
//                .endSpec()
//                    .withNewStatus()
//                    .withConditions(new Condition("lastTransitionTime","Failure Message","Failure Reason","","False","Succeeded"))
//                .endStatus();
//
//        kubernetesRule.expect()
//                .post()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/tekton-pipelines/pipelineruns")
//                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunBuilder.build())
//                .once();
//
//        kubernetesRule.expect()
//                .get()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/tekton-pipelines/pipelineruns/release")
//                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunBuilder.build())
//                .once();
//
//        TaskRunList taskRunList = new TaskRunListBuilder()
//                .addToItems(
//                        new TaskRunBuilder()
//                                .withNewMetadata()
//                                .withName("testTaskRun")
//                                .withOwnerReferences(ownerReference("pipeline-run-uid"))
//                                .endMetadata()
//                                .build())
//                .build();
//
//        kubernetesRule.expect()
//                .get()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/tekton-pipelines/taskruns?labelSelector=tekton.dev%2FpipelineTask%3DpipelineTaskName%2Ctekton.dev%2FpipelineRun%3Drelease")
//                .andReturn(HttpURLConnection.HTTP_OK, taskRunList)
//                .once();
//
//        Pod pod = new PodBuilder()
//                .withNewMetadata()
//                .withName("hello-world-pod")
//                .withNamespace("tekton-pipelines")
//                .withOwnerReferences(ownerReference("TaskRun","testTaskRun"))
//                .endMetadata()
//                .withNewSpec()
//                .withContainers(
//                        new ContainerBuilder()
//                                .withName("hello-world-container")
//                                .build()
//                )
//                .endSpec()
//                .withNewStatus()
//                .withPhase("Succeeded")
//                .withContainerStatuses(
//                        new ContainerStatusBuilder()
//                                .withName("hello-world-container")
//                                .withState(
//                                        new ContainerStateBuilder()
//                                                .withTerminated(new ContainerStateTerminatedBuilder().withStartedAt("timestamp").build())
//                                                .build()
//                                )
//                                .build())
//                .endStatus()
//                .build();
//
//        PodList podList = new PodListBuilder()
//                .addToItems(pod)
//                .build();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/tekton-pipelines/pods?labelSelector=tekton.dev%2FtaskRun%3DtestTaskRun")
//                .andReturn(HttpURLConnection.HTTP_OK, podList).once();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/tekton-pipelines/pods/hello-world-pod")
//                .andReturn(HttpURLConnection.HTTP_OK, pod).always();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/tekton-pipelines/pods/hello-world-pod/log?pretty=false&container=hello-world-container&follow=true")
//                .andReturn(HttpURLConnection.HTTP_OK, "Whoop! This is the pod log").once();
//
//        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
//        URL zipFile = getClass().getResource("tekton-test-project.zip");
//        assertThat(zipFile, is(notNullValue()));
//
//        p.setDefinition(new CpsFlowDefinition("pipeline { \n"
//                                              + "  agent any\n"
//                                              + "  stages {\n"
//                                              + "    stage('Stage') {\n"
//                                              + "      steps {\n"
//                                              + "        unzip '" + zipFile.getPath() + "'\n"
//                                              + "        tektonCreateRaw(inputType: 'YAML', input: \"\"\"apiVersion: tekton.dev/v1beta1\n"
//                                              + "kind: PipelineRun\n"
//                                              + "metadata:\n"
//                                              + "  name: release\n"
//                                              + "spec:\n"
//                                              + "  params:\n"
//                                              + "\"\"\", namespace: 'tekton-pipelines')\n"
//                                              + "      }\n"
//                                              + "    }\n"
//                                              + "  }\n"
//                                              + "}\n", true));
//
//        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
//
//        String log = JenkinsRule.getLog(b);
//        System.out.println(log);
//
//        assertThat(log, containsString("[Pipeline] tektonCreateRaw"));
//        assertThat(log, containsString("[Tekton] Pod tekton-pipelines/hello-world-pod"));
//        assertThat(log, containsString("[Tekton] Pod tekton-pipelines/hello-world-pod - Running..."));
//        assertThat(log, containsString("[Tekton] Container tekton-pipelines/hello-world-pod/hello-world-container"));
//        assertThat(log, containsString("[Tekton] Container tekton-pipelines/hello-world-pod/hello-world-container - Completed"));
//        assertThat(log, containsString("Whoop! This is the pod log"));
//
//        assertThat(kubernetesRule.getMockServer().getRequestCount(), is(9));
//    }

//    @Test
//    public void testDeclarativePipelineWithYamlInput_MultipleDocuments() throws Exception {
//        ToolUtils.getJXPipelineBinary(ToolUtils.class.getClassLoader());
//
//        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
//        URL zipFile = getClass().getResource("tekton-test-project.zip");
//        assertThat(zipFile, is(notNullValue()));
//
//        p.setDefinition(new CpsFlowDefinition("pipeline { \n"
//                                              + "  agent any\n"
//                                              + "  stages {\n"
//                                              + "    stage('Stage') {\n"
//                                              + "      steps {\n"
//                                              + "        unzip '" + zipFile.getPath() + "'\n"
//                                              + "        tektonCreateRaw(inputType: 'YAML', input: \"\"\"apiVersion: tekton.dev/v1beta1\n"
//                                              + "kind: PipelineRun\n"
//                                              + "metadata:\n"
//                                              + "  name: release\n"
//                                              + "spec:\n"
//                                              + "  params:\n"
//                                              + "---\n"
//                                              + "apiVersion: tekton.dev/v1beta1\n"
//                                              + "kind: Task\n"
//                                              + "metadata:\n"
//                                              + "  name: task\n"
//                                              + "\"\"\", namespace: 'tekton-pipelines')\n"
//                                              + "      }\n"
//                                              + "    }\n"
//                                              + "  }\n"
//                                              + "}\n", true));
//
//        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
//
//        String log = JenkinsRule.getLog(b);
//        System.out.println(log);
//
//        assertThat(log, containsString("[Pipeline] tektonCreateRaw"));
//        assertThat(log, containsString("Multiple Objects in YAML not supported yet"));
//
//        assertThat(kubernetesRule.getMockServer().getRequestCount(), is(0));
//    }

    private OwnerReference ownerReference(String uid) {
        return new OwnerReference("", false, false, "", "", uid);
    }

    private OwnerReference ownerReference(String kind, String name) {
        return new OwnerReference("", false, false, kind, name, "");
    }
}
