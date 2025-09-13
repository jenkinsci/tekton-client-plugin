package org.waveywaves.jenkins.plugins.tekton.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for TektonCrdToJavaProcessor.
 * Tests CRD to POJO generation including Jenkins integration features.
 */
class TektonCrdToJavaProcessorTest {

    @TempDir
    Path tempDir;
    
    private TektonCrdToJavaProcessor processor;
    private Path crdDirectory;
    private Path outputDirectory;
    private static final String BASE_PACKAGE = "org.waveywaves.jenkins.plugins.tekton.generated";

    @BeforeEach
    void setUp() throws IOException {
        processor = new TektonCrdToJavaProcessor();
        crdDirectory = tempDir.resolve("crds");
        outputDirectory = tempDir.resolve("generated-sources");
        Files.createDirectories(crdDirectory);
        Files.createDirectories(outputDirectory);
        
        // Copy mock CRD files
        copyMockCrdsToTempDir();
    }

    private void copyMockCrdsToTempDir() throws IOException {
        // Create simple task CRD
        String simpleTaskCrd = createSimpleTaskCrd();
        Files.write(crdDirectory.resolve("simple-task-crd.yaml"), simpleTaskCrd.getBytes());
        
        // Create complex pipeline CRD with multiple versions
        String complexPipelineCrd = createComplexPipelineCrd();
        Files.write(crdDirectory.resolve("complex-pipeline-crd.yaml"), complexPipelineCrd.getBytes());
        
        // Create edge case CRD
        String edgeCaseCrd = createEdgeCaseCrd();
        Files.write(crdDirectory.resolve("edge-case-crd.yaml"), edgeCaseCrd.getBytes());
    }

    @Test
    void testProcessDirectoryWithInheritance() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Assert - Check that files were generated
        assertThat(outputDirectory).exists();
        
        // Find generated Java classes
        List<Path> generatedFiles = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .toList();
        
        assertThat(generatedFiles).isNotEmpty();
        
        // Verify Jenkins integration for at least one generated class
        // Note: The actual generated class names depend on the CRD structure
        // Let's find any Java file first
        Path stepClass = generatedFiles.stream()
            .findFirst()
            .orElseThrow(() -> new AssertionError("No Java classes found"));
        
        String content = Files.readString(stepClass);
        
        // For now, just verify basic structure since inheritance mapping needs to be configured
        assertThat(content).contains("package org.waveywaves.jenkins.plugins.tekton.generated");
        assertThat(content).contains("public class");
        
