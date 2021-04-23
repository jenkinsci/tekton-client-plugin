package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.model.Result;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskBuilder;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.jvnet.hudson.test.JenkinsRule;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
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

    //@Test
    public void testScriptedPipeline() throws Exception {
        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        URL zipFile = getClass().getResource("tekton-test-project.zip");
        assertThat(zipFile, is(notNullValue()));

        p.setDefinition(new CpsFlowDefinition("node {\n"
                                              + "  unzip '" + zipFile.getPath() + "'\n"
                                              + "  createRaw(inputType: 'FILE', input: '.tekton/task.yaml')\n"
                                              + "}\n", true));

        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        String log = jenkinsRule.getLog(b);
        System.out.println(log);

        assertThat(log, containsString("Extracting: .tekton/task.yaml"));
        assertThat(log, containsString("[Pipeline] createRaw"));
        assertThat(log, not(containsString(".tekton/task.yaml (No such file or directory)")));
    }

    //@Test
    public void testDeclarativePipelineWithFileInput() throws Exception {
        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        URL zipFile = getClass().getResource("tekton-test-project.zip");
        assertThat(zipFile, is(notNullValue()));

        p.setDefinition(new CpsFlowDefinition("pipeline { \n"
                                              + "  agent any\n"
                                              + "  stages {\n"
                                              + "    stage('Stage') {\n"
                                              + "      steps {\n"
                                              + "        unzip '" + zipFile.getPath() + "'\n"
                                              + "        createRaw(inputType: 'FILE', input: '.tekton/task.yaml')\n"
                                              + "      }\n"
                                              + "    }\n"
                                              + "  }\n"
                                              + "}\n", true));

        WorkflowRun b = jenkinsRule.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        String log = jenkinsRule.getLog(b);
        System.out.println(log);

        assertThat(log, containsString("Extracting: .tekton/task.yaml"));
        assertThat(log, containsString("[Pipeline] createRaw"));
        assertThat(log, not(containsString(".tekton/task.yaml (No such file or directory)")));
    }

    @Test
    public void testDeclarativePipelineWithYamlInput() throws Exception {
        TaskBuilder taskBuilder = new TaskBuilder()
                .withNewMetadata().withName("testTask").endMetadata();
        List<Task> tList = new ArrayList<Task>();
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
                                              + "        createRaw(inputType: 'YAML', input: \"\"\"apiVersion: tekton.dev/v1beta1\n"
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
        assertThat(log, containsString("[Pipeline] createRaw"));
        assertThat(log, not(containsString(".tekton/task.yaml (No such file or directory)")));
    }
}
