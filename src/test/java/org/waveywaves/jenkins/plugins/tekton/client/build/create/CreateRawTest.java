package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.AbortException;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.pipeline.v1beta1.Param;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import org.junit.jupiter.api.AfterEach;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.build.FakeChecksPublisher;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.mock.CreateRawMock;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.mock.FakeCreateRaw;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Pure unit tests for CreateRaw. No JenkinsRule or real Jenkins context; clients are mocked via Mockito.
 */
class CreateRawTest {
    private static final Logger LOGGER = Logger.getLogger(CreateRawTest.class.getName());

    private Run<?, ?> run;
    private String namespace;
    private FakeChecksPublisher checksPublisher;

    @BeforeEach void before() {
        checksPublisher = new FakeChecksPublisher();
        run = mock(Run.class);
    }

    @AfterEach void after() {
        checksPublisher.validate();
    }

//    @Test
//    public void runCreateTaskTest() {
//        String testTaskYaml = "apiVersion: tekton.dev/v1beta1\n" +
//                "kind: Task\n" +
//                "metadata:\n" +
//                "  name: testTask\n";
//        CreateRaw createRaw = new CreateRawMock(testTaskYaml, CreateRaw.InputType.YAML.toString());
//        createRaw.setNamespace(namespace);
//        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
//        createRaw.setEnableCatalog(false);
//        String created = createRaw.runCreate(run, null, null);
//        assertThat(created, is(TektonUtils.TektonResourceType.task.toString()));
//    }

//    @Test
//    public void runCreateTaskRunTest() {
//        String testTaskRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
//                "kind: TaskRun\n" +
//                "metadata:\n" +
//                "  generateName: home-is-set-\n";
//        CreateRaw createRaw = new CreateRawMock(testTaskRunYaml, CreateRaw.InputType.YAML.toString());
//        createRaw.setNamespace(namespace);
//        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
//        createRaw.setEnableCatalog(false);
//        String created = createRaw.runCreate(run, null, null);
//        assertThat(created, is(TektonUtils.TektonResourceType.taskrun.toString()));
//    }

//    @Test
//    public void runCreatePipelineTest() {
//        String testPipelineYaml = "apiVersion: tekton.dev/v1beta1\n" +
//                "kind: Pipeline\n" +
//                "metadata:\n" +
//                "  name: testPipeline\n";
//        CreateRaw createRaw = new CreateRawMock(testPipelineYaml, CreateRaw.InputType.YAML.toString());
//        createRaw.setNamespace(namespace);
//        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
//        createRaw.setEnableCatalog(false);
//        String created = createRaw.runCreate(run, null, null);
//        assertThat(created, is(TektonUtils.TektonResourceType.pipeline.toString()));
//    }

//    @Test
//    public void runCreatePipelineRunTest() {
//        String testPipelineRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
//                "kind: PipelineRun\n" +
//                "metadata:\n" +
//                "  name: testPipelineRun\n";
//        CreateRaw createRaw = new CreateRawMock(testPipelineRunYaml, CreateRaw.InputType.YAML.toString());
//        createRaw.setNamespace(namespace);
//        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
//        createRaw.setEnableCatalog(false);
//        createRaw.setChecksPublisher(checksPublisher);
//        String created = createRaw.runCreate(run, null, null);
//        assertThat(created, is(TektonUtils.TektonResourceType.pipelinerun.toString()));
//    }

    @Test void testCreateRawWithTektonCatalog() throws Exception {
        String testTaskYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: Task\n" +
                "metadata:\n" +
                "  name: testTask\n";
        FakeCreateRaw createRaw = new FakeCreateRaw(testTaskYaml, CreateRaw.InputType.YAML.toString());
        createRaw.setNamespace(namespace);
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setEnableCatalog(true);

        Path tmpDir = Files.createTempDirectory("");
        FilePath workspace = new FilePath(tmpDir.toFile());

        String cheese = "edam";
        EnvVars envVars = new EnvVars("CHEESE", cheese);
//        createRaw.runCreate(run, workspace, envVars);
//
//        String created = createRaw.getLastResource();
//
//        String expectedYaml = testTaskYaml +
//                "labels:\n" +
//                "  cheese: " + cheese + "\n";
//        assertThat(created, is(expectedYaml));
    }

