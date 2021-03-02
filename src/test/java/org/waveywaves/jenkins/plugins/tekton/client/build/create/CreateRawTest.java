package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.EnvVars;
import hudson.FilePath;
import org.junit.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.mock.CreateRawMock;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.mock.FakeCreateRaw;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateRawTest {
    public static final boolean EnableCatalog = false;

    @Test
    public void runCreateTaskTest(){
        String testTaskYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Task\n" +
                "metadata:\n" +
                "  name: testTask\n";
        CreateRaw createRaw = new CreateRawMock(testTaskYaml, CreateRaw.InputType.YAML.toString(), EnableCatalog);
        String created = createRaw.runCreate(null, null);
        assert created.equals(TektonUtils.TektonResourceType.task.toString());
    }

    @Test
    public void runCreateTaskRunTest(){
        String testTaskRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: TaskRun\n" +
                "metadata:\n" +
                "  generateName: home-is-set-\n";
        CreateRaw createRaw = new CreateRawMock(testTaskRunYaml, CreateRaw.InputType.YAML.toString(), EnableCatalog);
        String created = createRaw.runCreate(null, null);
        assert created.equals(TektonUtils.TektonResourceType.taskrun.toString());
    }

    @Test
    public void runCreatePipelineTest(){
        String testPipelineYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Pipeline\n" +
                "metadata:\n" +
                "  name: testPipeline\n";
        CreateRaw createRaw = new CreateRawMock(testPipelineYaml, CreateRaw.InputType.YAML.toString(), EnableCatalog);
        String created = createRaw.runCreate(null, null);
        assert created.equals(TektonUtils.TektonResourceType.pipeline.toString());
    }

    @Test
    public void runCreatePipelineRunTest(){
        String testPipelineRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: PipelineRun\n" +
                "metadata:\n" +
                "  name: testPipelineRun\n";
        CreateRaw createRaw = new CreateRawMock(testPipelineRunYaml, CreateRaw.InputType.YAML.toString(), EnableCatalog);
        String created = createRaw.runCreate(null, null);
        assert created.equals(TektonUtils.TektonResourceType.pipelinerun.toString());
    }

    @Test
    public void runCreatePipelineResourceTest(){
        String testPipelineResourceYaml = "apiVersion: tekton.dev/v1alpha1\n" +
                "kind: PipelineResource\n" +
                "metadata:\n" +
                "  name: testPipelineResource\n";
        CreateRaw createRaw = new CreateRawMock(testPipelineResourceYaml, CreateRaw.InputType.YAML.toString(), EnableCatalog);
        String created = createRaw.runCreate(null, null);
        assert created.equals(TektonUtils.TektonResourceType.pipelineresource.toString());
    }


    @Test
    public void testCreateRawWithTektonCatalog() throws Exception {
        String testTaskYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Task\n" +
                "metadata:\n" +
                "  name: testTask\n";
        FakeCreateRaw createRaw = new FakeCreateRaw(testTaskYaml, CreateRaw.InputType.YAML.toString(), true);

        Path tmpDir = Files.createTempDirectory("");
        FilePath workspace = new FilePath(tmpDir.toFile());

        String cheese = "edam";
        EnvVars envVars = new EnvVars("CHEESE", cheese);
        createRaw.runCreate(workspace, envVars);

        String created = createRaw.getLastResource();

        String expectedYaml = testTaskYaml +
                "  labels:\n" +
                "    cheese: " + cheese + "\n";
        assertThat(created).isEqualTo(expectedYaml);
    }

}
