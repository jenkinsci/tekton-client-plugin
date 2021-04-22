package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.model.Result;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import java.net.URL;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.jvnet.hudson.test.JenkinsRule;

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

    @Test
    public void testPipelineWithNoTags() throws Exception {
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

        assertThat(log, containsString("configuring tekton paths from .tekton/task.yaml of type FILE"));
        assertThat(log, not(containsString("[WARNING] inputFile does not exist: .tekton/task.yaml")));
    }

    @Test
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

        assertThat(log, containsString("configuring tekton paths from .tekton/task.yaml of type FILE"));
        assertThat(log, not(containsString("[WARNING] inputFile does not exist: .tekton/task.yaml")));
    }
}
