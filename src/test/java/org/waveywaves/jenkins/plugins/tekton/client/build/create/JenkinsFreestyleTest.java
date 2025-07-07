/**
 * DONE testFreestyleJobWithFileInput: Verifies CreateRaw reads Tekton YAML from a file.
 * DONE testFreestyleJobWithYamlInput: Verifies CreateRaw handles direct YAML string input.
 * testFreestyleJobWithComplexYamlInput: Verifies full PipelineRun + TaskRun + Pod lifecycle with log streaming.
 * testFreestyleJobWithExpandedYamlInput: Verifies CreateRaw processes an expanded pipeline YAML.
 */

package org.waveywaves.jenkins.plugins.tekton.client.build.create;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.ToolUtils;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.tekton.pipeline.v1beta1.*;
import io.fabric8.kubernetes.api.model.ContainerStateTerminatedBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerStateBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.Config;

import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.JenkinsRule;
import org.junit.jupiter.api.*;


import java.net.HttpURLConnection;
import java.net.URL;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import com.google.common.io.Resources;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

@WithJenkins
public class JenkinsFreestyleTest {

    private KubernetesServer kubernetesServer;

    @BeforeEach
    void before() {
        kubernetesServer = new KubernetesServer(false, true);
        kubernetesServer.before(); 
        Config config = kubernetesServer.getClient().getConfiguration();
        TektonUtils.initializeKubeClients(config);
    }

    @AfterEach
    void after() {
        kubernetesServer.after(); 
    }

    @Test
    void testFreestyleJobWithFileInput(JenkinsRule jenkins) throws Exception {
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata()
                .withName("testTask")
                .endMetadata();

        kubernetesServer.expect()
                .post()
                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(200, taskBuilder.build())
                .once();

        // Load test resource
        URL zipFile = getClass().getResource("/org/waveywaves/jenkins/plugins/tekton/client/build/create/tekton-test-project.zip");
        assertThat("Test zip file must exist", zipFile, is(notNullValue()));

        // Create freestyle project
        FreeStyleProject project = jenkins.createFreeStyleProject("p");
        project.setScm(new ExtractResourceSCM(zipFile));
        project.getBuildersList().add(new CreateRaw(".tekton/task.yaml", "FILE"));

        // Act: Build job
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

        // Assert
        assertThat(kubernetesServer.getMockServer().getRequestCount(), is(1));
        String log = JenkinsRule.getLog(build);
        assertThat(log, containsString("Legacy code started this job"));
    }