    @Test void testEnhancePipelineWithParams() {
        PipelineRun pr = new PipelineRunBuilder()
                .withNewSpec()
                    .withParams()
                .endSpec()
            .build();
        assertThat(pr, is(notNullValue()));
        assertThat(pr.getSpec(), is(notNullValue()));
        assertThat(pr.getSpec().getParams(), is(notNullValue()));

        EnvVars envVars = new EnvVars();
        envVars.put("GIT_URL" , "https://github.com/org/repo.git");
        envVars.put("JOB_NAME" , "test-tekton-client/main");
        envVars.put("BUILD_ID" , "12");
        envVars.put("GIT_COMMIT", "e9f3c472fea4661b2142c5e88928c5a35e03f51f");
        envVars.put("GIT_BRANCH" , "main");

        new CreateRaw("", "YAML").enhancePipelineRunWithEnvVars(pr, envVars);

        List<Param> params = pr.getSpec().getParams();
        assertThat(params.stream().filter( p -> p.getName().equals("REPO_URL")).findFirst().get(), is(notNullValue()));
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

    /**
     * CI-safe test for Issue #45: verifies the default namespace constant used when
     * no namespace is specified (headless-safe; does not call Jenkins or runCreate).
     */
    @Test void testDefaultNamespaceConstantForMissingNamespace() {
        assertThat(CreateRaw.DEFAULT_NAMESPACE).isEqualTo("default");
    }

    /**
     * Verifies that when checksPublisher is null (e.g. "No suitable checks publisher found"),
     * the pipeline run creation path does not throw NPE. Regression test for Issue #253 / NPE at createPipelineRun.
     */
    @Test void testRunCreateWithNullChecksPublisherDoesNotThrow() {
        String pipelineRunYaml = "apiVersion: tekton.dev/v1beta1\n" +
                "kind: PipelineRun\n" +
                "metadata:\n" +
                "  name: test-pr\n" +
                "  namespace: default\n" +
                "spec:\n" +
                "  pipelineRef:\n" +
                "    name: test-pipeline\n";
        CreateRawMock createRaw = new CreateRawMock(pipelineRunYaml, CreateRaw.InputType.YAML.toString());
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setNamespace("default");
        createRaw.setChecksPublisher(null); // Simulate "No suitable checks publisher found"

        assertDoesNotThrow(() -> createRaw.runCreate(run, null, null),
                "runCreate must not throw NPE when checksPublisher is null (Issue #253)");
    }

    /**
     * Verifies that createPipelineRun fails with a clear exception (AbortException or similar)
     * rather than NullPointerException when input or environment is invalid (e.g. missing metadata or no Tekton client).
     */
    @Test void testCreatePipelineRunFailsGracefullyWithoutNPE() {
        String minimalYaml = "apiVersion: tekton.dev/v1beta1\nkind: PipelineRun\n";
        CreateRaw createRaw = new CreateRaw(minimalYaml, CreateRaw.InputType.YAML.toString());
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setChecksPublisher(null);
        ByteArrayInputStream stream = new ByteArrayInputStream(minimalYaml.getBytes(StandardCharsets.UTF_8));
        EnvVars envVars = new EnvVars();

        Throwable thrown = assertThrows(Throwable.class,
                () -> createRaw.createPipelineRun(stream, envVars));
        assertThat(thrown).isNotNull();
        assertThat(thrown).isNotInstanceOf(NullPointerException.class);
        if (thrown instanceof AbortException) {
            assertThat(thrown.getMessage()).isNotBlank();
        }
    }

    // --- Issue #470: Tekton API v1 support ---

    @Test void testGetApiVersionFromDataV1() {
        String yaml = "apiVersion: tekton.dev/v1\nkind: PipelineRun\nmetadata:\n  name: pr\n";
        assertThat(TektonUtils.getApiVersionFromData(yaml.getBytes(StandardCharsets.UTF_8))).isEqualTo("v1");
    }

    @Test void testGetApiVersionFromDataV1Beta1() {
        String yaml = "apiVersion: tekton.dev/v1beta1\nkind: PipelineRun\nmetadata:\n  name: pr\n";
        assertThat(TektonUtils.getApiVersionFromData(yaml.getBytes(StandardCharsets.UTF_8))).isEqualTo("v1beta1");
    }

    @Test void testUnsupportedApiVersionThrowsAbortException() {
        String yaml = "apiVersion: tekton.dev/v1alpha1\nkind: PipelineRun\nmetadata:\n  name: pr\nspec:\n  pipelineRef:\n    name: p\n";
        CreateRaw createRaw = new CreateRaw(yaml, CreateRaw.InputType.YAML.toString());
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setChecksPublisher(null);
        ByteArrayInputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        AbortException thrown = assertThrows(AbortException.class,
                () -> createRaw.createPipelineRun(stream, new EnvVars()));
        assertThat(thrown.getMessage()).contains("Unsupported Tekton API version");
        assertThat(thrown.getMessage()).contains("v1alpha1");
        assertThat(thrown.getMessage()).contains("v1 and v1beta1");
    }

    /**
     * v1beta1 path: when no cluster is available we get a clear error (e.g. Tekton client not available),
     * but it must NOT be the API version mismatch. Ensures v1beta1 routing is used.
     */
    @Test void testCreateV1Beta1PipelineRunPathStillUsed() {
        String yaml = "apiVersion: tekton.dev/v1beta1\nkind: PipelineRun\nmetadata:\n  name: pr\nspec:\n  pipelineRef:\n    name: p\n";
        CreateRaw createRaw = new CreateRaw(yaml, CreateRaw.InputType.YAML.toString());
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setChecksPublisher(null);
        ByteArrayInputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        Throwable thrown = assertThrows(Throwable.class,
                () -> createRaw.createPipelineRun(stream, new EnvVars()));
        assertThat(thrown.getMessage()).isNotNull();
        assertThat(thrown.getMessage()).doesNotContain("does not match the expected API version");
    }

    /**
     * v1 path: with a mock KubernetesClient the v1 branch succeeds and returns the created resource name.
     * No real cluster or parent POM environment required.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testCreateV1PipelineRunSucceedsWithMockClient() throws Exception {
        String yaml = "apiVersion: tekton.dev/v1\nkind: PipelineRun\nmetadata:\n  name: pr\n  namespace: default\nspec:\n  pipelineRef:\n    name: p\n";
        CreateRaw createRaw = new CreateRaw(yaml, CreateRaw.InputType.YAML.toString());
        createRaw.setClusterName(TektonUtils.DEFAULT_CLIENT_KEY);
        createRaw.setChecksPublisher(null);
        createRaw.setNamespace("default");

        KubernetesClient kc = mock(KubernetesClient.class);
        Resource<HasMetadata> resource = mock(Resource.class);
        when(kc.resource(any(HasMetadata.class))).thenReturn(resource);

        HasMetadata created = mock(HasMetadata.class);
        ObjectMeta meta = mock(ObjectMeta.class);
        when(meta.getName()).thenReturn("test-pr");
        when(created.getMetadata()).thenReturn(meta);
        when(resource.createOrReplace()).thenReturn(created);

        createRaw.setKubernetesClient(kc);

        ByteArrayInputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        String name = createRaw.createPipelineRun(stream, new EnvVars());
        assertEquals("test-pr", name);
    }
}
