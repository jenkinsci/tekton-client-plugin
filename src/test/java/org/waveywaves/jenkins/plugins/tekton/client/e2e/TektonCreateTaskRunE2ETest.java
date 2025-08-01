package org.waveywaves.jenkins.plugins.tekton.client.e2e;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.CreateRaw;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TektonCreateTaskRunE2ETest extends E2ETestBase {

    @BeforeEach
    public void setUp() throws Exception {
        // This will run after the base class @BeforeEach
    }

    @Test
    public void testCreateTaskRunFromYAML() throws Exception {
        // Create a simple TaskRun YAML
        String taskRunYaml = """
                apiVersion: tekton.dev/v1beta1
                kind: TaskRun
                metadata:
                  name: echo-hello-world
                  namespace: %s
                spec:
                  taskSpec:
                    steps:
                    - name: echo
                      image: ubuntu
                      command:
                      - echo
                      - "Hello World"
                """.formatted(getCurrentTestNamespace());

        // Create Jenkins freestyle project
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-taskrun-yaml");

        // Add Tekton create step
        CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
        createStep.setNamespace(getCurrentTestNamespace());
        createStep.setClusterName("default");

        project.getBuildersList().add(createStep);

        // Execute the build
        FreeStyleBuild build = project.scheduleBuild2(0).get(2, TimeUnit.MINUTES);

        // Verify build succeeded
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

        // Log build information for debugging
        System.out.println("Jenkins build completed with result: " + build.getResult());
        System.out.println("Build URL: " + build.getUrl());

        // Verify TaskRun was created in Kubernetes
        TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
                .inNamespace(getCurrentTestNamespace())
                .withName("echo-hello-world")
                .get();

        assertThat(createdTaskRun).isNotNull();
        assertThat(createdTaskRun.getMetadata().getName()).isEqualTo("echo-hello-world");
        assertThat(createdTaskRun.getSpec().getTaskSpec().getSteps()).hasSize(1);
        assertThat(createdTaskRun.getSpec().getTaskSpec().getSteps().get(0).getImage()).isEqualTo("ubuntu");

        // Wait a moment for the TaskRun to start
        Thread.sleep(2000);

        // Wait for TaskRun to complete and verify it succeeded
        waitForTaskRunCompletion("echo-hello-world", getCurrentTestNamespace());

        TaskRun completedTaskRun = tektonClient.v1beta1().taskRuns()
                .inNamespace(getCurrentTestNamespace())
                .withName("echo-hello-world")
                .get();

        assertThat(completedTaskRun.getStatus().getConditions()).isNotEmpty();
        assertThat(completedTaskRun.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
        assertThat(completedTaskRun.getStatus().getConditions().get(0).getStatus()).isEqualTo("True");
    }

    @Test
    public void testCreateTaskRunFromFile() throws Exception {
        // Create a test TaskRun file in workspace
        String taskRunYaml = """
                apiVersion: tekton.dev/v1beta1
                kind: TaskRun
                metadata:
                  name: file-based-taskrun
                  namespace: %s
                spec:
                  taskSpec:
                    steps:
                    - name: check-file
                      image: busybox
                      command:
                      - sh
                      - -c
                      - "ls -la && echo 'File test successful'"
                """.formatted(getCurrentTestNamespace());

        // Create Jenkins freestyle project
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-taskrun-from-file");

        // Add build step to create the YAML file
        project.getBuildersList().add(new hudson.tasks.Shell("echo '" + taskRunYaml + "' > taskrun.yaml"));

        // Add Tekton create step
        CreateRaw createStep = new CreateRaw("taskrun.yaml", "FILE");
        createStep.setNamespace(getCurrentTestNamespace());
        createStep.setClusterName("default");

        project.getBuildersList().add(createStep);

        // Execute the build
        FreeStyleBuild build = project.scheduleBuild2(0).get(2, TimeUnit.MINUTES);

        // Verify build succeeded
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

        // Log build information for debugging
        System.out.println("Jenkins build completed with result: " + build.getResult());
        System.out.println("Build URL: " + build.getUrl());

        // Verify TaskRun was created
        TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
                .inNamespace(getCurrentTestNamespace())
                .withName("file-based-taskrun")
                .get();

        assertThat(createdTaskRun).isNotNull();
        assertThat(createdTaskRun.getMetadata().getName()).isEqualTo("file-based-taskrun");

        // Wait a moment for the TaskRun to start
        Thread.sleep(2000);

        // Wait for TaskRun to complete and verify it succeeded
        waitForTaskRunCompletion("file-based-taskrun", getCurrentTestNamespace());

        TaskRun completedTaskRun = tektonClient.v1beta1().taskRuns()
                .inNamespace(getCurrentTestNamespace())
                .withName("file-based-taskrun")
                .get();

        assertThat(completedTaskRun.getStatus().getConditions()).isNotEmpty();
        assertThat(completedTaskRun.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
        assertThat(completedTaskRun.getStatus().getConditions().get(0).getStatus()).isEqualTo("True");
    }

    @Test
    public void testCreateTaskRunWithParameters() throws Exception {
        // Create a TaskRun with parameters
        String taskRunYaml = """
                apiVersion: tekton.dev/v1beta1
                kind: TaskRun
                metadata:
                  name: parameterized-taskrun
                  namespace: %s
                spec:
                  params:
                  - name: message
                    value: "Hello from Jenkins E2E test"
                  - name: count
                    value: "3"
                  taskSpec:
                    params:
                    - name: message
                      type: string
                    - name: count
                      type: string
                    steps:
                    - name: repeat-message
                      image: ubuntu
                      command:
                      - /bin/bash
                      - -c
                      - |
                        for i in $(seq 1 $(params.count)); do
                          echo "$(params.message) - iteration $i"
                        done
                """.formatted(getCurrentTestNamespace());

        // Create Jenkins freestyle project
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-parameterized-taskrun");

        // Add Tekton create step
        CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
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

        // Verify TaskRun was created with correct parameters
        TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
                .inNamespace(getCurrentTestNamespace())
                .withName("parameterized-taskrun")
                .get();

        assertThat(createdTaskRun).isNotNull();

        // Wait a moment for the TaskRun to start
        Thread.sleep(2000);

        // Find our custom parameters
        boolean messageParamFound = createdTaskRun.getSpec().getParams().stream()
                .anyMatch(param -> "message".equals(param.getName()));
        boolean countParamFound = createdTaskRun.getSpec().getParams().stream()
                .anyMatch(param -> "count".equals(param.getName()));

        assertThat(messageParamFound).isTrue();
        assertThat(countParamFound).isTrue();

        // Get the message parameter
        var messageParam = createdTaskRun.getSpec().getParams().stream()
                .filter(param -> "message".equals(param.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Message parameter not found"));

        assertThat(messageParam.getValue().getStringVal()).isEqualTo("Hello from Jenkins E2E test");

        // Wait for TaskRun to complete and verify it succeeded
        waitForTaskRunCompletion("parameterized-taskrun", getCurrentTestNamespace());

        TaskRun completedTaskRun = tektonClient.v1beta1().taskRuns()
                .inNamespace(getCurrentTestNamespace())
                .withName("parameterized-taskrun")
                .get();

        assertThat(completedTaskRun.getStatus().getConditions()).isNotEmpty();
        assertThat(completedTaskRun.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
        assertThat(completedTaskRun.getStatus().getConditions().get(0).getStatus()).isEqualTo("True");
    }

    // @Test
    // public void testCreateTaskRunFailure() throws Exception {
    // // Create a TaskRun that will fail
    // String taskRunYaml = """
    // apiVersion: tekton.dev/v1beta1
    // kind: TaskRun
    // metadata:
    // name: failing-taskrun
    // namespace: %s
    // spec:
    // taskSpec:
    // steps:
    // - name: fail-step
    // image: busybox
    // command:
    // - sh
    // - -c
    // - "echo 'This will fail' && exit 1"
    // """.formatted(getCurrentTestNamespace());

    // // Create Jenkins freestyle project
    // FreeStyleProject project =
    // jenkinsRule.createFreeStyleProject("test-failing-taskrun");

    // // Add Tekton create step
    // CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
    // createStep.setNamespace(getCurrentTestNamespace());
    // createStep.setClusterName("default");

    // project.getBuildersList().add(createStep);

    // // Execute the build - this should fail
    // FreeStyleBuild build = project.scheduleBuild2(0).get(3, TimeUnit.MINUTES);

    // // Verify build failed due to TaskRun failure

    // // Jenkins build is SUCCESS because the CreateRaw step only needs to apply
    // the YAML successfully;
    // // it doesn’t wait for the TaskRun to finish. We assert the TaskRun’s own
    // failure later.
    // assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

    // // Verify TaskRun was created but failed
    // TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
    // .inNamespace(getCurrentTestNamespace())
    // .withName("failing-taskrun")
    // .get();

    // assertThat(createdTaskRun).isNotNull();

    // // Wait for TaskRun to complete with failure
    // waitForTaskRunCompletion("failing-taskrun", getCurrentTestNamespace());

    // TaskRun completedTaskRun = tektonClient.v1beta1().taskRuns()
    // .inNamespace(getCurrentTestNamespace())
    // .withName("failing-taskrun")
    // .get();

    // assertThat(completedTaskRun.getStatus().getConditions()).isNotEmpty();
    // assertThat(completedTaskRun.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
    // assertThat(completedTaskRun.getStatus().getConditions().get(0).getStatus()).isEqualTo("False");
    // }

    @Test
    public void testCreateTaskRunFailure() throws Exception {
        // Create a TaskRun that will fail
        String taskRunYaml = """
                apiVersion: tekton.dev/v1beta1
                kind: TaskRun
                metadata:
                  name: failing-taskrun
                  namespace: %s
                spec:
                  taskSpec:
                    steps:
                    - name: fail-step
                      image: busybox
                      command:
                      - sh
                      - -c
                      - "echo 'This will fail' && exit 1"
                """.formatted(getCurrentTestNamespace());

        // Create Jenkins freestyle project
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-failing-taskrun");

        // Add Tekton create step
        CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
        createStep.setNamespace(getCurrentTestNamespace());
        createStep.setClusterName("default");

        project.getBuildersList().add(createStep);

        // Execute the build - this should fail
        FreeStyleBuild build = project.scheduleBuild2(0).get(3, TimeUnit.MINUTES);

        // Verify build failed due to TaskRun failure

        // Jenkins build is SUCCESS because the CreateRaw step only needs to apply the
        // YAML successfully;
        // it doesn't wait for the TaskRun to finish. We assert the TaskRun's own
        // failure later.
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

        // Log build information for debugging
        System.out.println("Jenkins build completed with result: " + build.getResult());
        System.out.println("Build URL: " + build.getUrl());

        // Verify TaskRun was created but failed
        TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
                .inNamespace(getCurrentTestNamespace())
                .withName("failing-taskrun")
                .get();

        assertThat(createdTaskRun).isNotNull();

        // Wait a moment for the TaskRun to start
        Thread.sleep(2000);

        // Wait for TaskRun to complete with failure
        waitForTaskRunCompletion("failing-taskrun", getCurrentTestNamespace());

        TaskRun completedTaskRun = tektonClient.v1beta1().taskRuns()
                .inNamespace(getCurrentTestNamespace())
                .withName("failing-taskrun")
                .get();

        assertThat(completedTaskRun.getStatus().getConditions()).isNotEmpty();
        assertThat(completedTaskRun.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
        assertThat(completedTaskRun.getStatus().getConditions().get(0).getStatus()).isEqualTo("False");
    }

    private void waitForTaskRunCompletion(String taskRunName, String namespace) throws InterruptedException {
        for (int i = 0; i < 60; i++) { // Wait up to 5 minutes
            TaskRun taskRun = tektonClient.v1beta1().taskRuns()
                    .inNamespace(namespace)
                    .withName(taskRunName)
                    .get();

            if (taskRun != null &&
                    taskRun.getStatus() != null &&
                    taskRun.getStatus().getConditions() != null &&
                    !taskRun.getStatus().getConditions().isEmpty()) {

                String status = taskRun.getStatus().getConditions().get(0).getStatus();
                String type = taskRun.getStatus().getConditions().get(0).getType();

                if ("True".equals(status) || "False".equals(status)) {
                    // Log the final status for debugging
                    System.out.println("TaskRun " + taskRunName + " completed with status: " + type + " = " + status);
                    return; // TaskRun completed (either success or failure)
                }
            }

            // Log progress every 30 seconds
            if (i % 6 == 0) {
                System.out
                        .println("Waiting for TaskRun " + taskRunName + " to complete... (attempt " + (i + 1) + "/60)");
            }

            Thread.sleep(5000); // Wait 5 seconds before checking again
        }

        // Get the final state for better error reporting
        TaskRun finalTaskRun = tektonClient.v1beta1().taskRuns()
                .inNamespace(namespace)
                .withName(taskRunName)
                .get();

        String errorMessage = "TaskRun " + taskRunName + " did not complete within timeout";
        if (finalTaskRun != null && finalTaskRun.getStatus() != null) {
            errorMessage += ". Final status: " + finalTaskRun.getStatus().getConditions();
        }

        throw new RuntimeException(errorMessage);
    }
}