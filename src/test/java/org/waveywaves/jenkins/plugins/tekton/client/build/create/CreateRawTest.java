package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import io.fabric8.tekton.pipeline.v1beta1.Param;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.FakeChecksPublisher;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.mock.CreateRawMock;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.mock.FakeCreateRaw;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class CreateRawTest {

    private Run<?, ?> run;
    private String namespace;
    private FakeChecksPublisher checksPublisher;

    @BeforeEach
    void before() {
        checksPublisher = new FakeChecksPublisher();
    }

    @AfterEach
    void after() {
        checksPublisher.validate();
    }

    @Test
    void runCreateTaskTest() {
        String testTaskYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Task\n" +
                "metadata:\n" +
                "  name: testTask\n";

        CreateRaw createRaw = new CreateRawMock(testTaskYaml, CreateRaw.InputType.YAML.toString());
        createRaw.setNamespace("test");
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setEnableCatalog(false);

        String created = createRaw.runCreate(run, null, null);

        assertThat(created, is(TektonUtils.TektonResourceType.task.toString()));
    }

    @Test
    void runCreateTaskRunTest() {
        String testTaskRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: TaskRun\n" +
                "metadata:\n" +
                "  generateName: home-is-set-\n";

        CreateRaw createRaw = new CreateRawMock(testTaskRunYaml, CreateRaw.InputType.YAML.toString());
        createRaw.setNamespace("test");
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setEnableCatalog(false);

        String created = createRaw.runCreate(run, null, null);

        assertThat(created, is(TektonUtils.TektonResourceType.taskrun.toString()));
    }

    @Test
    void runCreatePipelineTest() {
        String testPipelineYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Pipeline\n" +
                "metadata:\n" +
                "  name: testPipeline\n";

        CreateRaw createRaw = new CreateRawMock(testPipelineYaml, CreateRaw.InputType.YAML.toString());
        createRaw.setNamespace(namespace);
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setEnableCatalog(false);

        String created = createRaw.runCreate(run, null, null);
        assertThat(created, is(TektonUtils.TektonResourceType.pipeline.toString()));
    }

    @Test
    void runCreatePipelineRunTest() {
        String testPipelineRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: PipelineRun\n" +
                "metadata:\n" +
                "  name: testPipelineRun\n";

        CreateRaw createRaw = new CreateRawMock(testPipelineRunYaml, CreateRaw.InputType.YAML.toString());
        createRaw.setNamespace(namespace);
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setEnableCatalog(false);
        createRaw.setChecksPublisher(checksPublisher);

        String created = createRaw.runCreate(run, null, null);

        assertThat(created, is(TektonUtils.TektonResourceType.pipelinerun.toString()));
    }

    @Test
    void testCreateRawWithTektonCatalog() throws Exception {
        String testTaskYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Task\n" +
                "metadata:\n" +
                "  name: testTask\n";

        FakeCreateRaw createRaw = new FakeCreateRaw(testTaskYaml, CreateRaw.InputType.YAML.toString());
        createRaw.setNamespace(namespace);
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setEnableCatalog(true);
        createRaw.setChecksPublisher(checksPublisher);

        Path tmpDir = Files.createTempDirectory("");
        FilePath workspace = new FilePath(tmpDir.toFile());

        String cheese = "edam";
        EnvVars envVars = new EnvVars("CHEESE", cheese);

        // Act
        createRaw.runCreate(run, workspace, envVars);
        String created = createRaw.getLastResource();
        String expectedYaml = testTaskYaml +
                "labels:\n" +
                "  cheese: " + cheese + "\n";

        // Assert
        assertThat(created, is(expectedYaml));
    }

    @Test
    void testEnhancePipelineWithParams() {
        PipelineRun pr = new PipelineRunBuilder()
                .withNewSpec()
                .withParams()
                .endSpec()
                .build();
        assertThat(pr, is(notNullValue()));
        assertThat(pr.getSpec(), is(notNullValue()));
        assertThat(pr.getSpec().getParams(), is(notNullValue()));

        EnvVars envVars = new EnvVars();
        envVars.put("GIT_URL", "https://github.com/org/repo.git");
        envVars.put("JOB_NAME", "test-tekton-client/main");
        envVars.put("BUILD_ID", "12");
        envVars.put("GIT_COMMIT", "e9f3c472fea4661b2142c5e88928c5a35e03f51f");
        envVars.put("GIT_BRANCH", "main");

        new CreateRaw("", "YAML").enhancePipelineRunWithEnvVars(pr, envVars);

        List<Param> params = pr.getSpec().getParams();
        assertThat(params.stream().filter(p -> p.getName().equals("REPO_URL")).findFirst().get(), is(notNullValue()));
        assertThat(getStringValue(params, "REPO_URL"), is("https://github.com/org/repo.git"));
        assertThat(getStringValue(params, "JOB_NAME"), is("test-tekton-client/main"));
        assertThat(getStringValue(params, "BUILD_ID"), is("12"));
        assertThat(getStringValue(params, "PULL_PULL_SHA"), is("e9f3c472fea4661b2142c5e88928c5a35e03f51f"));
        assertThat(getStringValue(params, "PULL_BASE_REF"), is("main"));
        assertThat(getStringValue(params, "REPO_NAME"), is("repo"));
        assertThat(getStringValue(params, "REPO_OWNER"), is("org"));
    }

    private String getStringValue(List<Param> params, String name) {
        Param param = params.stream().filter(p -> p.getName().equals(name)).findFirst().get();
        assertThat(param, is(notNullValue()));
        return param.getValue().getStringVal();
    }

}
