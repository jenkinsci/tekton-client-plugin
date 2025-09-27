package org.waveywaves.jenkins.plugins.tekton.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        
        // Configure Jenkins integration like the main generator does
        TektonPojoGenerator.configureJenkinsIntegration(processor, BASE_PACKAGE);
        
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
    void testJenkinsStepStructure() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated Jenkins step class
        Path stepClass = findGeneratedStepClass("CreateJenkinstasksTyped");
        String content = Files.readString(stepClass).trim();
        
        // Assert - Full Jenkins Step structure
        assertThat(content).contains("public class CreateJenkinstasksTyped");
        assertThat(content).contains("extends BaseStep");
        assertThat(content).contains("@DataBoundConstructor");
        assertThat(content).contains("super()");
        
        // Required imports for Jenkins integration
        assertThat(content).contains("import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep");
        assertThat(content).contains("import org.kohsuke.stapler.DataBoundConstructor");
        
        // Jackson annotations for JSON serialization
        assertThat(content).contains("@JsonInclude");
        assertThat(content).contains("@JsonPropertyOrder");
    }

    @Test
    void testMultiVersionJenkinsIntegration() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find both version classes
        Path v1Class = findGeneratedStepClass("CreateJenkinspipelinesTyped", "v1");
        Path v1beta1Class = findGeneratedStepClass("CreateJenkinspipelinesTyped", "v1beta1");
        
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
    void testPackageAndClassNaming() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Check package naming convention
        Path taskClass = findGeneratedStepClass("CreateJenkinstasksTyped");
        String taskContent = Files.readString(taskClass);
        
        assertThat(taskContent).contains("package org.waveywaves.jenkins.plugins.tekton.generated.jenkinstasks.v1");
        assertThat(taskContent).contains("public class CreateJenkinstasksTyped");
        
        // Verify naming avoids conflicts with existing Jenkins classes
        assertThat(taskContent).doesNotContain("public class Task"); // Avoid conflict with Jenkins Task
        assertThat(taskContent).doesNotContain("public class Pipeline"); // Avoid conflict with Jenkins Pipeline
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
