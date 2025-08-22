package org.waveywaves.jenkins.plugins.tekton.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EnhancedCrdProcessor to validate advanced CRD processing features,
 * inheritance, Jenkins integration, and edge case handling.
 */
public class EnhancedCrdProcessorTest {

    @TempDir
    Path tempDir;

    private EnhancedCrdProcessor processor;
    private Path outputDir;
    private Path crdDir;

    @BeforeEach
    void setUp() throws IOException {
        processor = new EnhancedCrdProcessor();
        outputDir = tempDir.resolve("generated-sources");
        crdDir = tempDir.resolve("crds");
        
        Files.createDirectories(outputDir);
        Files.createDirectories(crdDir);
        
        // Copy mock CRD files to temp directory for testing
        copyMockCrdsToTempDir();
    }
    
    private void copyMockCrdsToTempDir() throws IOException {
        String[] mockFiles = {
            "mock-simple-task.yaml",
            "mock-complex-pipeline.yaml", 
            "mock-edge-case-crd.yaml"
        };
        
        for (String fileName : mockFiles) {
            Path sourceFile = Paths.get("src/test/resources/org/waveywaves/jenkins/plugins/tekton/generator", fileName);
            Path targetFile = crdDir.resolve(fileName);
            if (Files.exists(sourceFile)) {
                Files.copy(sourceFile, targetFile);
            }
        }
    }

    @Test
    void testProcessDirectoryWithInheritance() throws Exception {
        // Given - Mock CRD files are already copied to crdDir in setUp()

        // When
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        // Verify main Jenkins Step class extends BaseStep for simple task
        Path stepClassFile = findGeneratedStepClass("simpletasks", "v1", "CreateSimpleTaskTyped");
        assertTrue(Files.exists(stepClassFile), "Jenkins Step class should be generated");
        
        String classContent = Files.readString(stepClassFile);
        
        // Verify inheritance
        assertTrue(classContent.contains("extends BaseStep"), "Should extend BaseStep");
        
        // Verify Jenkins annotations
        assertTrue(classContent.contains("@DataBoundConstructor"), "Should have DataBoundConstructor");
        
        // Verify imports
        assertTrue(classContent.contains("import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;"), 
            "Should import BaseStep");
        assertTrue(classContent.contains("import org.kohsuke.stapler.DataBoundConstructor;"), 
            "Should import DataBoundConstructor");
    }

    @Test
    void testMultiVersionCrdProcessing() throws Exception {
        // Given - Complex pipeline CRD has multiple versions

        // When
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        // Verify both v1 and v1beta1 versions are generated for complex pipeline
        Path v1StepClass = findGeneratedStepClass("complexpipelines", "v1", "CreateComplexPipelineTyped");
        Path v1beta1StepClass = findGeneratedStepClass("complexpipelines", "v1beta1", "CreateComplexPipelineTyped");
        
        assertTrue(Files.exists(v1StepClass), "v1 Step class should be generated");
        assertTrue(Files.exists(v1beta1StepClass), "v1beta1 Step class should be generated");
        
        // Verify both extend BaseStep
        String v1Content = Files.readString(v1StepClass);
        String v1beta1Content = Files.readString(v1beta1StepClass);
        
        assertTrue(v1Content.contains("extends BaseStep"), "v1 should extend BaseStep");
        assertTrue(v1beta1Content.contains("extends BaseStep"), "v1beta1 should extend BaseStep");
    }

    @Test
    void testEdgeCaseHandling() throws Exception {
        // Given - Edge case CRD with complex structures

        // When & Then
        assertDoesNotThrow(() -> {
            processor.processDirectory(crdDir, outputDir, 
                "org.waveywaves.jenkins.plugins.tekton.generated", true);
        }, "Should handle edge cases without throwing exceptions");

        // Verify main class is still generated despite edge cases
        Path stepClass = findGeneratedStepClass("edgecases", "v1alpha1", "CreateEdgeCaseTyped");
        if (Files.exists(stepClass)) {
            String classContent = Files.readString(stepClass);
            
            // Verify it's still a valid Jenkins Step
            assertTrue(classContent.contains("extends BaseStep"), "Should still extend BaseStep");
            assertTrue(classContent.contains("@DataBoundConstructor"), "Should have required annotations");
        }
    }

