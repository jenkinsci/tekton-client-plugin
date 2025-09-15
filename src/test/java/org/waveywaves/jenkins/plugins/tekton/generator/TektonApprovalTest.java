package org.waveywaves.jenkins.plugins.tekton.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
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
 * Approval-style tests for CRD to POJO generation.
 * Similar to fabric8's approach but focused on Tekton Jenkins integration.
 * 
 * These tests capture the expected output structure and verify consistency
 * across different CRD types and versions.
 */
class TektonApprovalTest {

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
    }

    @ParameterizedTest
    @MethodSource("getCRDGenerationInputData")
    void generate_withValidCrd_shouldGenerateExpectedStructure(
            String testName, 
            String crdYaml, 
            String expectedMainClassName, 
            String approvalLabel) throws IOException {
        
        // Arrange
        Files.write(crdDirectory.resolve(testName + "-crd.yaml"), crdYaml.getBytes());
        
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Assert
        List<Path> generatedFiles = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .toList();
        
        assertThat(generatedFiles).isNotEmpty();
        
        // Find the main class
        Path mainClass = findMainClass(generatedFiles, expectedMainClassName);
        assertThat(mainClass).exists();
        
        String mainClassContent = Files.readString(mainClass);
        
        // Verify expected structure
        verifyJenkinsStepStructure(mainClassContent, expectedMainClassName);
        verifyParameterHandling(mainClassContent);
        verifyJacksonAnnotations(mainClassContent);
        verifyConstructorStructure(mainClassContent, expectedMainClassName);
        
        // Additional verification for specific test cases
        switch (approvalLabel) {
            case "TaskJavaStep" -> verifyTaskSpecificFeatures(mainClassContent);
            case "PipelineJavaStep" -> verifyPipelineSpecificFeatures(mainClassContent);
            case "ComplexJavaStep" -> verifyComplexObjectHandling(mainClassContent);
        }
    }

    private static Stream<Arguments> getCRDGenerationInputData() {
        return Stream.of(
            Arguments.of("tekton-task", createTektonTaskCrd(), "CreateTaskTyped", "TaskJavaStep"),
            Arguments.of("tekton-pipeline", createTektonPipelineCrd(), "CreatePipelineTyped", "PipelineJavaStep"),
            Arguments.of("tekton-taskrun", createTektonTaskRunCrd(), "CreateTaskRunTyped", "TaskRunJavaStep"),
            Arguments.of("complex-crd", createComplexCrd(), "CreateCustomRunTyped", "ComplexJavaStep")
        );
    }

    private void verifyJenkinsStepStructure(String content, String className) {
        // Jenkins Step inheritance
        assertThat(content).contains("extends BaseStep");
        assertThat(content).contains("import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep");
        
        // DataBound constructor
        assertThat(content).contains("@DataBoundConstructor");
        assertThat(content).contains("import org.kohsuke.stapler.DataBoundConstructor");
        
        // Class declaration
        assertThat(content).contains("public class " + className);
        
        // Super call in constructor
        assertThat(content).containsPattern("@DataBoundConstructor\\s+public " + className);
        assertThat(content).contains("super()");
    }

    private void verifyParameterHandling(String content) {
        // Check that parameters are properly handled
        if (content.contains("param") || content.contains("Param")) {
            // Parameters should have proper getters/setters
            if (content.contains("private") && content.contains("param")) {
                // Should have getter methods
                assertThat(content).containsPattern("public .* get[A-Z].*\\(\\)");
            }
        }
        
        // Check for common parameter issues
        assertThat(content).doesNotContain("param param"); // No duplicate words
        assertThat(content).doesNotContain("Param Param"); // No duplicate class names
    }

    private void verifyJacksonAnnotations(String content) {
        // Jackson annotations for JSON serialization
        assertThat(content).contains("@JsonInclude");
        assertThat(content).contains("@JsonPropertyOrder");
        
        // Import statements
        assertThat(content).contains("import com.fasterxml.jackson.annotation");
    }

    private void verifyConstructorStructure(String content, String className) {
        // Constructor should exist and be properly annotated
        assertThat(content).containsPattern("@DataBoundConstructor\\s+public " + className + "\\s*\\(");
        
        // Constructor should call super()
        assertThat(content).contains("super()");
        
        // Constructor should not have syntax errors
        assertThat(content).doesNotContain("public public"); // No duplicate keywords
        assertThat(content).doesNotContain("constructor constructor"); // No duplicate words
    }

    private void verifyTaskSpecificFeatures(String content) {
        // Task-specific verification
        if (content.contains("step") || content.contains("Step")) {
            // Should handle steps properly
            assertThat(content).doesNotContain("step step");
        }
    }

    private void verifyPipelineSpecificFeatures(String content) {
        // Pipeline-specific verification
        if (content.contains("task") || content.contains("Task")) {
            // Should handle tasks in pipeline properly
            assertThat(content).doesNotContain("task task");
        }
    }

    private void verifyComplexObjectHandling(String content) {
        // Complex object handling
        assertThat(content).doesNotContain("object object");
        assertThat(content).doesNotContain("Object Object");
        
        // Should not have malformed nested class names
        assertThat(content).doesNotContain("class class");
    }

    private Path findMainClass(List<Path> generatedFiles, String expectedClassName) {
        return generatedFiles.stream()
            .filter(p -> p.getFileName().toString().equals(expectedClassName + ".java"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Main class not found: " + expectedClassName));
    }

    // Static CRD creation methods
    private static String createTektonTaskCrd() {
        return """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: tektontasks.tekton.dev
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
                                    items:
                                      type: string
                                  - type: object
                                description:
                                  type: string
                          results:
                            type: array
                            items:
                              type: object
                              properties:
                                name:
                                  type: string
                                type:
                                  type: string
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
                                script:
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
                          workspaces:
                            type: array
                            items:
                              type: object
                              properties:
                                name:
                                  type: string
                                description:
                                  type: string
                                mountPath:
                                  type: string
                                readOnly:
                                  type: boolean
              scope: Namespaced
              names:
                plural: tektontasks
                singular: tektontask
                kind: TektonTask
            """;
    }

    private static String createTektonPipelineCrd() {
        return """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: tektonpipelines.tekton.dev
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
                                  - type: object
                                description:
                                  type: string
                          results:
                            type: array
                            items:
                              type: object
                              properties:
                                name:
                                  type: string
                                description:
                                  type: string
                                value:
                                  type: string
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
                                      enum: ["Task", "ClusterTask"]
                                    apiVersion:
                                      type: string
                                taskSpec:
                                  type: object
                                params:
                                  type: array
                                  items:
                                    type: object
                                    properties:
                                      name:
                                        type: string
                                      value:
                                        oneOf:
                                        - type: string
                                        - type: array
                                        - type: object
                                runAfter:
                                  type: array
                                  items:
                                    type: string
                                workspaces:
                                  type: array
                                  items:
                                    type: object
                                    properties:
                                      name:
                                        type: string
                                      workspace:
                                        type: string
                          workspaces:
                            type: array
                            items:
                              type: object
                              properties:
                                name:
                                  type: string
                                description:
                                  type: string
              scope: Namespaced
              names:
                plural: tektonpipelines
                singular: tektonpipeline
                kind: TektonPipeline
            """;
    }

    private static String createTektonTaskRunCrd() {
        return """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: tektontaskruns.tekton.dev
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
                          taskRef:
                            type: object
                            properties:
                              name:
                                type: string
                              kind:
                                type: string
                              apiVersion:
                                type: string
                          taskSpec:
                            type: object
                          params:
                            type: array
                            items:
                              type: object
                              properties:
                                name:
                                  type: string
                                value:
                                  oneOf:
                                  - type: string
                                  - type: array
                                    items:
                                      type: string
                                  - type: object
                          serviceAccountName:
                            type: string
                          timeout:
                            type: string
                          workspaces:
                            type: array
                            items:
                              type: object
                              properties:
                                name:
                                  type: string
                                persistentVolumeClaim:
                                  type: object
                                  properties:
                                    claimName:
                                      type: string
                                emptyDir:
                                  type: object
                                configMap:
                                  type: object
                                  properties:
                                    name:
                                      type: string
                                secret:
                                  type: object
                                  properties:
                                    secretName:
                                      type: string
                      status:
                        type: object
                        properties:
                          conditions:
                            type: array
                            items:
                              type: object
                              properties:
                                type:
                                  type: string
                                status:
                                  type: string
                                reason:
                                  type: string
                                message:
                                  type: string
                          startTime:
                            type: string
                          completionTime:
                            type: string
                          results:
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
                plural: tektontaskruns
                singular: tektontaskrun
                kind: TektonTaskRun
            """;
    }

    private static String createComplexCrd() {
        return """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: complexcrds.tekton.dev
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
                          "complex-param":
                            type: object
                            properties:
                              "nested-array":
                                type: array
                                items:
                                  type: object
                                  properties:
                                    "deep-object":
                                      type: object
                                      properties:
                                        "very-deep-string":
                                          type: string
                                        "very-deep-array":
                                          type: array
                                          items:
                                            type: string
                          "array-of-objects":
                            type: array
                            items:
                              type: object
                              properties:
                                name:
                                  type: string
                                config:
                                  type: object
                                  additionalProperties:
                                    type: string
                                nested:
                                  type: object
                                  properties:
                                    level1:
                                      type: object
                                      properties:
                                        level2:
                                          type: object
                                          properties:
                                            level3:
                                              type: string
                          "union-type-field":
                            oneOf:
                            - type: string
                            - type: object
                              properties:
                                objectType:
                                  type: string
                            - type: array
                              items:
                                type: string
                          "map-like-field":
                            type: object
                            additionalProperties:
                              type: object
                              properties:
                                value:
                                  type: string
                                metadata:
                                  type: object
              scope: Namespaced
              names:
                plural: complexcrds
                singular: complexcrd
                kind: ComplexCrd
            """;
    }
}
