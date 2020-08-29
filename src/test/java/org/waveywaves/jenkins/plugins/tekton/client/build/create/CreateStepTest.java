package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import org.junit.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.mock.CreateStepMock;

public class CreateStepTest {

    @Test
    public void runCreateTaskTest(){
        String testTaskYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Task\n" +
                "metadata:\n" +
                "  name: testTask\n";
        CreateStep createStep = new CreateStepMock(testTaskYaml,CreateStep.InputType.YAML.toString());
        String created = createStep.runCreate();
        assert created.equals(TektonUtils.TektonResourceType.task.toString());
    }

    @Test
    public void runCreateTaskRunTest(){
        String testTaskRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: TaskRun\n" +
                "metadata:\n" +
                "  generateName: home-is-set-\n";
        CreateStep createStep = new CreateStepMock(testTaskRunYaml,CreateStep.InputType.YAML.toString());
        String created = createStep.runCreate();
        assert created.equals(TektonUtils.TektonResourceType.taskrun.toString());
    }

    @Test
    public void runCreatePipelineTest(){
        String testPipelineYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Pipeline\n" +
                "metadata:\n" +
                "  name: testPipeline\n";
        CreateStep createStep = new CreateStepMock(testPipelineYaml,CreateStep.InputType.YAML.toString());
        String created = createStep.runCreate();
        assert created.equals(TektonUtils.TektonResourceType.pipeline.toString());
    }

    @Test
    public void runCreatePipelineRunTest(){
        String testPipelineRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: PipelineRun\n" +
                "metadata:\n" +
                "  name: testPipelineRun\n";
        CreateStep createStep = new CreateStepMock(testPipelineRunYaml,CreateStep.InputType.YAML.toString());
        String created = createStep.runCreate();
        assert created.equals(TektonUtils.TektonResourceType.pipelinerun.toString());
    }

    @Test
    public void runCreatePipelineResourceTest(){
        String testPipelineResourceYaml = "apiVersion: tekton.dev/v1alpha1\n" +
                "kind: PipelineResource\n" +
                "metadata:\n" +
                "  name: testPipelineResource\n";
        CreateStep createStep = new CreateStepMock(testPipelineResourceYaml,CreateStep.InputType.YAML.toString());
        String created = createStep.runCreate();
        assert created.equals(TektonUtils.TektonResourceType.pipelineresource.toString());
    }
}
