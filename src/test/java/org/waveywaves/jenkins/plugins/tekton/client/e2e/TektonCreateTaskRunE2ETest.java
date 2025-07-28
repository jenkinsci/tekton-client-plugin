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

//     @BeforeEach
//     public void setUp() throws Exception {
//         // This will run after the base class @BeforeEach
//     }

//     @Test
//     public void testCreateTaskRunFromYAML() throws Exception {
//         // Create a simple TaskRun YAML
//         String taskRunYaml = """
// apiVersion: tekton.dev/v1beta1
// kind: TaskRun
// metadata:
//   name: echo-hello-world
//   namespace: %s
// spec:
//   taskSpec:
//     steps:
//     - name: echo
//       image: ubuntu
//       command:
//       - echo
//       - "Hello World"
// """.formatted(getCurrentTestNamespace());

//         // Create Jenkins freestyle project
//         FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-taskrun-yaml");
        
//         // Add Tekton create step
//         CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
//         createStep.setNamespace(getCurrentTestNamespace());
//         createStep.setClusterName("default");
        
//         project.getBuildersList().add(createStep);

//         // Execute the build
//         FreeStyleBuild build = project.scheduleBuild2(0).get(2, TimeUnit.MINUTES);
        
//         // Verify build succeeded
//         assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        
//         // Verify TaskRun was created in Kubernetes
//         TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
//                 .inNamespace(getCurrentTestNamespace())
//                 .withName("echo-hello-world")
//                 .get();
        
//         assertThat(createdTaskRun).isNotNull();
//         assertThat(createdTaskRun.getMetadata().getName()).isEqualTo("echo-hello-world");
//         assertThat(createdTaskRun.getSpec().getTaskSpec().getSteps()).hasSize(1);
//         assertThat(createdTaskRun.getSpec().getTaskSpec().getSteps().get(0).getImage()).isEqualTo("ubuntu");
        
//         // Wait for TaskRun to complete and verify it succeeded
//         waitForTaskRunCompletion("echo-hello-world", getCurrentTestNamespace());
        
//         TaskRun completedTaskRun = tektonClient.v1beta1().taskRuns()
//                 .inNamespace(getCurrentTestNamespace())
//                 .withName("echo-hello-world")
//                 .get();
        
//         assertThat(completedTaskRun.getStatus().getConditions()).isNotEmpty();
//         assertThat(completedTaskRun.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
//         assertThat(completedTaskRun.getStatus().getConditions().get(0).getStatus()).isEqualTo("True");
//     }

//     @Test
//     public void testCreateTaskRunFromFile() throws Exception {
//         // Create a test TaskRun file in workspace
//         String taskRunYaml = """
// apiVersion: tekton.dev/v1beta1
// kind: TaskRun
// metadata:
//   name: file-based-taskrun
//   namespace: %s
// spec:
//   taskSpec:
//     steps:
//     - name: check-file
//       image: busybox
//       command:
//       - sh
//       - -c
//       - "ls -la && echo 'File test successful'"
// """.formatted(getCurrentTestNamespace());

//         // Create Jenkins freestyle project
//         FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-taskrun-from-file");
        
//         // Add build step to create the YAML file
//         project.getBuildersList().add(new hudson.tasks.Shell("echo '" + taskRunYaml + "' > taskrun.yaml"));
        
//         // Add Tekton create step
//         CreateRaw createStep = new CreateRaw("taskrun.yaml", "FILE");
//         createStep.setNamespace(getCurrentTestNamespace());
//         createStep.setClusterName("default");
        
//         project.getBuildersList().add(createStep);

//         // Execute the build
//         FreeStyleBuild build = project.scheduleBuild2(0).get(2, TimeUnit.MINUTES);
        
//         // Verify build succeeded
//         assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        
//         // Verify TaskRun was created
//         TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
//                 .inNamespace(getCurrentTestNamespace())
//                 .withName("file-based-taskrun")
//                 .get();
        
//         assertThat(createdTaskRun).isNotNull();
//         assertThat(createdTaskRun.getMetadata().getName()).isEqualTo("file-based-taskrun");
//     }

//     @Test
//     public void testCreateTaskRunWithParameters() throws Exception {
//         // Create a TaskRun with parameters
//         String taskRunYaml = """
// apiVersion: tekton.dev/v1beta1
// kind: TaskRun
// metadata:
//   name: parameterized-taskrun
//   namespace: %s
// spec:
//   params:
//   - name: message
//     value: "Hello from Jenkins E2E test"
//   - name: count
//     value: "3"
//   taskSpec:
//     params:
//     - name: message
//       type: string
//     - name: count
//       type: string
//     steps:
//     - name: repeat-message
//       image: bash
//       script: |
//         for i in $(seq 1 $(params.count)); do
//           echo "$(params.message) - iteration $i"
//         done
// """.formatted(getCurrentTestNamespace());

