package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskBuilder;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.mockwebserver.RecordedRequest;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.ToolUtils;
import org.waveywaves.jenkins.plugins.tekton.client.global.ClusterConfig;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JenkinsTest {

    public JenkinsRule jenkinsRule = new JenkinsRule();
    public KubernetesServer kubernetesRule = new KubernetesServer();

    @Rule
    public TestRule chain =
            RuleChain.outerRule(kubernetesRule)
                    .around(jenkinsRule);

    @Before
    public void before() {
        KubernetesClient client = kubernetesRule.getClient();
        Config config = client.getConfiguration();
        TektonUtils.initializeKubeClients(config);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata().withName("testTask").endMetadata();
        Task testTask = taskBuilder.build();

        kubernetesRule.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_CREATED, testTask).once();

        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        URL zipFile = getClass().getResource("tekton-test-project.zip");
        assertThat(zipFile, is(notNullValue()));

        p.setDefinition(new CpsFlowDefinition("node {\n"
                                              + "  unzip '" + zipFile.getPath() + "'\n"
                                              + "  tektonCreateRaw(inputType: 'FILE', input: '.tekton/task.yaml')\n"
                                              + "}\n", true));

        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        String log = jenkinsRule.getLog(b);
        System.out.println(log);

        assertThat(log, containsString("Extracting: .tekton/task.yaml"));
        assertThat(log, containsString("[Pipeline] tektonCreateRaw"));
        assertThat(log, not(containsString(".tekton/task.yaml (No such file or directory)")));
    }

    @Test
    public void testDeclarativePipelineWithFileInput() throws Exception {
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata().withName("testTask").endMetadata();
        Task testTask = taskBuilder.build();

        kubernetesRule.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_CREATED, testTask).once();

        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        URL zipFile = getClass().getResource("tekton-test-project.zip");
        assertThat(zipFile, is(notNullValue()));

        p.setDefinition(new CpsFlowDefinition("pipeline { \n"
                                              + "  agent any\n"
                                              + "  stages {\n"
                                              + "    stage('Stage') {\n"
                                              + "      steps {\n"
                                              + "        unzip '" + zipFile.getPath() + "'\n"
                                              + "        tektonCreateRaw(inputType: 'FILE', input: '.tekton/task.yaml')\n"
                                              + "      }\n"
                                              + "    }\n"
                                              + "  }\n"
                                              + "}\n", true));

        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        String log = jenkinsRule.getLog(b);
        System.out.println(log);

        assertThat(log, containsString("Extracting: .tekton/task.yaml"));
        assertThat(log, containsString("[Pipeline] tektonCreateRaw"));
        assertThat(log, not(containsString(".tekton/task.yaml (No such file or directory)")));
    }

    @Test
    public void testDeclarativePipelineWithYamlInput() throws Exception {
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata().withName("testTask").endMetadata();
        Task testTask = taskBuilder.build();

        kubernetesRule.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_CREATED, testTask).once();

        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        URL zipFile = getClass().getResource("tekton-test-project.zip");
        assertThat(zipFile, is(notNullValue()));

        p.setDefinition(new CpsFlowDefinition("pipeline { \n"
                                              + "  agent any\n"
                                              + "  stages {\n"
                                              + "    stage('Stage') {\n"
                                              + "      steps {\n"
                                              + "        unzip '" + zipFile.getPath() + "'\n"
                                              + "        tektonCreateRaw(inputType: 'YAML', input: \"\"\"apiVersion: tekton.dev/v1beta1\n"
                                              + "kind: Task\n"
                                              + "metadata:\n"
                                              + "  name: testTask\n"
                                              + "\"\"\")\n"
                                              + "      }\n"
                                              + "    }\n"
                                              + "  }\n"
                                              + "}\n", true));

        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        String log = jenkinsRule.getLog(b);
        System.out.println(log);

        assertThat(log, containsString("Extracting: .tekton/task.yaml"));
        assertThat(log, containsString("[Pipeline] tektonCreateRaw"));
        assertThat(log, not(containsString(".tekton/task.yaml (No such file or directory)")));
    }

    @Test
    public void testFreestyleJobWithFileInput() throws Exception {
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata().withName("testTask").endMetadata();
        Task testTask = taskBuilder.build();

        kubernetesRule.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_CREATED, testTask).once();

        FreeStyleProject p = jenkinsRule.jenkins.createProject(FreeStyleProject.class, "p");
        URL zipFile = getClass().getResource("tekton-test-project.zip");
        assertThat(zipFile, is(notNullValue()));

        p.setScm(new ExtractResourceSCM(zipFile));
        p.getBuildersList().add(new CreateRaw(".tekton/task.yaml", "FILE"));

        FreeStyleBuild b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        String log = jenkinsRule.getLog(b);
        System.out.println(log);

        assertThat(log, containsString("Legacy code started this job"));
    }

    @Test
    public void testFreestyleJobWithYamlInput() throws Exception {
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata().withName("testTask").endMetadata();
        Task testTask = taskBuilder.build();

        kubernetesRule.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/tasks")
                .andReturn(HttpURLConnection.HTTP_CREATED, testTask).once();

        FreeStyleProject p = jenkinsRule.jenkins.createProject(FreeStyleProject.class, "p");
        URL zipFile = getClass().getResource("tekton-test-project.zip");
        assertThat(zipFile, is(notNullValue()));

        p.setScm(new ExtractResourceSCM(zipFile));
        p.getBuildersList().add(new CreateRaw("apiVersion: tekton.dev/v1beta1\n"
                                              + "kind: Task\n"
                                              + "metadata:\n"
                                              + "  name: testTask\n", "YAML"));

        FreeStyleBuild b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        String log = jenkinsRule.getLog(b);
        System.out.println(log);

        assertThat(log, containsString("Legacy code started this job"));
    }

    @Test
    public void testFreestyleJobWithComplexYamlInput() throws Exception {
        ToolUtils.getJXPipelineBinary(ToolUtils.class.getClassLoader());

        PipelineRunBuilder pipelineRunBuilder = new PipelineRunBuilder()
                .withNewMetadata().withName("release").endMetadata();
        PipelineRun testPipelineRun = pipelineRunBuilder.build();

        kubernetesRule.expect().post().withPath("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns")
                .andReturn(HttpURLConnection.HTTP_CREATED, testPipelineRun).once();

        FreeStyleProject p = jenkinsRule.jenkins.createProject(FreeStyleProject.class, "p");
        URL zipFile = getClass().getResource("tekton-test-project.zip");
        assertThat(zipFile, is(notNullValue()));

        p.setScm(new ExtractResourceSCM(zipFile));
        CreateRaw createRaw = new CreateRaw("apiVersion: tekton.dev/v1beta1\n"
                                  + "kind: PipelineRun\n"
                                  + "metadata:\n"
                                  + "  creationTimestamp: null\n"
                                  + "  name: release\n"
                                  + "spec:\n"
                                  + "  pipelineSpec:\n"
                                  + "    tasks:\n"
                                  + "    - name: from-build-pack\n"
                                  + "      resources: {}\n"
                                  + "      taskSpec:\n"
                                  + "        metadata: {}\n"
                                  + "        stepTemplate:\n"
                                  + "          image: uses:jenkins-x/jx3-pipeline-catalog/tasks/go/release.yaml@versionStream\n"
                                  + "          name: \"\"\n"
                                  + "          resources:\n"
                                  + "            requests:\n"
                                  + "              cpu: 400m\n"
                                  + "              memory: 600Mi\n"
                                  + "          workingDir: /workspace/source\n"
                                  + "        steps:\n"
                                  + "        - image: uses:jenkins-x/jx3-pipeline-catalog/tasks/git-clone/git-clone.yaml@versionStream\n"
                                  + "          name: \"\"\n"
                                  + "          resources: {}\n"
                                  + "        - name: next-version\n"
                                  + "          resources: {}\n"
                                  + "        - name: jx-variables\n"
                                  + "          resources: {}\n"
                                  + "        - name: build-make-build\n"
                                  + "          resources: {}\n"
                                  + "        - name: promote-changelog\n"
                                  + "          resources: {}\n"
                                  + "  podTemplate: {}\n"
                                  + "  serviceAccountName: tekton-bot\n"
                                  + "  timeout: 240h0m0s\n"
                                  + "status: {}\n", "YAML");
        createRaw.setEnableCatalog(true);
        p.getBuildersList().add(createRaw);

        FreeStyleBuild b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        String log = jenkinsRule.getLog(b);
        System.out.println(log);

        assertThat(log, containsString("Legacy code started this job"));
        assertThat(kubernetesRule.getMockServer().getRequestCount(), is(1));

        RecordedRequest request = kubernetesRule.getLastRequest();

        assertThat(request.getPath(), is("/apis/tekton.dev/v1beta1/namespaces/test/pipelineruns"));
        assertThat(request.getMethod(), is("POST"));
        String body = request.getBody().readUtf8();
        // this is the modification from the fake jx-pipeline-effective
        assertThat(body, containsString("\"labels\":{\"cheese\":null}"));
    }
}
