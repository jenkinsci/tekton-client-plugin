package org.waveywaves.jenkins.plugins.tekton.client.e2e;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.CreateRaw;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TektonCreatePipelineRunE2ETest extends E2ETestBase {

  @BeforeEach
  public void setUp() throws Exception {
    // This will run after the base class @BeforeEach
  }

  @Test
  public void testCreatePipelineRunFromYAML() throws Exception {
    // Create a test PipelineRun YAML
    String pipelineRunYaml = """
        apiVersion: tekton.dev/v1beta1
        kind: PipelineRun
        metadata:
          name: simple-pipeline-run
          namespace: %s
        spec:
          pipelineSpec:
            tasks:
            - name: hello-task
              taskSpec:
                steps:
                - name: hello
                  image: ubuntu
                  command:
                  - echo
                  args:
                  - "Hello from Pipeline"
            - name: goodbye-task
              taskSpec:
                steps:
                - name: goodbye
                  image: ubuntu
                  command:
                  - echo
                  args:
                  - "Goodbye from Pipeline"
              runAfter:
              - hello-task
        """.formatted(getCurrentTestNamespace());

    // Create Jenkins freestyle project
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-pipelinerun-creation");

    // Add Tekton create step
    CreateRaw createStep = new CreateRaw(pipelineRunYaml, "YAML");
    createStep.setNamespace(getCurrentTestNamespace());
    createStep.setClusterName("default");

    project.getBuildersList().add(createStep);

    // Execute the build
    FreeStyleBuild build = project.scheduleBuild2(0).get(3, TimeUnit.MINUTES);

    // Verify build succeeded
    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    // Log build information for debugging
    System.out.println("Jenkins build completed with result: " + build.getResult());
    System.out.println("Build URL: " + build.getUrl());

    // Verify PipelineRun was created in Kubernetes
    PipelineRun createdPipelineRun = tektonClient.v1beta1().pipelineRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName("simple-pipeline-run")
        .get();

    assertThat(createdPipelineRun).isNotNull();
    assertThat(createdPipelineRun.getMetadata().getName()).isEqualTo("simple-pipeline-run");
    assertThat(createdPipelineRun.getSpec().getPipelineSpec().getTasks()).hasSize(2);

    // Wait a moment for the PipelineRun to start
    Thread.sleep(2000);

    // Wait for PipelineRun to complete and verify it succeeded
    waitForPipelineRunCompletion("simple-pipeline-run", getCurrentTestNamespace());

    PipelineRun completedPipelineRun = tektonClient.v1beta1().pipelineRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName("simple-pipeline-run")
        .get();

    assertThat(completedPipelineRun.getStatus().getConditions()).isNotEmpty();
    assertThat(completedPipelineRun.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
    assertThat(completedPipelineRun.getStatus().getConditions().get(0).getStatus()).isEqualTo("True");
  }

  @Test
  public void testCreatePipelineRunWithParameters() throws Exception {
    // Create a parameterized PipelineRun
    String pipelineRunYaml = """
        apiVersion: tekton.dev/v1beta1
        kind: PipelineRun
        metadata:
          name: parameterized-pipeline-run
          namespace: %s
        spec:
          params:
          - name: message
            value: "Hello from parameterized pipeline"
          - name: name
            value: "Jenkins E2E Test"
          pipelineSpec:
            params:
            - name: message
              type: string
              description: Message to display
            - name: name
              type: string
              description: Name to greet
            tasks:
            - name: greet-task
              params:
              - name: message
                value: $(params.message)
              - name: name
                value: $(params.name)
              taskSpec:
                params:
                - name: message
                  type: string
                - name: name
                  type: string
                steps:
                - name: greet
                  image: ubuntu
                  command:
                  - /bin/bash
                  - -c
                  - |
                    echo "$(params.message)"
                    echo "Hello $(params.name)!"
        """.formatted(getCurrentTestNamespace());

    // Create Jenkins freestyle project
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-parameterized-pipelinerun");

    // Add Tekton create step
    CreateRaw createStep = new CreateRaw(pipelineRunYaml, "YAML");
    createStep.setNamespace(getCurrentTestNamespace());
    createStep.setClusterName("default");

    project.getBuildersList().add(createStep);

    // Execute the build
    FreeStyleBuild build = project.scheduleBuild2(0).get(3, TimeUnit.MINUTES);

    // Verify build succeeded
    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    // Log build information for debugging
    System.out.println("Jenkins build completed with result: " + build.getResult());
    System.out.println("Build URL: " + build.getUrl());

    // Verify PipelineRun was created with correct parameters
    PipelineRun createdPipelineRun = tektonClient.v1beta1().pipelineRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName("parameterized-pipeline-run")
        .get();

    assertThat(createdPipelineRun).isNotNull();

    // Don't assert total param count — Jenkins-specific params (e.g., BUILD_ID,
    // JOB_NAME) are automatically injected.
    // Instead, verify only the custom parameters we care about to keep the test
    // stable.
    // assertThat(createdPipelineRun.getSpec().getParams()).hasSize(2); --> REMOVED

    // Wait a moment for the PipelineRun to start
    Thread.sleep(2000);

    // Find our custom parameter
    boolean messageParamFound = createdPipelineRun.getSpec().getParams().stream()
        .anyMatch(param -> "message".equals(param.getName()));
    assertThat(messageParamFound).isTrue();

    // Get the message parameter
    var messageParam = createdPipelineRun.getSpec().getParams().stream()
        .filter(param -> "message".equals(param.getName()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Message parameter not found"));

    assertThat(messageParam.getValue().getStringVal()).isEqualTo("Hello from parameterized pipeline");
  }

  @Test
  public void testCreatePipelineRunWithWorkspaces() throws Exception {
    // Create a PipelineRun with workspaces
    String pipelineRunYaml = """
        apiVersion: tekton.dev/v1beta1
        kind: PipelineRun
        metadata:
          name: workspace-pipeline-run
          namespace: %s
        spec:
          workspaces:
          - name: shared-data
            emptyDir: {}
          pipelineSpec:
            workspaces:
            - name: shared-data
            tasks:
            - name: write-task
              workspaces:
              - name: shared-data
                workspace: shared-data
              taskSpec:
                workspaces:
                - name: shared-data
                steps:
                - name: write-file
                  image: ubuntu
                  command:
                  - /bin/bash
                  - -c
                  - |
                    echo "Hello from first task" > $(workspaces.shared-data.path)/message.txt
                    echo "File written successfully"
            - name: read-task
              workspaces:
              - name: shared-data
                workspace: shared-data
              taskSpec:
                workspaces:
                - name: shared-data
                steps:
                - name: read-file
                  image: ubuntu
                  command:
                  - /bin/bash
                  - -c
                  - |
                    echo "Reading file from workspace:"
                    cat $(workspaces.shared-data.path)/message.txt
              runAfter:
              - write-task
        """.formatted(getCurrentTestNamespace());

    // Create Jenkins freestyle project
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-workspace-pipelinerun");

    // Add Tekton create step
    CreateRaw createStep = new CreateRaw(pipelineRunYaml, "YAML");
    createStep.setNamespace(getCurrentTestNamespace());
    createStep.setClusterName("default");

    project.getBuildersList().add(createStep);

    // Execute the build
    FreeStyleBuild build = project.scheduleBuild2(0).get(4, TimeUnit.MINUTES);

    // Verify build succeeded
    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    // Log build information for debugging
    System.out.println("Jenkins build completed with result: " + build.getResult());
    System.out.println("Build URL: " + build.getUrl());

    // Verify PipelineRun was created with workspaces
    PipelineRun createdPipelineRun = tektonClient.v1beta1().pipelineRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName("workspace-pipeline-run")
        .get();

    assertThat(createdPipelineRun).isNotNull();
    assertThat(createdPipelineRun.getSpec().getWorkspaces()).hasSize(1);
    assertThat(createdPipelineRun.getSpec().getWorkspaces().get(0).getName()).isEqualTo("shared-data");

    // Wait a moment for the PipelineRun to start
    Thread.sleep(2000);

    // Wait for PipelineRun to complete and verify it succeeded
    waitForPipelineRunCompletion("workspace-pipeline-run", getCurrentTestNamespace());

    PipelineRun completedPipelineRun = tektonClient.v1beta1().pipelineRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName("workspace-pipeline-run")
        .get();

    assertThat(completedPipelineRun.getStatus().getConditions()).isNotEmpty();
    assertThat(completedPipelineRun.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
    assertThat(completedPipelineRun.getStatus().getConditions().get(0).getStatus()).isEqualTo("True");
  }

  @Test
  public void testCreatePipelineRunWithEnvVarsIntegration() throws Exception {
    // Create a PipelineRun that will receive Jenkins environment variables
    String pipelineRunYaml = """
        apiVersion: tekton.dev/v1beta1
        kind: PipelineRun
        metadata:
          name: jenkins-integration-pipeline-run
          namespace: %s
        spec:
          pipelineSpec:
            params:
            - name: BUILD_ID
              type: string
              default: "unknown"
            - name: JOB_NAME
              type: string
              default: "unknown"
            tasks:
            - name: display-jenkins-info
              params:
              - name: BUILD_ID
                value: $(params.BUILD_ID)
              - name: JOB_NAME
                value: $(params.JOB_NAME)
              taskSpec:
                params:
                - name: BUILD_ID
                  type: string
                - name: JOB_NAME
                  type: string
                steps:
                - name: display-info
                  image: ubuntu
                  command:
                  - /bin/bash
                  - -c
                  - |
                    echo "Jenkins Build ID: $(params.BUILD_ID)"
                    echo "Jenkins Job Name: $(params.JOB_NAME)"
                    echo "Integration test successful"
        """.formatted(getCurrentTestNamespace());

    // Create Jenkins freestyle project
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-jenkins-integration");

    // Add Tekton create step
    CreateRaw createStep = new CreateRaw(pipelineRunYaml, "YAML");
    createStep.setNamespace(getCurrentTestNamespace());
    createStep.setClusterName("default");

    project.getBuildersList().add(createStep);

    // Execute the build
    FreeStyleBuild build = project.scheduleBuild2(0).get(3, TimeUnit.MINUTES);

    // Verify build succeeded
    assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    // Log build information for debugging
    System.out.println("Jenkins build completed with result: " + build.getResult());
    System.out.println("Build URL: " + build.getUrl());

    // Verify PipelineRun was created and contains Jenkins environment variables
    PipelineRun createdPipelineRun = tektonClient.v1beta1().pipelineRuns()
        .inNamespace(getCurrentTestNamespace())
        .withName("jenkins-integration-pipeline-run")
        .get();

    assertThat(createdPipelineRun).isNotNull();

    // Wait a moment for the PipelineRun to start
    Thread.sleep(2000);

    // The plugin should automatically inject BUILD_ID and JOB_NAME parameters
    assertThat(createdPipelineRun.getSpec().getParams()).isNotEmpty();

    boolean buildIdFound = createdPipelineRun.getSpec().getParams().stream()
        .anyMatch(param -> "BUILD_ID".equals(param.getName()));
    boolean jobNameFound = createdPipelineRun.getSpec().getParams().stream()
        .anyMatch(param -> "JOB_NAME".equals(param.getName()));

    assertThat(buildIdFound).isTrue();
    assertThat(jobNameFound).isTrue();
  }

  private void waitForPipelineRunCompletion(String pipelineRunName, String namespace) throws InterruptedException {
    for (int i = 0; i < 120; i++) { // Wait up to 10 minutes for pipelines
      PipelineRun pipelineRun = tektonClient.v1beta1().pipelineRuns()
          .inNamespace(namespace)
          .withName(pipelineRunName)
          .get();

      if (pipelineRun != null &&
          pipelineRun.getStatus() != null &&
          pipelineRun.getStatus().getConditions() != null &&
          !pipelineRun.getStatus().getConditions().isEmpty()) {

        String status = pipelineRun.getStatus().getConditions().get(0).getStatus();
        String type = pipelineRun.getStatus().getConditions().get(0).getType();

        if ("True".equals(status) || "False".equals(status)) {
          // Log the final status for debugging
          System.out.println("PipelineRun " + pipelineRunName + " completed with status: " + type + " = " + status);
          return; // PipelineRun completed (either success or failure)
        }
      }

      // Log progress every 30 seconds
      if (i % 6 == 0) {
        System.out
            .println("Waiting for PipelineRun " + pipelineRunName + " to complete... (attempt " + (i + 1) + "/120)");
      }

      Thread.sleep(5000); // Wait 5 seconds before checking again
    }

    // Get the final state for better error reporting
    PipelineRun finalPipelineRun = tektonClient.v1beta1().pipelineRuns()
        .inNamespace(namespace)
        .withName(pipelineRunName)
        .get();

    String errorMessage = "PipelineRun " + pipelineRunName + " did not complete within timeout";
    if (finalPipelineRun != null && finalPipelineRun.getStatus() != null) {
      errorMessage += ". Final status: " + finalPipelineRun.getStatus().getConditions();
    }

    throw new RuntimeException(errorMessage);
  }
}