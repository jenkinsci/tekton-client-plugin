package org.waveywaves.jenkins.plugins.tekton.client.e2e;

import hudson.model.*;
import hudson.tasks.Shell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.CreateRaw;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TektonJenkinsBuildE2ETest extends E2ETestBase {

  @BeforeEach
  public void setUp() throws Exception {
    // Called after E2ETestBase setup
  }

  @Test
  public void testFreestyleBuildWithJenkinsVariables() throws Exception {
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-jenkins-integration");

    String shellScript = String.format("""
        #!/bin/bash
        cat > taskrun.yaml << EOF
        apiVersion: tekton.dev/v1beta1
        kind: TaskRun
        metadata:
          name: jenkins-build-$BUILD_NUMBER
          namespace: %s
          labels:
            build-number: "$BUILD_NUMBER"
            job-name: "$JOB_NAME"
        spec:
          params:
          - name: build-id
            value: "$BUILD_ID"
          - name: job-name
            value: "$JOB_NAME"
          - name: build-number
            value: "$BUILD_NUMBER"
          taskSpec:
            params:
            - name: build-id
              type: string
            - name: job-name
              type: string
            - name: build-number
              type: string
            steps:
            - name: show-build-info
              image: busybox
              command:
              - sh
              - -c
              - |
                echo "Jenkins Build ID: $(params.build-id)"
                echo "Jenkins Job Name: $(params.job-name)"
                echo "Jenkins Build Number: $(params.build-number)"
                echo "Build integration successful!"
        EOF
        """, getCurrentTestNamespace());

    project.getBuildersList().add(new Shell(shellScript));

    CreateRaw createStep = new CreateRaw("taskrun.yaml", "FILE");
    createStep.setNamespace(getCurrentTestNamespace());
    createStep.setClusterName("default");
    project.getBuildersList().add(createStep);

    FreeStyleBuild build = project.scheduleBuild2(0).get(3, TimeUnit.MINUTES);
    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    String taskRunName = "jenkins-build-" + build.getNumber();
    Thread.sleep(5000);

    TaskRun taskRun = tektonClient.v1beta1().taskRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName(taskRunName)
        .get();

    assertThat(taskRun).isNotNull();
    assertThat(taskRun.getMetadata().getLabels())
        .containsEntry("build-number", String.valueOf(build.getNumber()))
        .containsEntry("job-name", project.getName());

    assertThat(taskRun.getSpec().getParams()).hasSize(3);
    waitForTaskRunCompletion(taskRunName, getCurrentTestNamespace());

    TaskRun completed = tektonClient.v1beta1().taskRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName(taskRunName)
        .get();

    assertThat(completed.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
    assertThat(completed.getStatus().getConditions().get(0).getStatus()).isEqualTo("True");
  }

  @Test
  public void testParameterizedBuildWithTekton() throws Exception {
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-parameterized-build");

    project.addProperty(new ParametersDefinitionProperty(
        new StringParameterDefinition("APP_NAME", "my-app", "Application name"),
        new StringParameterDefinition("VERSION", "1.0.0", "Application version"),
        new ChoiceParameterDefinition("ENVIRONMENT", new String[] { "dev", "staging", "prod" }, "Target environment")));

    String shellScript = String.format("""
        #!/bin/bash
        cat > taskrun.yaml << EOF
        apiVersion: tekton.dev/v1beta1
        kind: TaskRun
        metadata:
          name: param-build-$BUILD_NUMBER
          namespace: %s
          labels:
            app: "$APP_NAME"
            version: "$VERSION"
            env: "$ENVIRONMENT"
        spec:
          params:
          - name: app-name
            value: "$APP_NAME"
          - name: version
            value: "$VERSION"
          - name: environment
            value: "$ENVIRONMENT"
          - name: build-number
            value: "$BUILD_NUMBER"
          taskSpec:
            params:
            - name: app-name
              type: string
            - name: version
              type: string
            - name: environment
              type: string
            - name: build-number
              type: string
            steps:
            - name: build-app
              image: busybox
              command:
              - sh
              - -c
              - |
                echo "Building application: $(params.app-name)"
                echo "Version: $(params.version)"
                echo "Environment: $(params.environment)"
                echo "Build Number: $(params.build-number)"
                echo "Parameterized build successful!"
        EOF
        """, getCurrentTestNamespace());

    project.getBuildersList().add(new Shell(shellScript));

    CreateRaw createStep = new CreateRaw("taskrun.yaml", "FILE");
    createStep.setNamespace(getCurrentTestNamespace());
    createStep.setClusterName("default");
    project.getBuildersList().add(createStep);

    FreeStyleBuild build = project.scheduleBuild2(0, new ParametersAction(
        new StringParameterValue("APP_NAME", "test-app"),
        new StringParameterValue("VERSION", "2.0.0"),
        new StringParameterValue("ENVIRONMENT", "staging"))).get(3, TimeUnit.MINUTES);

    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    String taskRunName = "param-build-" + build.getNumber();
    TaskRun taskRun = tektonClient.v1beta1().taskRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName(taskRunName)
        .get();

    assertThat(taskRun).isNotNull();
    assertThat(taskRun.getMetadata().getLabels())
        .containsEntry("app", "test-app")
        .containsEntry("version", "2.0.0")
        .containsEntry("env", "staging");

    assertThat(taskRun.getSpec().getParams()).hasSize(4);
    waitForTaskRunCompletion(taskRunName, getCurrentTestNamespace());

    TaskRun completed = tektonClient.v1beta1().taskRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName(taskRunName)
        .get();

    assertThat(completed.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
    assertThat(completed.getStatus().getConditions().get(0).getStatus()).isEqualTo("True");
  }

  @Test
  public void testBuildFromFileWithJenkinsVars() throws Exception {
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-file-with-vars");

    String shellScript = String.format("""
        #!/bin/bash
        cat > taskrun.yaml << EOF
        apiVersion: tekton.dev/v1beta1
        kind: TaskRun
        metadata:
          name: file-task-$BUILD_NUMBER
          namespace: %s
        spec:
          params:
          - name: workspace-path
            value: "$WORKSPACE"
          - name: build-url
            value: "$BUILD_URL"
          taskSpec:
            params:
            - name: workspace-path
              type: string
            - name: build-url
              type: string
            steps:
            - name: check-workspace
              image: busybox
              command:
              - sh
              - -c
              - |
                echo "Workspace: $(params.workspace-path)"
                echo "Build URL: $(params.build-url)"
                ls -la
                echo "File-based build with Jenkins vars successful!"
        EOF
        """, getCurrentTestNamespace());

    project.getBuildersList().add(new Shell(shellScript));

    CreateRaw createStep = new CreateRaw("taskrun.yaml", "FILE");
    createStep.setNamespace(getCurrentTestNamespace());
    createStep.setClusterName("default");
    project.getBuildersList().add(createStep);

    FreeStyleBuild build = project.scheduleBuild2(0).get(3, TimeUnit.MINUTES);
    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    String taskRunName = "file-task-" + build.getNumber();

    TaskRun taskRun = tektonClient.v1beta1().taskRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName(taskRunName)
        .get();

    assertThat(taskRun).isNotNull();

    boolean workspaceSubstituted = taskRun.getSpec().getParams().stream()
        .anyMatch(p -> "workspace-path".equals(p.getName()) && p.getValue().getStringVal().contains("/workspace"));
    assertThat(workspaceSubstituted).isTrue();

    waitForTaskRunCompletion(taskRunName, getCurrentTestNamespace());

    TaskRun completed = tektonClient.v1beta1().taskRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName(taskRunName)
        .get();

    assertThat(completed.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
    assertThat(completed.getStatus().getConditions().get(0).getStatus()).isEqualTo("True");
  }

  @Test
  public void testPipelineRunWithJenkinsIntegration() throws Exception {
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-pipeline-integration");

    String shellScript = String.format("""
        #!/bin/bash
        cat > pipelinerun.yaml << EOF
        apiVersion: tekton.dev/v1beta1
        kind: PipelineRun
        metadata:
          name: jenkins-pipeline-$BUILD_NUMBER
          namespace: %s
        spec:
          pipelineSpec:
            tasks:
            - name: setup-task
              taskSpec:
                steps:
                - name: setup
                  image: busybox
                  command:
                  - sh
                  - -c
                  - |
                    echo "Setting up Jenkins pipeline"
                    echo "Build number from env: $BUILD_NUMBER"
                    echo "Job name from env: $JOB_NAME"
                    sleep 1
            - name: build-task
              taskSpec:
                steps:
                - name: build
                  image: busybox
                  command:
                  - sh
                  - -c
                  - |
                    echo "Running build task"
                    sleep 2
                    echo "Build completed!"
              runAfter:
              - setup-task
        EOF
        echo "Generated MINIMAL PipelineRun YAML:"
        cat pipelinerun.yaml
        """, getCurrentTestNamespace());

    project.getBuildersList().add(new Shell(shellScript));

    CreateRaw createStep = new CreateRaw("pipelinerun.yaml", "FILE");
    createStep.setNamespace(getCurrentTestNamespace());
    createStep.setClusterName("default");
    project.getBuildersList().add(createStep);

    FreeStyleBuild build = project.scheduleBuild2(0).get(4, TimeUnit.MINUTES);
    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    String pipelineRunName = "jenkins-pipeline-" + build.getNumber();

    PipelineRun pipelineRun = tektonClient.v1beta1().pipelineRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName(pipelineRunName)
        .get();

    assertThat(pipelineRun).isNotNull();

    waitForPipelineRunCompletion(pipelineRunName, getCurrentTestNamespace());

    PipelineRun completed = tektonClient.v1beta1().pipelineRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName(pipelineRunName)
        .get();

    assertThat(completed.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
    assertThat(completed.getStatus().getConditions().get(0).getStatus()).isEqualTo("True");
  }

  // --- Helpers ---
  private void waitForTaskRunCompletion(String name, String ns) throws InterruptedException {
    for (int i = 0; i < 60; i++) {
      TaskRun taskRun = tektonClient.v1beta1().taskRuns().inNamespace(ns).withName(name).get();
      if (taskRun != null && taskRun.getStatus() != null && !taskRun.getStatus().getConditions().isEmpty()) {
        String status = taskRun.getStatus().getConditions().get(0).getStatus();
        if ("True".equals(status) || "False".equals(status))
          return;
      }
      Thread.sleep(5000);
    }
    throw new RuntimeException("TaskRun did not complete within timeout");
  }

  private void waitForPipelineRunCompletion(String name, String ns) throws InterruptedException {
    for (int i = 0; i < 120; i++) {
      PipelineRun run = tektonClient.v1beta1().pipelineRuns().inNamespace(ns).withName(name).get();
      if (run != null && run.getStatus() != null && !run.getStatus().getConditions().isEmpty()) {
        String status = run.getStatus().getConditions().get(0).getStatus();
        if ("True".equals(status) || "False".equals(status))
          return;
      }
      Thread.sleep(5000);
    }
    throw new RuntimeException("PipelineRun did not complete within timeout");
  }
}