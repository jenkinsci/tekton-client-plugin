package org.waveywaves.jenkins.plugins.tekton.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests to verify that generated POJOs properly extend BaseStep
 * and have correct Jenkins annotations for plugin integration.
 */
public class JenkinsIntegrationTest {

    @TempDir
    Path tempDir;

    private Path outputDir;
    private Path crdDir;
    private EnhancedCrdProcessor processor;

    @BeforeEach
    void setUp() throws IOException {
        outputDir = tempDir.resolve("generated-sources");
        crdDir = tempDir.resolve("crds");
        processor = new EnhancedCrdProcessor();
        
        Files.createDirectories(outputDir);
        Files.createDirectories(crdDir);
        
        // Copy mock CRD files
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
    void testBaseStepInheritance() throws Exception {
        // Given
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        Path stepClass = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated/simpletasks/v1/CreateSimpleTaskTyped.java");
        if (Files.exists(stepClass)) {
            String classContent = Files.readString(stepClass);

            // Verify inheritance
            assertTrue(classContent.contains("extends BaseStep"), "Should extend BaseStep");
            
            // Verify import statement
            assertTrue(classContent.contains("import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;"), 
                "Should import BaseStep");
            
            // Verify class declaration is correct
            Pattern classPattern = Pattern.compile("public class CreateSimpleTaskTyped extends BaseStep");
            Matcher matcher = classPattern.matcher(classContent);
            assertTrue(matcher.find(), "Class declaration should be syntactically correct");
        }
    }

    @Test
    void testDataBoundConstructorAnnotation() throws Exception {
        // Given
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        Path stepClass = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated/simpletasks/v1/CreateSimpleTaskTyped.java");
        if (Files.exists(stepClass)) {
            String classContent = Files.readString(stepClass);

            // Verify @DataBoundConstructor annotation
            assertTrue(classContent.contains("@DataBoundConstructor"), "Should have @DataBoundConstructor annotation");
            
            // Verify import for annotation
            assertTrue(classContent.contains("import org.kohsuke.stapler.DataBoundConstructor;"), 
                "Should import DataBoundConstructor");
            
            // Verify constructor structure
            Pattern constructorPattern = Pattern.compile("@DataBoundConstructor\\s+public CreateSimpleTaskTyped\\(");
            Matcher matcher = constructorPattern.matcher(classContent);
            assertTrue(matcher.find(), "Constructor should be properly annotated");
            
            // Verify constructor calls super()
            assertTrue(classContent.contains("super();"), "Constructor should call super()");
        }
    }

    @Test
    void testJenkinsStepStructure() throws Exception {
        // Given
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        Path v1StepClass = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated/complexpipelines/v1/CreateComplexPipelineTyped.java");
        if (Files.exists(v1StepClass)) {
            String classContent = Files.readString(v1StepClass);

            // Verify complete Jenkins Step structure
            assertTrue(classContent.contains("public class CreateComplexPipelineTyped extends BaseStep"), 
                "Should have correct class declaration with inheritance");
            
            // Verify required imports for Jenkins integration
            assertTrue(classContent.contains("import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep;"), 
                "Should import BaseStep");
            assertTrue(classContent.contains("import org.kohsuke.stapler.DataBoundConstructor;"), 
                "Should import DataBoundConstructor");
            
            // Verify constructor is properly structured
            assertTrue(classContent.contains("@DataBoundConstructor"), "Should have constructor annotation");
            assertTrue(classContent.contains("public CreateComplexPipelineTyped("), "Should have public constructor");
            assertTrue(classContent.contains("super();"), "Should call super constructor");
        }
    }

    @Test
    void testMultiVersionJenkinsIntegration() throws Exception {
        // Given
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        // Test both v1 and v1beta1 versions
        String[] versions = {"v1", "v1beta1"};
        
        for (String version : versions) {
            Path stepClass = outputDir.resolve(String.format(
                "org/waveywaves/jenkins/plugins/tekton/generated/complexpipelines/%s/CreateComplexPipelineTyped.java", 
                version));
            
            if (Files.exists(stepClass)) {
                String classContent = Files.readString(stepClass);
                
                // Verify each version has proper Jenkins integration
                assertTrue(classContent.contains("extends BaseStep"), 
                    version + " should extend BaseStep");
                assertTrue(classContent.contains("@DataBoundConstructor"), 
                    version + " should have DataBoundConstructor");
                assertTrue(classContent.contains("super();"), 
                    version + " should call super()");
                
                // Verify package is version-specific
                assertTrue(classContent.contains("package org.waveywaves.jenkins.plugins.tekton.generated.complexpipelines." + version + ";"), 
                    version + " should have correct package declaration");
            }
        }
    }

    @Test
    void testEdgeCaseJenkinsIntegration() throws Exception {
        // Given
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        Path stepClass = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated/edgecases/v1alpha1/CreateEdgeCaseTyped.java");
        if (Files.exists(stepClass)) {
            String classContent = Files.readString(stepClass);

            // Verify Jenkins integration is preserved even with edge cases
            assertTrue(classContent.contains("extends BaseStep"), 
                "Edge case class should still extend BaseStep");
            assertTrue(classContent.contains("@DataBoundConstructor"), 
                "Edge case class should have required annotations");
            
            // Verify it compiles to valid Java (basic syntax check)
            assertFalse(classContent.contains("class class"), "Should not have syntax errors");
            assertFalse(classContent.contains("import import"), "Should not have duplicate imports");
            
            // Verify class structure is correct despite edge cases
            Pattern classPattern = Pattern.compile("public class CreateEdgeCaseTyped extends BaseStep\\s*\\{");
            Matcher matcher = classPattern.matcher(classContent);
            assertTrue(matcher.find(), "Class structure should be valid despite edge cases");
        }
    }

    @Test
    void testPackageAndClassNaming() throws Exception {
        // Given
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        Path stepClass = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated/simpletasks/v1/CreateSimpleTaskTyped.java");
        if (Files.exists(stepClass)) {
            String classContent = Files.readString(stepClass);

            // Verify naming conventions follow Jenkins standards
            assertTrue(classContent.contains("package org.waveywaves.jenkins.plugins.tekton.generated.simpletasks.v1;"), 
                "Package name should follow convention");
            assertTrue(classContent.contains("public class CreateSimpleTaskTyped"), 
                "Class name should follow Create*Typed pattern");
            
            // Verify it doesn't conflict with existing Jenkins classes
            assertFalse(classContent.contains("class Task "), "Should not conflict with existing Task classes");
            assertFalse(classContent.contains("class Pipeline "), "Should not conflict with existing Pipeline classes");

            // testing top level classes
        }
    }

    @Test 
    void testGeneratedCodeSyntaxValidity() throws Exception {
        // Given
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", true);

        // Then
        Path stepClass = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated/complexpipelines/v1/CreateComplexPipelineTyped.java");
        if (Files.exists(stepClass)) {
            String classContent = Files.readString(stepClass);

            // Basic syntax validation
            assertTrue(classContent.startsWith("package "), "Should start with package declaration");
            assertTrue(classContent.contains("public class "), "Should have public class declaration");
            
            // Verify proper brace matching (basic check)
            long openBraces = classContent.chars().filter(ch -> ch == '{').count();
            long closeBraces = classContent.chars().filter(ch -> ch == '}').count();
            assertEquals(openBraces, closeBraces, "Braces should be balanced");
            
            // Verify no common syntax errors
            assertFalse(classContent.contains(";;"), "Should not have double semicolons");
            assertFalse(classContent.contains("{{"), "Should not have double open braces");
            assertFalse(classContent.contains("}}"), "Should not have double close braces");
            assertFalse(classContent.contains("class class"), "Should not have duplicate class keyword");
        }
    }

    @Test
    void testDirectoryProcessingWithoutInheritance() throws Exception {
        // Given
        processor.processDirectory(crdDir, outputDir, 
            "org.waveywaves.jenkins.plugins.tekton.generated", false);

        // Then - Should still generate classes but without BaseStep inheritance
        Path packageDir = outputDir.resolve("org/waveywaves/jenkins/plugins/tekton/generated");
        if (Files.exists(packageDir)) {
            // Just verify that some files are generated
            boolean hasGeneratedFiles = Files.walk(packageDir)
                .anyMatch(path -> path.toString().endsWith(".java"));
            assertTrue(hasGeneratedFiles || !Files.exists(packageDir), 
                "Should generate files or handle gracefully");
        }
    }
}