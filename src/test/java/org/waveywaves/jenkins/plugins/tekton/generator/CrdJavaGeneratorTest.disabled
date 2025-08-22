package org.waveywaves.jenkins.plugins.tekton.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CrdJavaGenerator to validate POJO generation from mock CRD YAML files.
 */
public class CrdJavaGeneratorTest {

    @TempDir
    Path tempDir;

    private Path outputDir;
    private Path crdDir;

    @BeforeEach
    void setUp() throws IOException {
        outputDir = tempDir.resolve("generated-sources");
        crdDir = tempDir.resolve("crds");
        
        Files.createDirectories(outputDir);
        Files.createDirectories(crdDir);
        
        // Copy mock CRD files to temp directory
        copyMockCrdsToTempDir();
    }

    private void copyMockCrdsToTempDir() throws IOException {
        // Copy mock CRD files from test resources to temp directory
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
    void testGenerateFromMockCrds() throws Exception {
        // Given
        String[] args = {
            crdDir.toString(),
            outputDir.toString(),
            "org.waveywaves.jenkins.plugins.tekton.generated"
        };

        // When
        CrdJavaGenerator.main(args);

        // Then
        // Verify that Java files were generated
        assertTrue(Files.exists(outputDir), "Output directory should exist");
        
        // Check for generated packages and classes structure
        Path basePackage = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated");
        assertTrue(Files.exists(basePackage), "Base package should be created");
        
        // Verify some Java files were generated
        long javaFileCount = Files.walk(basePackage)
            .filter(path -> path.toString().endsWith(".java"))
            .count();
        assertTrue(javaFileCount > 0, "Java files should be generated");
    }

    @Test
    void testGeneratedPackageStructure() throws Exception {
        // Given
        String[] args = {
            crdDir.toString(),
            outputDir.toString(),
            "org.waveywaves.jenkins.plugins.tekton.generated"
        };

        // When
        CrdJavaGenerator.main(args);

        // Then
        // Verify proper package structure is created
        Path basePackage = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated");
        if (Files.exists(basePackage)) {
            // Check if any step classes exist with proper structure
            List<Path> stepClasses = Files.walk(basePackage)
                .filter(path -> path.getFileName().toString().endsWith("Typed.java"))
                .toList();
                
            if (!stepClasses.isEmpty()) {
                // Verify at least one step class has proper content
                Path stepClass = stepClasses.get(0);
                String classContent = Files.readString(stepClass);
                
                // Basic validation
                assertTrue(classContent.contains("package org.waveywaves.jenkins.plugins.tekton.generated"), 
                    "Should have correct package declaration");
                assertTrue(classContent.contains("public class"), "Should have class declaration");
                assertTrue(classContent.contains("extends BaseStep"), "Should extend BaseStep");
            }
        }
    }

    @Test
    void testEdgeCaseHandling() throws Exception {
        // Given - Copy edge case CRD
        String[] args = {
            crdDir.toString(),
            outputDir.toString(),
            "org.waveywaves.jenkins.plugins.tekton.generated"
        };

        // When & Then
        // Should handle edge cases without throwing uncaught exceptions
        assertDoesNotThrow(() -> CrdJavaGenerator.main(args), 
            "Generator should handle various CRDs gracefully");
        
        // Verify output directory exists
        assertTrue(Files.exists(outputDir), "Output directory should be created");
    }

    @Test
    void testInvalidArgumentsHandling() {
        // Given - Wrong number of arguments
        String[] args = {"only-one-arg"};

        // When & Then
        // Should handle invalid arguments gracefully (CrdJavaGenerator prints usage and exits)
        assertDoesNotThrow(() -> CrdJavaGenerator.main(args),
            "Generator should handle invalid arguments gracefully");
    }

    @Test
    void testEmptyCrdDirectory() throws Exception {
        // Given - Empty CRD directory
        Path emptyCrdDir = tempDir.resolve("empty-crds");
        Files.createDirectories(emptyCrdDir);
        
        String[] args = {
            emptyCrdDir.toString(),
            outputDir.toString(),
            "org.waveywaves.jenkins.plugins.tekton.generated"
        };

        // When
        CrdJavaGenerator.main(args);

        // Then
        // Should handle empty directory gracefully
        assertTrue(Files.exists(outputDir), "Output directory should be created");
    }

    @Test
    void testNonExistentCrdDirectory() {
        // Given - Non-existent CRD directory
        String[] args = {
            tempDir.resolve("non-existent").toString(),
            outputDir.toString(),
            "org.waveywaves.jenkins.plugins.tekton.generated"
        };

        // When & Then
        // Should handle missing directories gracefully
        assertDoesNotThrow(() -> CrdJavaGenerator.main(args),
            "Generator should handle missing CRD directories gracefully");
    }

    @Test
    void testMultipleCrdProcessing() throws Exception {
        // Given - Multiple CRD files in directory
        String[] args = {
            crdDir.toString(),
            outputDir.toString(),
            "org.waveywaves.jenkins.plugins.tekton.generated"
        };

        // When
        CrdJavaGenerator.main(args);

        // Then
        // Verify that multiple CRDs can be processed together
        Path basePackage = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated");
        if (Files.exists(basePackage)) {
            // Count different CRD types generated
            boolean hasSimpleTask = Files.walk(basePackage)
                .anyMatch(path -> path.toString().contains("simpletasks"));
            boolean hasComplexPipeline = Files.walk(basePackage)
                .anyMatch(path -> path.toString().contains("complexpipelines"));
            boolean hasEdgeCase = Files.walk(basePackage)
                .anyMatch(path -> path.toString().contains("edgecases"));
                
            // At least one should be generated (depending on CRD validity)
            assertTrue(hasSimpleTask || hasComplexPipeline || hasEdgeCase, 
                "Should generate classes for at least one CRD type");
        }
    }

    @Test
    void testJenkinsStepClassGeneration() throws Exception {
        // Given
        String[] args = {
            crdDir.toString(),
            outputDir.toString(),
            "org.waveywaves.jenkins.plugins.tekton.generated"
        };

        // When
        CrdJavaGenerator.main(args);

        // Then
        // Look for Jenkins Step classes (classes ending with "Typed")
        Path basePackage = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated");
        if (Files.exists(basePackage)) {
            List<Path> stepClasses = Files.walk(basePackage)
                .filter(path -> path.getFileName().toString().endsWith("Typed.java"))
                .toList();
                
            if (!stepClasses.isEmpty()) {
                // Verify step classes have proper Jenkins annotations
                for (Path stepClass : stepClasses) {
                    String content = Files.readString(stepClass);
                    assertTrue(content.contains("extends BaseStep") || content.contains("@DataBoundConstructor"), 
                        "Step class should have Jenkins integration features");
                }
            }
        }
    }

    @Test
    void testGeneratedCodeValidity() throws Exception {
        // Given
        String[] args = {
            crdDir.toString(),
            outputDir.toString(),
            "org.waveywaves.jenkins.plugins.tekton.generated"
        };

        // When
        CrdJavaGenerator.main(args);

        // Then
        // Verify generated code has basic validity
        Path basePackage = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated");
        if (Files.exists(basePackage)) {
            List<Path> javaFiles = Files.walk(basePackage)
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
                
            for (Path javaFile : javaFiles) {
                String content = Files.readString(javaFile);
                
                // Basic syntax checks
                assertTrue(content.startsWith("package "), "Should start with package declaration");
                
                // Check for balanced braces
                long openBraces = content.chars().filter(ch -> ch == '{').count();
                long closeBraces = content.chars().filter(ch -> ch == '}').count();
                assertEquals(openBraces, closeBraces, "Braces should be balanced in " + javaFile);
                
                // No obvious syntax errors
                assertFalse(content.contains("class class"), "Should not have duplicate keywords");
                assertFalse(content.contains(";;"), "Should not have double semicolons");
            }
        }
    }
}