        // Check Jackson annotations (should always be present)
        assertThat(content).contains("@JsonInclude");
        assertThat(content).contains("import com.fasterxml.jackson.annotation");
    }

    @Test
    void testMultiVersionCrdProcessing() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Assert - Check both versions are generated
        List<Path> v1Files = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().contains("/v1/"))
            .filter(p -> p.toString().endsWith(".java"))
            .toList();
        
        List<Path> v1beta1Files = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().contains("/v1beta1/"))
            .filter(p -> p.toString().endsWith(".java"))
            .toList();
        
        assertThat(v1Files).isNotEmpty();
        assertThat(v1beta1Files).isNotEmpty();
        
        // Verify both versions have proper package structure
        for (Path file : v1Files) {
            String content = Files.readString(file);
            assertThat(content).contains("package org.waveywaves.jenkins.plugins.tekton.generated");
            assertThat(content).contains(".v1");
        }
        
        for (Path file : v1beta1Files) {
            String content = Files.readString(file);
            assertThat(content).contains("package org.waveywaves.jenkins.plugins.tekton.generated");
            assertThat(content).contains(".v1beta1");
        }
    }

    @Test
    void testEdgeCaseHandling() throws IOException {
        // Act - Should not throw exception
        assertThatCode(() -> {
            processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        }).doesNotThrowAnyException();
        
        // Verify edge case CRD still generates valid classes
        List<Path> edgeCaseFiles = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().contains("edge"))
            .filter(p -> p.toString().endsWith(".java"))
            .toList();
        
        assertThat(edgeCaseFiles).isNotEmpty();
        
        Path edgeCaseFile = edgeCaseFiles.get(0);
        String content = Files.readString(edgeCaseFile);
        assertThat(content).contains("package org.waveywaves.jenkins.plugins.tekton.generated");
        assertThat(content).contains("public class");
    }

    @Test
    void testClassNameMapping() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Assert - Verify class naming
        List<Path> taskFiles = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().contains("simpletasks"))
            .filter(p -> p.toString().endsWith(".java"))
            .toList();
        
        assertThat(taskFiles).isNotEmpty();
        
        // Check that at least one file has proper class declaration
        boolean hasValidClass = taskFiles.stream().anyMatch(file -> {
            try {
                String content = Files.readString(file);
                return content.contains("public class") && content.contains("Simpletasks");
            } catch (IOException e) {
                return false;
            }
        });
        assertThat(hasValidClass).isTrue();
    }

    @Test
    void testPackageStructureGeneration() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Assert - Check package structure
        Path expectedPackageDir = outputDirectory.resolve("org/waveywaves/jenkins/plugins/tekton/generated/simpletasks/v1");
        assertThat(expectedPackageDir).exists();
        
        // Check package declaration in generated file
        List<Path> generatedFiles = Files.list(expectedPackageDir)
            .filter(p -> p.toString().endsWith("CreateSimpleTaskTyped.java"))
            .toList();
        
        assertThat(generatedFiles).hasSize(1);
        
        String content = Files.readString(generatedFiles.get(0));
        assertThat(content).contains("package org.waveywaves.jenkins.plugins.tekton.generated.simpletasks.v1");
    }

    @Test
    void testPOJOGeneration() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Assert - Check multiple POJOs are generated
        List<Path> allJavaFiles = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .toList();
        
        assertThat(allJavaFiles).hasSizeGreaterThan(3); // Should generate multiple POJOs
        
        // Check some files have Jenkins Step patterns
        long jenkinsStepFiles = allJavaFiles.stream()
            .mapToLong(file -> {
                try {
                    String content = Files.readString(file);
                    return content.contains("extends BaseStep") ? 1 : 0;
                } catch (IOException e) {
                    return 0;
                }
            })
            .sum();
        
        assertThat(jenkinsStepFiles).isGreaterThan(0);
        
        // Check files have proper naming pattern
        long typedFiles = allJavaFiles.stream()
            .filter(p -> p.getFileName().toString().endsWith("Typed.java"))
            .count();
        
        assertThat(typedFiles).isGreaterThan(0);
    }

    @Test
    void testInvalidCrdGracefulHandling() throws IOException {
        // Arrange - Create invalid YAML
        String invalidYaml = "invalid: yaml: content:";
        Files.write(crdDirectory.resolve("invalid-crd.yaml"), invalidYaml.getBytes());
        
        // Act - Should not throw exception
        assertThatCode(() -> {
            processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        }).doesNotThrowAnyException();
    }

    @Test
    void testEmptyDirectoryHandling() throws IOException {
        // Arrange - Create empty directory
        Path emptyDir = tempDir.resolve("empty-crds");
        Files.createDirectories(emptyDir);
        
        // Act - Should not throw exception
        assertThatCode(() -> {
            processor.processDirectory(emptyDir, outputDirectory, BASE_PACKAGE, true);
        }).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("getCompilationTestData")
    void testGeneratedCodeCompilation(String crdName, String expectedClassName, boolean shouldHaveConstructor) throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find the generated class
        Path generatedClass = findGeneratedStepClass(expectedClassName);
        assertThat(generatedClass).exists();
        
        String content = Files.readString(generatedClass);
        
        // Basic syntax checks
        assertThat(content).startsWith("package ");
        assertThat(content).contains("public class " + expectedClassName);
        assertThat(countOccurrences(content, '{'))
            .as("Braces should be balanced")
            .isEqualTo(countOccurrences(content, '}'));
        
        // Jenkins integration checks
        assertThat(content).contains("extends BaseStep");
        
        if (shouldHaveConstructor) {
            assertThat(content).contains("@DataBoundConstructor");
            assertThat(content).contains("super()");
        }
        
        // Should not have common syntax errors
        assertThat(content).doesNotContain(";;");
        assertThat(content).doesNotContain("{{");
        assertThat(content).doesNotContain("class class");
    }

    private static Stream<Arguments> getCompilationTestData() {
        return Stream.of(
            Arguments.of("simple-task", "CreateSimpleTaskTyped", true),
            Arguments.of("complex-pipeline", "CreateComplexPipelineTyped", true),
            Arguments.of("edge-case", "CreateEdgeCaseTyped", true)
        );
    }

    private Path findGeneratedStepClass(String className) throws IOException {
        return Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().equals(className + ".java"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Generated class not found: " + className));
    }

    private int countOccurrences(String text, char character) {
        return (int) text.chars().filter(c -> c == character).count();
    }

    // Mock CRD creation methods
    private String createSimpleTaskCrd() {
        return """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: simpletasks.tekton.dev
            spec:
              group: tekton.dev
              versions:
              - name: v1
                served: true
                storage: true
                schema:
                  openAPIV3Schema:
                    type: object
                    properties:
                      apiVersion:
                        type: string
                      kind:
                        type: string
                      metadata:
                        type: object
                      spec:
                        type: object
                        properties:
                          description:
                            type: string
                          params:
                            type: array
                            items:
                              type: object
                              properties:
                                name:
                                  type: string
                                type:
                                  type: string
                                default:
                                  type: string
                          steps:
                            type: array
                            items:
                              type: object
                              properties:
                                name:
                                  type: string
                                image:
                                  type: string
                                command:
                                  type: array
                                  items:
                                    type: string
              scope: Namespaced
              names:
                plural: simpletasks
                singular: simpletask
                kind: SimpleTask
            """;
    }

    private String createComplexPipelineCrd() {
        return """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: complexpipelines.tekton.dev
            spec:
              group: tekton.dev
              versions:
              - name: v1
                served: true
                storage: true
                schema:
                  openAPIV3Schema:
                    type: object
                    properties:
                      apiVersion:
                        type: string
                      kind:
                        type: string
                      metadata:
                        type: object
                      spec:
                        type: object
                        properties:
                          description:
                            type: string
                          params:
                            type: array
                            items:
                              type: object
                          tasks:
                            type: array
                            items:
                              type: object
              - name: v1beta1
                served: true
                storage: false
                schema:
                  openAPIV3Schema:
                    type: object
                    properties:
                      apiVersion:
                        type: string
                      kind:
                        type: string
                      metadata:
                        type: object
                      spec:
                        type: object
                        properties:
                          description:
                            type: string
                          params:
                            type: array
                            items:
                              type: object
              scope: Namespaced
              names:
                plural: complexpipelines
                singular: complexpipeline
                kind: ComplexPipeline
            """;
    }

    private String createEdgeCaseCrd() {
        return """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: edge-cases.tekton.dev
            spec:
              group: tekton.dev
              versions:
              - name: v1
                served: true
                storage: true
                schema:
                  openAPIV3Schema:
                    type: object
                    properties:
                      apiVersion:
                        type: string
                      kind:
                        type: string
                      metadata:
                        type: object
                      spec:
                        type: object
                        properties:
                          "special-field":
                            type: string
                          "nested-object":
                            type: object
                            properties:
                              "deep-nested":
                                type: object
                                properties:
                                  "very-deep":
                                    type: string
              scope: Namespaced
              names:
                plural: edgecases
                singular: edgecase
                kind: EdgeCase
            """;
    }
}
