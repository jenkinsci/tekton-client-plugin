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
 * Tests focused on Jenkins-specific integration aspects of generated POJOs.
 * Verifies that generated classes properly integrate with Jenkins pipeline framework.
 */
class JenkinsIntegrationTest {

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
        // Simple Task CRD for basic Jenkins integration testing
        String simpleTaskCrd = createJenkinsTaskCrd();
        Files.write(crdDirectory.resolve("jenkins-task-crd.yaml"), simpleTaskCrd.getBytes());
        
        // Complex Pipeline CRD for advanced Jenkins features
        String complexPipelineCrd = createJenkinsPipelineCrd();
        Files.write(crdDirectory.resolve("jenkins-pipeline-crd.yaml"), complexPipelineCrd.getBytes());
        
        // Edge case CRD to test robust Jenkins integration
        String edgeCaseCrd = createJenkinsEdgeCaseCrd();
        Files.write(crdDirectory.resolve("jenkins-edge-case-crd.yaml"), edgeCaseCrd.getBytes());
    }

    @Test
    void testBaseStepInheritance() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated Jenkins step class
        Path stepClass = findGeneratedStepClass("CreateJenkinsTaskTyped");
        String content = Files.readString(stepClass);
        
        // Assert - Check BaseStep inheritance
        assertThat(content).contains("extends BaseStep");
        assertThat(content).contains("import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep");
        
        // Check class declaration syntax
        assertThat(content).containsPattern("public class CreateJenkinsTaskTyped\\s+extends BaseStep");
    }

    @Test
    void testDataBoundConstructorAnnotation() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated Jenkins step class
        Path stepClass = findGeneratedStepClass("CreateJenkinsTaskTyped");
        String content = Files.readString(stepClass);
        
        // Assert - Check DataBoundConstructor
        assertThat(content).contains("@DataBoundConstructor");
        assertThat(content).contains("import org.kohsuke.stapler.DataBoundConstructor");
        
        // Check constructor format
        assertThat(content).containsPattern("@DataBoundConstructor\\s+public CreateJenkinsTaskTyped");
        
        // Check super() call in constructor
        assertThat(content).contains("super()");
    }

    @Test
    void testJenkinsStepStructure() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated Jenkins step class
        Path stepClass = findGeneratedStepClass("CreateJenkinsTaskTyped");
        String content = Files.readString(stepClass);
        
        // Assert - Full Jenkins Step structure
        assertThat(content).contains("public class CreateJenkinsTaskTyped extends BaseStep");
        
        // Required imports for Jenkins integration
        assertThat(content).contains("import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep");
        assertThat(content).contains("import org.kohsuke.stapler.DataBoundConstructor");
        
        // Constructor with proper annotation and super call
        assertThat(content).containsPattern("@DataBoundConstructor\\s+public CreateJenkinsTaskTyped");
        assertThat(content).contains("super()");
        
        // Jackson annotations for JSON serialization
        assertThat(content).contains("@JsonInclude");
        assertThat(content).contains("@JsonPropertyOrder");
    }

    @Test
    void testMultiVersionJenkinsIntegration() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find both version classes
        Path v1Class = findGeneratedStepClass("CreateJenkinsPipelineTyped", "v1");
        Path v1beta1Class = findGeneratedStepClass("CreateJenkinsPipelineTyped", "v1beta1");
        
        String v1Content = Files.readString(v1Class);
        String v1beta1Content = Files.readString(v1beta1Class);
        
        // Assert - Both versions have Jenkins integration
        assertThat(v1Content).contains("extends BaseStep");
        assertThat(v1Content).contains("@DataBoundConstructor");
        
        assertThat(v1beta1Content).contains("extends BaseStep");
        assertThat(v1beta1Content).contains("@DataBoundConstructor");
        
        // Check package naming
        assertThat(v1Content).contains("package org.waveywaves.jenkins.plugins.tekton.generated.jenkinspipelines.v1");
        assertThat(v1beta1Content).contains("package org.waveywaves.jenkins.plugins.tekton.generated.jenkinspipelines.v1beta1");
    }

    @Test
    void testEdgeCaseJenkinsIntegration() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find edge case class
        Path edgeClass = findGeneratedStepClass("CreateJenkinsEdgeCaseTyped");
        String content = Files.readString(edgeClass);
        
        // Assert - Jenkins integration works even with edge cases
        assertThat(content).contains("extends BaseStep");
        assertThat(content).contains("@DataBoundConstructor");
        
        // Should not have duplicate imports
        long baseStepImports = content.lines()
            .filter(line -> line.contains("import") && line.contains("BaseStep"))
            .count();
        assertThat(baseStepImports).isEqualTo(1);
        
        // Should not have syntax errors from edge cases
        assertThat(content).doesNotContain("class class");
        assertThat(content).doesNotContain("import import");
    }

    @Test
    void testPackageAndClassNaming() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Check package naming convention
        Path taskClass = findGeneratedStepClass("CreateJenkinsTaskTyped");
        String taskContent = Files.readString(taskClass);
        
        assertThat(taskContent).contains("package org.waveywaves.jenkins.plugins.tekton.generated.jenkinstasks.v1");
        assertThat(taskContent).contains("public class CreateJenkinsTaskTyped");
        
        // Verify naming avoids conflicts with existing Jenkins classes
        assertThat(taskContent).doesNotContain("public class Task"); // Avoid conflict with Jenkins Task
        assertThat(taskContent).doesNotContain("public class Pipeline"); // Avoid conflict with Jenkins Pipeline
    }

    @Test
    void testGeneratedCodeSyntaxValidity() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Get all generated step classes
        List<Path> stepClasses = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().endsWith("Typed.java"))
            .toList();
        
        assertThat(stepClasses).isNotEmpty();
        
        for (Path stepClass : stepClasses) {
            String content = Files.readString(stepClass);
            
            // Basic syntax validation
            assertThat(content).startsWith("package ");
            assertThat(content).contains("public class ");
            
            // Balanced braces
            long openBraces = content.chars().filter(c -> c == '{').count();
            long closeBraces = content.chars().filter(c -> c == '}').count();
            assertThat(openBraces).as("Braces should be balanced in " + stepClass.getFileName()).isEqualTo(closeBraces);
            
            // No common syntax errors
            assertThat(content).doesNotContain(";;");
            assertThat(content).doesNotContain("{{");
            assertThat(content).doesNotContain("class class");
        }
    }

    @Test
    void testDirectoryProcessingWithoutInheritance() throws IOException {
        // Act - Process without inheritance
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, false);
        
        // Should still generate files but without Jenkins-specific features
        List<Path> generatedFiles = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .toList();
        
        assertThat(generatedFiles).isNotEmpty();
        
        // Files should exist but may not have Jenkins integration
        assertThatCode(() -> {
            processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, false);
        }).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("getJenkinsIntegrationTestData")
    void testParameterizedJenkinsIntegration(String crdName, String expectedClassName, String expectedPackage) throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated class
        Path generatedClass = findGeneratedStepClass(expectedClassName);
        String content = Files.readString(generatedClass);
        
        // Assert - Jenkins integration
        assertThat(content).contains("extends BaseStep");
        assertThat(content).contains("@DataBoundConstructor");
        assertThat(content).contains("package " + expectedPackage);
        assertThat(content).contains("public class " + expectedClassName);
        
        // Verify constructor calls super()
        assertThat(content).containsPattern("@DataBoundConstructor\\s+public " + expectedClassName);
        assertThat(content).contains("super()");
    }

    private static Stream<Arguments> getJenkinsIntegrationTestData() {
        return Stream.of(
            Arguments.of("jenkins-task", "CreateJenkinsTaskTyped", 
                "org.waveywaves.jenkins.plugins.tekton.generated.jenkinstasks.v1"),
            Arguments.of("jenkins-pipeline", "CreateJenkinsPipelineTyped", 
                "org.waveywaves.jenkins.plugins.tekton.generated.jenkinspipelines.v1"),
            Arguments.of("jenkins-edge-case", "CreateJenkinsEdgeCaseTyped", 
                "org.waveywaves.jenkins.plugins.tekton.generated.jenkinsedgecases.v1")
        );
    }

    private Path findGeneratedStepClass(String className) throws IOException {
        return findGeneratedStepClass(className, null);
    }

    private Path findGeneratedStepClass(String className, String version) throws IOException {
        Stream<Path> pathStream = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().equals(className + ".java"));
        
        if (version != null) {
            pathStream = pathStream.filter(p -> p.toString().contains("/" + version + "/"));
        }
        
        return pathStream.findFirst()
            .orElseThrow(() -> new AssertionError("Generated class not found: " + className + 
                (version != null ? " (version: " + version + ")" : "")));
    }

    // Mock CRD creation methods for Jenkins integration testing
    private String createJenkinsTaskCrd() {
        return """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: jenkinstasks.tekton.dev
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
                                  enum: ["string", "array", "object"]
                                default:
                                  oneOf:
                                  - type: string
                                  - type: array
                                  - type: object
                                description:
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
                                args:
                                  type: array
                                  items:
                                    type: string
                                env:
                                  type: array
                                  items:
                                    type: object
                                    properties:
                                      name:
                                        type: string
                                      value:
                                        type: string
              scope: Namespaced
              names:
                plural: jenkinstasks
                singular: jenkinstask
                kind: JenkinsTask
            """;
    }

    private String createJenkinsPipelineCrd() {
        return """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: jenkinspipelines.tekton.dev
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
                                  oneOf:
                                  - type: string
                                  - type: array
                          tasks:
                            type: array
                            items:
                              type: object
                              properties:
                                name:
                                  type: string
                                taskRef:
                                  type: object
                                  properties:
                                    name:
                                      type: string
                                    kind:
                                      type: string
                                params:
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
                          tasks:
                            type: array
                            items:
                              type: object
              scope: Namespaced
              names:
                plural: jenkinspipelines
                singular: jenkinspipeline
                kind: JenkinsPipeline
            """;
    }

    private String createJenkinsEdgeCaseCrd() {
        return """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: jenkinsedgecases.tekton.dev
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
                          "special-field-with-dashes":
                            type: string
                          "field_with_underscores":
                            type: string
                          "nested-complex-object":
                            type: object
                            properties:
                              "deep-nested-field":
                                type: object
                                properties:
                                  "very-deep-array":
                                    type: array
                                    items:
                                      type: object
                                      properties:
                                        "complex-item":
                                          type: string
                          "array-of-complex-objects":
                            type: array
                            items:
                              type: object
                              properties:
                                "item-name":
                                  type: string
                                "item-config":
                                  type: object
                                  additionalProperties:
                                    type: string
              scope: Namespaced
              names:
                plural: jenkinsedgecases
                singular: jenkinsedgecase
                kind: JenkinsEdgeCase
            """;
    }
}