//         // Create Jenkins freestyle project
//         FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-parameterized-taskrun");
        
//         // Add Tekton create step
//         CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
//         createStep.setNamespace(getCurrentTestNamespace());
//         createStep.setClusterName("default");
        
//         project.getBuildersList().add(createStep);

//         // Execute the build
//         FreeStyleBuild build = project.scheduleBuild2(0).get(3, TimeUnit.MINUTES);
        
//         // Verify build succeeded
//         assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        
//         // Verify TaskRun was created with correct parameters
//         TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
//                 .inNamespace(getCurrentTestNamespace())
//                 .withName("parameterized-taskrun")
//                 .get();
        
//         assertThat(createdTaskRun).isNotNull();
//         assertThat(createdTaskRun.getSpec().getParams()).hasSize(2);
//         assertThat(createdTaskRun.getSpec().getParams().get(0).getName()).isEqualTo("message");
//         assertThat(createdTaskRun.getSpec().getParams().get(0).getValue().getStringVal()).isEqualTo("Hello from Jenkins E2E test");
//     }

//     @Test
//     public void testCreateTaskRunFailure() throws Exception {
//         // Create a TaskRun that will fail
//         String taskRunYaml = """
// apiVersion: tekton.dev/v1beta1
// kind: TaskRun
// metadata:
//   name: failing-taskrun
//   namespace: %s
// spec:
//   taskSpec:
//     steps:
//     - name: fail-step
//       image: busybox
//       command:
//       - sh
//       - -c
//       - "echo 'This will fail' && exit 1"
// """.formatted(getCurrentTestNamespace());

//         // Create Jenkins freestyle project
//         FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-failing-taskrun");
        
//         // Add Tekton create step
//         CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
//         createStep.setNamespace(getCurrentTestNamespace());
//         createStep.setClusterName("default");
        
//         project.getBuildersList().add(createStep);

//         // Execute the build - this should fail
//         FreeStyleBuild build = project.scheduleBuild2(0).get(3, TimeUnit.MINUTES);
        
//         // Verify build failed due to TaskRun failure

//         // Jenkins build is SUCCESS because the CreateRaw step only needs to apply the YAML successfully;
//         // it doesn’t wait for the TaskRun to finish. We assert the TaskRun’s own failure later.
//         assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        
//         // Verify TaskRun was created but failed
//         TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
//                 .inNamespace(getCurrentTestNamespace())
//                 .withName("failing-taskrun")
//                 .get();
        
//         assertThat(createdTaskRun).isNotNull();
        
//         // Wait for TaskRun to complete with failure
//         waitForTaskRunCompletion("failing-taskrun", getCurrentTestNamespace());
        
//         TaskRun completedTaskRun = tektonClient.v1beta1().taskRuns()
//                 .inNamespace(getCurrentTestNamespace())
//                 .withName("failing-taskrun")
//                 .get();
        
//         assertThat(completedTaskRun.getStatus().getConditions()).isNotEmpty();
//         assertThat(completedTaskRun.getStatus().getConditions().get(0).getType()).isEqualTo("Succeeded");
//         assertThat(completedTaskRun.getStatus().getConditions().get(0).getStatus()).isEqualTo("False");
//     }

//     private void waitForTaskRunCompletion(String taskRunName, String namespace) throws InterruptedException {
//         for (int i = 0; i < 60; i++) { // Wait up to 5 minutes
//             TaskRun taskRun = tektonClient.v1beta1().taskRuns()
//                     .inNamespace(namespace)
//                     .withName(taskRunName)
//                     .get();
            
//             if (taskRun != null && 
//                 taskRun.getStatus() != null && 
//                 taskRun.getStatus().getConditions() != null &&
//                 !taskRun.getStatus().getConditions().isEmpty()) {
                
//                 String status = taskRun.getStatus().getConditions().get(0).getStatus();
//                 if ("True".equals(status) || "False".equals(status)) {
//                     return; // TaskRun completed (either success or failure)
//                 }
//             }
            
//             Thread.sleep(5000); // Wait 5 seconds before checking again
//         }
        
//         throw new RuntimeException("TaskRun did not complete within timeout");
//     }
}
