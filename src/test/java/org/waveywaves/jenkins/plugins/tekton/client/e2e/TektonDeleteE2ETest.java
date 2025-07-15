package org.waveywaves.jenkins.plugins.tekton.client.e2e;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.CreateRaw;
import org.waveywaves.jenkins.plugins.tekton.client.build.delete.DeleteRaw;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TektonDeleteE2ETest extends E2ETestBase {

    @BeforeEach
    public void setUp() throws Exception {
        // This will run after the base class @BeforeEach
    }

//     @Test
//     public void testDeleteSpecificTaskRun() throws Exception {
//         System.out.println("=== START: testDeleteSpecificTaskRun ===");
        
//         String currentNamespace = getCurrentTestNamespace();
//         System.out.println("Using namespace: " + currentNamespace);
        
//         // First create a TaskRun
//         String taskRunYaml = """
// apiVersion: tekton.dev/v1beta1
// kind: TaskRun
// metadata:
//   name: taskrun-to-delete
//   namespace: %s
// spec:
//   taskSpec:
//     steps:
//     - name: echo
//       image: ubuntu
//       command:
//       - echo
//       args:
//       - "I will be deleted"
// """.formatted(currentNamespace);

//         System.out.println("TaskRun YAML to create:\n" + taskRunYaml);

//         // Create Jenkins freestyle project for creation
//         System.out.println("Creating Jenkins freestyle project for TaskRun creation...");
//         FreeStyleProject createProject = jenkinsRule.createFreeStyleProject("test-create-for-delete");
//         System.out.println("Created project: " + createProject.getName());
        
//         CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
//         createStep.setNamespace(currentNamespace);
//         createStep.setClusterName("");
//         System.out.println("CreateRaw step configured - namespace: " + createStep.getNamespace() + 
//                    ", clusterName: '" + createStep.getClusterName() + "'");
        
//         createProject.getBuildersList().add(createStep);
//         System.out.println("Added CreateRaw step to project");

//         // Execute the creation build
//         System.out.println("Scheduling creation build...");
//         FreeStyleBuild createBuild = createProject.scheduleBuild2(0).get(2, TimeUnit.MINUTES);
//         System.out.println("Creation build completed with result: " + createBuild.getResult());
        
//         // Print build log for debugging
//         if (createBuild.getResult() != Result.SUCCESS) {
//             System.err.println("Creation build failed! Build log:");
//             System.err.println(createBuild.getLog(100).toString());
//         } else {
//             System.out.println("Creation build log (last 20 lines):");
//             createBuild.getLog(20).forEach(System.out::println);
//         }
        
//         assertThat(createBuild.getResult()).isEqualTo(Result.SUCCESS);
        
//         // Verify TaskRun was created
//         System.out.println("Verifying TaskRun was created...");
//         TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
//                 .inNamespace(currentNamespace)
//                 .withName("taskrun-to-delete")
//                 .get();
        
//         if (createdTaskRun != null) {
//             System.out.println("TaskRun successfully created:");
//             System.out.println("  Name: " + createdTaskRun.getMetadata().getName());
//             System.out.println("  Namespace: " + createdTaskRun.getMetadata().getNamespace());
//             System.out.println("  Status: " + (createdTaskRun.getStatus() != null ? 
//                        createdTaskRun.getStatus().getConditions() : "No status"));
//         } else {
//             System.err.println("TaskRun was NOT created!");
            
//             // List all TaskRuns in namespace for debugging
//             System.out.println("All TaskRuns in namespace " + currentNamespace + ":");
//             tektonClient.v1beta1().taskRuns().inNamespace(currentNamespace).list()
//                     .getItems().forEach(tr -> 
//                         System.out.println("  - " + tr.getMetadata().getName()));
//         }
        
//         assertThat(createdTaskRun).isNotNull();

//         // Now create a project to delete the TaskRun
//         System.out.println("Creating Jenkins freestyle project for TaskRun deletion...");
//         FreeStyleProject deleteProject = jenkinsRule.createFreeStyleProject("test-delete-taskrun");
//         System.out.println("Created delete project: " + deleteProject.getName());
        
//         DeleteRaw deleteStep = new DeleteRaw("taskrun", "kind-cluster", 
//                 new DeleteRaw.DeleteAllBlock("taskrun-to-delete"));
//         deleteStep.setClusterName("kind-cluster");
//         System.out.println("DeleteRaw step configured - resourceType: taskrun, clusterName: kind-cluster, " +
//                    "resourceName: taskrun-to-delete");
        
//         deleteProject.getBuildersList().add(deleteStep);
//         System.out.println("Added DeleteRaw step to project");

//         // Execute the deletion build
//         System.out.println("Scheduling deletion build...");
//         FreeStyleBuild deleteBuild = deleteProject.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
//         System.out.println("Deletion build completed with result: " + deleteBuild.getResult());
        
//         // Print build log for debugging
//         if (deleteBuild.getResult() != Result.SUCCESS) {
//             System.err.println("Deletion build failed! Build log:");
//             System.err.println(deleteBuild.getLog(100).toString());
//         } else {
//             System.out.println("Deletion build log (last 20 lines):");
//             deleteBuild.getLog(20).forEach(System.out::println);
//         }
        
//         assertThat(deleteBuild.getResult()).isEqualTo(Result.SUCCESS);
        
//         // Verify TaskRun was deleted
//         System.out.println("Verifying TaskRun was deleted...");
        
//         // Wait a bit for deletion to propagate
//         Thread.sleep(2000);
        
//         TaskRun deletedTaskRun = tektonClient.v1beta1().taskRuns()
//                 .inNamespace(currentNamespace)
//                 .withName("taskrun-to-delete")
//                 .get();
        
//         if (deletedTaskRun == null) {
//             System.out.println("TaskRun successfully deleted!");
//         } else {
//             System.err.println("TaskRun still exists after deletion:");
//             System.err.println("  Name: " + deletedTaskRun.getMetadata().getName());
//             System.err.println("  Namespace: " + deletedTaskRun.getMetadata().getNamespace());
//             System.err.println("  DeletionTimestamp: " + deletedTaskRun.getMetadata().getDeletionTimestamp());
//             System.err.println("  Finalizers: " + deletedTaskRun.getMetadata().getFinalizers());
            
//             // List all TaskRuns in namespace for debugging
//             System.out.println("All TaskRuns in namespace " + currentNamespace + " after deletion:");
//             tektonClient.v1beta1().taskRuns().inNamespace(currentNamespace).list()
//                     .getItems().forEach(tr -> 
//                         System.out.println("  - " + tr.getMetadata().getName() + 
//                                    " (DeletionTimestamp: " + tr.getMetadata().getDeletionTimestamp() + ")"));
//         }
        
//         assertThat(deletedTaskRun).isNull();
        
//         System.out.println("=== END: testDeleteSpecificTaskRun - SUCCESS ===");
//     }

//     @Test
//     public void testDeleteSpecificTask() throws Exception {
//         // First create a Task
//         String taskYaml = """
// apiVersion: tekton.dev/v1beta1
// kind: Task
// metadata:
//   name: task-to-delete
//   namespace: %s
// spec:
//   steps:
//   - name: echo
//     image: ubuntu
//     command:
//     - echo
//     args:
//     - "I am a task to be deleted"
// """.formatted(getCurrentTestNamespace());

//         // Create Jenkins freestyle project for creation
//         FreeStyleProject createProject = jenkinsRule.createFreeStyleProject("test-create-task-for-delete");
        
//         CreateRaw createStep = new CreateRaw(taskYaml, "YAML");
//         createStep.setNamespace(getCurrentTestNamespace());
//         createStep.setClusterName("default");
        
//         createProject.getBuildersList().add(createStep);

//         // Execute the creation build
//         FreeStyleBuild createBuild = createProject.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
//         assertThat(createBuild.getResult()).isEqualTo(Result.SUCCESS);
        
//         // Verify Task was created
//         Task createdTask = tektonClient.v1beta1().tasks()
//                 .inNamespace(getCurrentTestNamespace())
//                 .withName("task-to-delete")
//                 .get();
//         assertThat(createdTask).isNotNull();

//         // Now create a project to delete the Task
//         FreeStyleProject deleteProject = jenkinsRule.createFreeStyleProject("test-delete-task");
        
//         DeleteRaw deleteStep = new DeleteRaw("task", "kind-cluster", 
//                 new DeleteRaw.DeleteAllBlock("task-to-delete"));
//         deleteStep.setClusterName("kind-cluster");
        
//         deleteProject.getBuildersList().add(deleteStep);

//         // Execute the deletion build
//         FreeStyleBuild deleteBuild = deleteProject.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
//         assertThat(deleteBuild.getResult()).isEqualTo(Result.SUCCESS);
        
//         // Verify Task was deleted
//         Task deletedTask = tektonClient.v1beta1().tasks()
//                 .inNamespace(getCurrentTestNamespace())
//                 .withName("task-to-delete")
//                 .get();
//         assertThat(deletedTask).isNull();
//     }

//     @Test
//     public void testDeleteAllTaskRuns() throws Exception {
//         // First create multiple TaskRuns
//         String[] taskRunNames = {"taskrun-1", "taskrun-2", "taskrun-3"};
        
//         for (String taskRunName : taskRunNames) {
//             String taskRunYaml = """
// apiVersion: tekton.dev/v1beta1
// kind: TaskRun
// metadata:
//   name: %s
//   namespace: %s
// spec:
//   taskSpec:
//     steps:
//     - name: echo
//       image: ubuntu
//       command:
//       - echo
//       args:
//       - "TaskRun %s"
// """.formatted(taskRunName, getCurrentTestNamespace(), taskRunName);

//             // Create Jenkins freestyle project for creation
//             FreeStyleProject createProject = jenkinsRule.createFreeStyleProject("test-create-" + taskRunName);
            
//             CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
//             createStep.setNamespace(getCurrentTestNamespace());
//             createStep.setClusterName("default");
            
//             createProject.getBuildersList().add(createStep);

//             // Execute the creation build
//             FreeStyleBuild createBuild = createProject.scheduleBuild2(0).get(2, TimeUnit.MINUTES);
//             assertThat(createBuild.getResult()).isEqualTo(Result.SUCCESS);
//         }
        
//         // Verify all TaskRuns were created
//         for (String taskRunName : taskRunNames) {
//             TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
//                     .inNamespace(getCurrentTestNamespace())
//                     .withName(taskRunName)
//                     .get();
//             assertThat(createdTaskRun).isNotNull();
//         }

//         // Now create a project to delete all TaskRuns (null name means delete all)
//         FreeStyleProject deleteProject = jenkinsRule.createFreeStyleProject("test-delete-all-taskruns");
        
//         DeleteRaw deleteStep = new DeleteRaw("taskrun", "kind-cluster", 
//                 new DeleteRaw.DeleteAllBlock(null)); // null means delete all
//         deleteStep.setClusterName("kind-cluster");
        
//         deleteProject.getBuildersList().add(deleteStep);

//         // Execute the deletion build
//         FreeStyleBuild deleteBuild = deleteProject.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
//         assertThat(deleteBuild.getResult()).isEqualTo(Result.SUCCESS);
        
//         // Verify all TaskRuns were deleted
//         for (String taskRunName : taskRunNames) {
//             TaskRun deletedTaskRun = tektonClient.v1beta1().taskRuns()
//                     .inNamespace(getCurrentTestNamespace())
//                     .withName(taskRunName)
//                     .get();
//             assertThat(deletedTaskRun).isNull();
//         }
//     }

//     @Test
//     public void testDeleteNonExistentResource() throws Exception {
//         // Try to delete a TaskRun that doesn't exist
//         FreeStyleProject deleteProject = jenkinsRule.createFreeStyleProject("test-delete-nonexistent");
        
//         DeleteRaw deleteStep = new DeleteRaw("taskrun", "kind-cluster", 
//                 new DeleteRaw.DeleteAllBlock("nonexistent-taskrun"));
//         deleteStep.setClusterName("kind-cluster");
        
//         deleteProject.getBuildersList().add(deleteStep);

//         // Execute the deletion build - should still succeed (idempotent)
//         FreeStyleBuild deleteBuild = deleteProject.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
//         assertThat(deleteBuild.getResult()).isEqualTo(Result.SUCCESS);
//     }

//     @Test
//     public void testCreateAndDeleteWorkflow() throws Exception {
//         // Test a complete workflow: create multiple resources, then delete them
//         String taskYaml = """
// apiVersion: tekton.dev/v1beta1
// kind: Task
// metadata:
//   name: workflow-task
//   namespace: %s
// spec:
//   steps:
//   - name: echo
//     image: ubuntu
//     command:
//     - echo
//     args:
//     - "Workflow task"
// """.formatted(getCurrentTestNamespace());

//         String taskRunYaml = """
// apiVersion: tekton.dev/v1beta1
// kind: TaskRun
// metadata:
//   name: workflow-taskrun
//   namespace: %s
// spec:
//   taskRef:
//     name: workflow-task
// """.formatted(getCurrentTestNamespace());

//         // Create Jenkins freestyle project for the complete workflow
//         FreeStyleProject workflowProject = jenkinsRule.createFreeStyleProject("test-complete-workflow");
        
//         // Step 1: Create Task
//         CreateRaw createTaskStep = new CreateRaw(taskYaml, "YAML");
//         createTaskStep.setNamespace(getCurrentTestNamespace());
//         createTaskStep.setClusterName("kind-cluster");
//         workflowProject.getBuildersList().add(createTaskStep);
        
//         // Step 2: Create TaskRun
//         CreateRaw createTaskRunStep = new CreateRaw(taskRunYaml, "YAML");
//         createTaskRunStep.setNamespace(getCurrentTestNamespace());
//         createTaskRunStep.setClusterName("kind-cluster");
//         workflowProject.getBuildersList().add(createTaskRunStep);
        
//         // Step 3: Delete TaskRun
//         DeleteRaw deleteTaskRunStep = new DeleteRaw("taskrun", "kind-cluster", 
//                 new DeleteRaw.DeleteAllBlock("workflow-taskrun"));
//         deleteTaskRunStep.setClusterName("kind-cluster");
//         workflowProject.getBuildersList().add(deleteTaskRunStep);
        
//         // Step 4: Delete Task
//         DeleteRaw deleteTaskStep = new DeleteRaw("task", "kind-cluster", 
//                 new DeleteRaw.DeleteAllBlock("workflow-task"));
//         deleteTaskStep.setClusterName("kind-cluster");
//         workflowProject.getBuildersList().add(deleteTaskStep);

//         // Execute the complete workflow
//         FreeStyleBuild workflowBuild = workflowProject.scheduleBuild2(0).get(3, TimeUnit.MINUTES);
//         assertThat(workflowBuild.getResult()).isEqualTo(Result.SUCCESS);
        
//         // Verify both resources were deleted
//         Task deletedTask = tektonClient.v1beta1().tasks()
//                 .inNamespace(getCurrentTestNamespace())
//                 .withName("workflow-task")
//                 .get();
//         assertThat(deletedTask).isNull();

//         TaskRun deletedTaskRun = tektonClient.v1beta1().taskRuns()
//                 .inNamespace(getCurrentTestNamespace())
//                 .withName("workflow-taskrun")
//                 .get();
//         assertThat(deletedTaskRun).isNull();
//     }
} 