   @Test
   public void testFreestyleJobWithYamlInput(JenkinsRule jenkins) throws Exception {
       TaskBuilder taskBuilder = new TaskBuilder()
               .withNewMetadata()
                   .withName("testTask")
               .endMetadata();

kubernetesServer.expect()
                .post()
                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(200, taskBuilder.build())
                .once();

       URL zipFile = getClass().getResource("/org/waveywaves/jenkins/plugins/tekton/client/build/create/tekton-test-project.zip");
       assertThat(zipFile, is(notNullValue()));

       FreeStyleProject p = jenkins.createFreeStyleProject("p");
        p.setScm(new ExtractResourceSCM(zipFile));

       p.getBuildersList().add(new CreateRaw("apiVersion: tekton.dev/v1beta1\n"
                                             + "kind: Task\n"
                                             + "metadata:\n"
                                             + "  name: testTask\n", "YAML"));

       FreeStyleBuild b = jenkins.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

       assertThat(kubernetesServer.getMockServer().getRequestCount(), is(1));

       String log = JenkinsRule.getLog(b);
       System.out.println(log);

       assertThat(log, containsString("Legacy code started this job"));
   }
//////////////////////////// testFreestyleJobWithComplexYamlInput //////////////////////////////////
//    @Test
//    public void testFreestyleJobWithComplexYamlInput(JenkinsRule jenkins) throws Exception {
//        ToolUtils.getJXPipelineBinary(ToolUtils.class.getClassLoader());
//         PipelineRunBuilder pipelineRunBuilder = new PipelineRunBuilder()
//                 .withNewMetadata()
//                     .withName("release")
//                     .withNamespace("test")
//                     .withUid("pipeline-run-uid")
//                 .endMetadata()
//                 .withNewSpec()
//                     .withNewPipelineSpec()
//                         .addNewTask()
//                             .withName("pipelineTaskName")
//                         .endTask()
//                     .endPipelineSpec()
//                 .endSpec()
//                 .withNewStatus()
//                     .withConditions(new Condition("lastTransitionTime", "", "", "", "True", "Succeeded"))
//                 .endStatus();

//        kubernetesServer.expect()
//                .post()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns")
//                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunBuilder.build())
//                .once();

//        kubernetesServer.expect()
//                .get()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns/release")
//                .andReturn(HttpURLConnection.HTTP_OK, pipelineRunBuilder.build())
//                .once();

//        TaskRunList taskRunList = new TaskRunListBuilder()
//                .addToItems(
//                        new TaskRunBuilder()
//                            .withNewMetadata()
//                                .withName("testTaskRun")
//                                .withOwnerReferences(ownerReference("pipeline-run-uid"))
//                            .endMetadata()
//                        .build())
//                .build();

//        kubernetesServer.expect()
//                .get()
//                .withPath("/apis/tekton.dev/v1beta1/namespaces/test/taskruns?labelSelector=tekton.dev%2FpipelineTask%3DpipelineTaskName%2Ctekton.dev%2FpipelineRun%3Drelease")
//                .andReturn(HttpURLConnection.HTTP_OK, taskRunList)
//                .once();

//        Pod pod = new PodBuilder()
//                .withNewMetadata()
//                    .withName("hello-world-pod")
//                    .withNamespace("test")
//                    .withOwnerReferences(ownerReference("TaskRun","testTaskRun"))
//                .endMetadata()
//                .withNewSpec()
//                    .withContainers(
//                        new ContainerBuilder()
//                             .withName("hello-world-container")
//                        .build()
//                    )
//                .endSpec()
//                .withNewStatus()
//                    .withPhase("Succeeded")
//                    .withContainerStatuses(
//                            new ContainerStatusBuilder()
//                                    .withName("hello-world-container")
//                                    .withState(
//                                            new ContainerStateBuilder()
//                                                    .withTerminated(new ContainerStateTerminatedBuilder().withStartedAt("timestamp").build())
//                                            .build()
//                                    )
//                            .build())
//                .endStatus()
//                .build();

//        PodList podList = new PodListBuilder()
//                .addToItems(pod)
//                .build();

//        kubernetesServer.expect().get().withPath("/api/v1/namespaces/test/pods?labelSelector=tekton.dev%2FtaskRun%3DtestTaskRun")
//                .andReturn(HttpURLConnection.HTTP_OK, podList).once();

//        kubernetesServer.expect().get().withPath("/api/v1/namespaces/test/pods/hello-world-pod")
//                .andReturn(HttpURLConnection.HTTP_OK, pod).always();

//        kubernetesServer.expect().get().withPath("/api/v1/namespaces/test/pods/hello-world-pod/log?pretty=false&container=hello-world-container&follow=true")
//                .andReturn(HttpURLConnection.HTTP_OK, "Whoop! This is the pod log").once();

//        FreeStyleProject p = jenkins.jenkins.createProject(FreeStyleProject.class, "p");
//        URL zipFile = getClass().getResource("/org/waveywaves/jenkins/plugins/tekton/client/build/create/tekton-test-project.zip");
//        assertThat(zipFile, is(notNullValue()));

//        p.setScm(new ExtractResourceSCM(zipFile));
//        CreateRaw createRaw = new CreateRaw(contents("jx-pipeline.yaml"), "YAML");
//        createRaw.setEnableCatalog(true);
//        p.getBuildersList().add(createRaw);

//        FreeStyleBuild b = jenkins.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

//        String log = JenkinsRule.getLog(b);
//        System.out.println(log);

//        assertThat(log, containsString("Legacy code started this job"));
//        assertThat(log, containsString("Whoop! This is the pod log"));

//        assertThat(kubernetesServer.getMockServer().getRequestCount(), is(9));
//    }

//    @Test
//    public void testFreestyleJobWithExpandedYamlInput() throws Exception {
//        ToolUtils.getJXPipelineBinary(ToolUtils.class.getClassLoader());
//
//        PipelineRunBuilder pipelineRunBuilder = new PipelineRunBuilder()
//                .withNewMetadata()
//                    .withName("release")
//                    .withNamespace("test")
//                    .withUid("pipeline-run-uid")
//                .endMetadata()
//                .withNewSpec()
//                    .withNewPipelineSpec()
//                        .addNewTask()
//                            .withName("pipelineTaskName")
//                        .endTask()
//                    .endPipelineSpec()
//                .endSpec()
//                .withNewStatus()
//                    .withConditions(new Condition("lastTransitionTime","","","","True","Succeeded"))
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
//                                    .withName("testTaskRun")
//                                    .withOwnerReferences(ownerReference("pipeline-run-uid"))
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
//                    .withName("hello-world-pod")
//                    .withNamespace("test")
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
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/test/pods?labelSelector=tekton.dev%2FtaskRun%3DtestTaskRun")
//                .andReturn(HttpURLConnection.HTTP_OK, podList).once();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/test/pods/hello-world-pod")
//                .andReturn(HttpURLConnection.HTTP_OK, pod).always();
//
//        kubernetesRule.expect().get().withPath("/api/v1/namespaces/test/pods/hello-world-pod/log?pretty=false&container=hello-world-container&follow=true")
//                .andReturn(HttpURLConnection.HTTP_OK, "Whoop! This is the pod log").once();
//
//        FreeStyleProject p = jenkinsRule.jenkins.createProject(FreeStyleProject.class, "p");
//        URL zipFile = getClass().getResource("tekton-test-project.zip");
//        assertThat(zipFile, is(notNullValue()));
//
//        p.setScm(new ExtractResourceSCM(zipFile));
//        CreateRaw createRaw = new CreateRaw(contents("jx-pipeline.expanded.yaml"), "YAML");
//        createRaw.setEnableCatalog(false);
//        p.getBuildersList().add(createRaw);
//
//        FreeStyleBuild b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());
//
//        String log = JenkinsRule.getLog(b);
//        System.out.println(log);
//
//        assertThat(log, containsString("Legacy code started this job"));
//        assertThat(log, containsString("Whoop! This is the pod log"));
//
//        assertThat(kubernetesRule.getMockServer().getRequestCount(), is(9));
//    }
//
private String contents(String name) throws IOException {
    URL url = getClass().getResource("/org/waveywaves/jenkins/plugins/tekton/client/build/create/" + name);
    if (url == null) {
        throw new IllegalArgumentException("Could not find file: " + name);
    }
    return Resources.toString(url, StandardCharsets.UTF_8);
}


   private OwnerReference ownerReference(String uid) {
       return new OwnerReference("", false, false, "", "", uid);
   }

   private OwnerReference ownerReference(String kind, String name) {
       return new OwnerReference("", false, false, kind, name, "");
   }
}
