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
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TektonDeleteE2ETest extends E2ETestBase {

        @BeforeEach
        public void setUp() throws Exception {
                // Setup TektonUtils for E2E test environment
                TektonUtils.initializeKubeClients(tektonClient.getConfiguration());

                java.lang.reflect.Field mapField = TektonUtils.class.getDeclaredField("tektonClientMap");
                mapField.setAccessible(true);

                Object mapObj = mapField.get(null);
                if (mapObj == null) {
                        mapObj = new java.util.concurrent.ConcurrentHashMap<String, Object>();
                        mapField.set(null, mapObj);
                }

                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> clientMap = (java.util.Map<String, Object>) mapObj;
                clientMap.put(TektonUtils.DEFAULT_CLIENT_KEY, tektonClient);

                // Register DeleteRaw descriptor
                DeleteRaw.DescriptorImpl descriptor = new DeleteRaw.DescriptorImpl();
                jenkinsRule.jenkins.getDescriptorList(hudson.tasks.Builder.class).add(descriptor);
        }

        @Test
        public void testDeleteSpecificTaskRun() throws Exception {
                String currentNamespace = getCurrentTestNamespace();

                // First create a TaskRun
                String taskRunYaml = """
                                apiVersion: tekton.dev/v1beta1
                                kind: TaskRun
                                metadata:
                                  name: taskrun-to-delete
                                  namespace: %s
                                spec:
                                  taskSpec:
                                    steps:
                                    - name: echo
                                      image: ubuntu
                                      command:
                                      - echo
                                      args:
                                      - "I will be deleted"
                                """.formatted(currentNamespace);

                // Create Jenkins freestyle project for creation
                FreeStyleProject createProject = jenkinsRule.createFreeStyleProject("test-create-for-delete");

                CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
                createStep.setNamespace(currentNamespace);
                createStep.setClusterName("");

                createProject.getBuildersList().add(createStep);

                // Execute the creation build
                FreeStyleBuild createBuild = createProject.scheduleBuild2(0).get(2, TimeUnit.MINUTES);
                assertThat(createBuild.getResult()).isEqualTo(Result.SUCCESS);

                // Verify TaskRun was created
                TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
                                .inNamespace(currentNamespace)
                                .withName("taskrun-to-delete")
                                .get();

                assertThat(createdTaskRun).isNotNull();

                FreeStyleProject deleteProject = jenkinsRule.createFreeStyleProject("test-delete-taskrun");

                DeleteRaw deleteStep = new DeleteRaw("taskrun", "",
                                new DeleteRaw.DeleteAllBlock("taskrun-to-delete"));

                deleteProject.getBuildersList().add(deleteStep);

                FreeStyleBuild deleteBuild = deleteProject.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
                assertThat(deleteBuild.getResult()).isEqualTo(Result.SUCCESS);

                Thread.sleep(2000);

                TaskRun deletedTaskRun = tektonClient.v1beta1().taskRuns()
                                .inNamespace(currentNamespace)
                                .withName("taskrun-to-delete")
                                .get();

                assertThat(deletedTaskRun).isNull();
        }

        @Test
        public void testDeleteSpecificTask() throws Exception {
                String currentNamespace = getCurrentTestNamespace();

                // First create a Task
                String taskYaml = """
                                apiVersion: tekton.dev/v1beta1
                                kind: Task
                                metadata:
                                 name: task-to-delete
                                 namespace: %s
                                spec:
                                 steps:
                                 - name: echo
                                   image: ubuntu
                                   command:
                                   - echo
                                   args:
                                   - "I am a task to be deleted"
                                """.formatted(currentNamespace);

                // Create Jenkins freestyle project for creation
                FreeStyleProject createProject = jenkinsRule.createFreeStyleProject("test-create-task-for-delete");

                CreateRaw createStep = new CreateRaw(taskYaml, "YAML");
                createStep.setNamespace(currentNamespace);
                createStep.setClusterName("");

                createProject.getBuildersList().add(createStep);

                // Execute the creation build
                FreeStyleBuild createBuild = createProject.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
                assertThat(createBuild.getResult()).isEqualTo(Result.SUCCESS);

                // Verify Task was created
                Task createdTask = tektonClient.v1beta1().tasks()
                                .inNamespace(currentNamespace)
                                .withName("task-to-delete")
                                .get();
                assertThat(createdTask).isNotNull();

                // Now create a project to delete the Task
                FreeStyleProject deleteProject = jenkinsRule.createFreeStyleProject("test-delete-task");

                DeleteRaw deleteStep = new DeleteRaw("task", "",
                                new DeleteRaw.DeleteAllBlock("task-to-delete"));

                deleteProject.getBuildersList().add(deleteStep);

                // Execute the deletion build
                FreeStyleBuild deleteBuild = deleteProject.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
                assertThat(deleteBuild.getResult()).isEqualTo(Result.SUCCESS);

                // Verify Task was deleted
                Task deletedTask = tektonClient.v1beta1().tasks()
                                .inNamespace(currentNamespace)
                                .withName("task-to-delete")
                                .get();
                assertThat(deletedTask).isNull();
        }

        @Test
        public void testDeleteAllTaskRuns() throws Exception {
                String currentNamespace = getCurrentTestNamespace();

                // First create multiple TaskRuns
                String[] taskRunNames = { "taskrun-1", "taskrun-2", "taskrun-3" };

                for (String taskRunName : taskRunNames) {
                        String taskRunYaml = """
                                        apiVersion: tekton.dev/v1beta1
                                        kind: TaskRun
                                        metadata:
                                         name: %s
                                         namespace: %s
                                        spec:
                                         taskSpec:
                                           steps:
                                           - name: echo
                                             image: ubuntu
                                             command:
                                             - echo
                                             args:
                                             - "TaskRun %s"
                                        """.formatted(taskRunName, currentNamespace, taskRunName);

                        // Create Jenkins freestyle project for creation
                        FreeStyleProject createProject = jenkinsRule
                                        .createFreeStyleProject("test-create-" + taskRunName);

                        CreateRaw createStep = new CreateRaw(taskRunYaml, "YAML");
                        createStep.setNamespace(currentNamespace);
                        createStep.setClusterName("");

                        createProject.getBuildersList().add(createStep);

                        // Execute the creation build
                        FreeStyleBuild createBuild = createProject.scheduleBuild2(0).get(2, TimeUnit.MINUTES);
                        assertThat(createBuild.getResult()).isEqualTo(Result.SUCCESS);
                }

                // Verify all TaskRuns were created
                for (String taskRunName : taskRunNames) {
                        TaskRun createdTaskRun = tektonClient.v1beta1().taskRuns()
                                        .inNamespace(currentNamespace)
                                        .withName(taskRunName)
                                        .get();
                        assertThat(createdTaskRun).isNotNull();
                }

                // Now create a project to delete all TaskRuns (null name means delete all)
                FreeStyleProject deleteProject = jenkinsRule.createFreeStyleProject("test-delete-all-taskruns");

                DeleteRaw deleteStep = new DeleteRaw("taskrun", "",
                                new DeleteRaw.DeleteAllBlock(null)); // null means delete all

                deleteProject.getBuildersList().add(deleteStep);

                // Execute the deletion build
                FreeStyleBuild deleteBuild = deleteProject.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
                assertThat(deleteBuild.getResult()).isEqualTo(Result.SUCCESS);

                // Verify all TaskRuns were deleted
                Thread.sleep(2000);

                for (String taskRunName : taskRunNames) {
                        TaskRun deletedTaskRun = tektonClient.v1beta1().taskRuns()
                                        .inNamespace(currentNamespace)
                                        .withName(taskRunName)
                                        .get();
                        assertThat(deletedTaskRun).isNull();
                }
        }

        @Test
        public void testDeleteNonExistentResource() throws Exception {
                // Try to delete a TaskRun that doesn't exist
                FreeStyleProject deleteProject = jenkinsRule.createFreeStyleProject("test-delete-nonexistent");

                DeleteRaw deleteStep = new DeleteRaw("taskrun", "",
                                new DeleteRaw.DeleteAllBlock("nonexistent-taskrun"));

                deleteProject.getBuildersList().add(deleteStep);

                // Execute the deletion build - should still succeed (idempotent)
                FreeStyleBuild deleteBuild = deleteProject.scheduleBuild2(0).get(1, TimeUnit.MINUTES);
                assertThat(deleteBuild.getResult()).isEqualTo(Result.SUCCESS);
        }

        @Test
        public void testCreateAndDeleteWorkflow() throws Exception {
                String currentNamespace = getCurrentTestNamespace();

                // Test a complete workflow: create multiple resources, then delete them
                String taskYaml = """
                                apiVersion: tekton.dev/v1beta1
                                kind: Task
                                metadata:
                                 name: workflow-task
                                 namespace: %s
                                spec:
                                 steps:
                                 - name: echo
                                   image: ubuntu
                                   command:
                                   - echo
                                   args:
                                   - "Workflow task"
                                """.formatted(currentNamespace);

                String taskRunYaml = """
                                apiVersion: tekton.dev/v1beta1
                                kind: TaskRun
                                metadata:
                                 name: workflow-taskrun
                                 namespace: %s
                                spec:
                                 taskRef:
                                   name: workflow-task
                                """.formatted(currentNamespace);

                // Create Jenkins freestyle project for the complete workflow
                FreeStyleProject workflowProject = jenkinsRule.createFreeStyleProject("test-complete-workflow");

                // Step 1: Create Task
                CreateRaw createTaskStep = new CreateRaw(taskYaml, "YAML");
                createTaskStep.setNamespace(currentNamespace);
                createTaskStep.setClusterName("");
                workflowProject.getBuildersList().add(createTaskStep);

                // Step 2: Create TaskRun
                CreateRaw createTaskRunStep = new CreateRaw(taskRunYaml, "YAML");
                createTaskRunStep.setNamespace(currentNamespace);
                createTaskRunStep.setClusterName("");
                workflowProject.getBuildersList().add(createTaskRunStep);

                // Step 3: Delete TaskRun
                DeleteRaw deleteTaskRunStep = new DeleteRaw("taskrun", "",
                                new DeleteRaw.DeleteAllBlock("workflow-taskrun"));
                workflowProject.getBuildersList().add(deleteTaskRunStep);

                // Step 4: Delete Task
                DeleteRaw deleteTaskStep = new DeleteRaw("task", "",
                                new DeleteRaw.DeleteAllBlock("workflow-task"));
                workflowProject.getBuildersList().add(deleteTaskStep);

                // Execute the complete workflow
                FreeStyleBuild workflowBuild = workflowProject.scheduleBuild2(0).get(3, TimeUnit.MINUTES);
                assertThat(workflowBuild.getResult()).isEqualTo(Result.SUCCESS);

                // Verify both resources were deleted
                Task deletedTask = tektonClient.v1beta1().tasks()
                                .inNamespace(currentNamespace)
                                .withName("workflow-task")
                                .get();
                assertThat(deletedTask).isNull();

                TaskRun deletedTaskRun = tektonClient.v1beta1().taskRuns()
                                .inNamespace(currentNamespace)
                                .withName("workflow-taskrun")
                                .get();
                assertThat(deletedTaskRun).isNull();
        }
}