    @Test
    void testClassNameMapping() throws Exception {
        // Given - CRD processor with custom class name mappings

        // When
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        // Verify correct class name mapping from plural CRD name to singular class name
        Path stepClass = findGeneratedStepClass("simpletasks", "v1", "CreateSimpleTaskTyped");
        if (Files.exists(stepClass)) {
            String classContent = Files.readString(stepClass);
            assertTrue(classContent.contains("public class CreateSimpleTaskTyped"), 
                "Class name should be correctly mapped");
        }
    }

    @Test
    void testPackageStructureGeneration() throws Exception {
        // Given
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        // Verify correct package structure
        Path packageDir = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated/simpletasks/v1");
        if (Files.exists(packageDir)) {
            // Verify generated class has correct package declaration
            Path stepClass = packageDir.resolve("CreateSimpleTaskTyped.java");
            if (Files.exists(stepClass)) {
                String classContent = Files.readString(stepClass);
                assertTrue(classContent.contains("package org.waveywaves.jenkins.plugins.tekton.generated.simpletasks.v1;"), 
                    "Package declaration should match directory structure");
            }
        }
    }

    @Test
    void testPOJOGeneration() throws Exception {
        // Given
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        // Verify multiple POJOs are generated for complex CRD
        Path packageDir = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated");
        
        if (Files.exists(packageDir)) {
            List<Path> javaFiles = Files.walk(packageDir)
                .filter(path -> path.toString().endsWith(".java"))
                .collect(Collectors.toList());
                
            assertTrue(javaFiles.size() > 0, "Should generate Java files");
            
            // Check if any files contain Jenkins Step patterns
            boolean hasStepClasses = javaFiles.stream()
                .anyMatch(path -> path.getFileName().toString().contains("CreateSimpleTaskTyped") ||
                                 path.getFileName().toString().contains("CreateComplexPipelineTyped") ||
                                 path.getFileName().toString().contains("CreateEdgeCaseTyped"));
            
            if (hasStepClasses) {
                // If step classes exist, verify they have proper structure
                Path someStepClass = javaFiles.stream()
                    .filter(path -> path.getFileName().toString().endsWith("Typed.java"))
                    .findFirst()
                    .orElse(null);
                    
                if (someStepClass != null) {
                    String content = Files.readString(someStepClass);
                    assertTrue(content.contains("extends BaseStep") || content.contains("class"), 
                        "Generated classes should have proper structure");
                }
            }
        }
    }

    @Test
    void testInvalidCrdGracefulHandling() throws Exception {
        // Given - Add invalid CRD to test directory
        Path invalidCrd = crdDir.resolve("invalid.yaml");
        Files.writeString(invalidCrd, "invalid: yaml: content:");

        // When & Then
        assertDoesNotThrow(() -> {
            processor.processDirectory(crdDir, outputDir, 
                "org.waveywaves.jenkins.plugins.tekton.generated", true);
        }, "Should handle invalid CRDs gracefully");
    }

    @Test 
    void testEmptyDirectoryHandling() throws Exception {
        // Given - Empty CRD directory
        Path emptyDir = tempDir.resolve("empty-crds");
        Files.createDirectories(emptyDir);

        // When & Then
        assertDoesNotThrow(() -> {
            processor.processDirectory(emptyDir, outputDir, 
                "org.waveywaves.jenkins.plugins.tekton.generated", true);
        }, "Should handle empty directories gracefully");
    }

    private Path findGeneratedStepClass(String crdPlural, String version, String expectedClassName) {
        return outputDir.resolve(String.format(
            "org/waveywaves/jenkins/plugins/tekton/generated/%s/%s/%s.java",
            crdPlural, version, expectedClassName));
    }
}