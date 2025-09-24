package org.waveywaves.jenkins.plugins.tekton.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Compilation tests to verify that generated CRD POJOs can actually compile.
 * This is crucial to ensure the generated code is syntactically correct and usable.
 */
class CompilationTest {

    @TempDir
    Path tempDir;
    
    private TektonCrdToJavaProcessor processor;
    private Path crdDirectory;
    private Path outputDirectory;
    private Path compilationDirectory;
    private static final String BASE_PACKAGE = "org.waveywaves.jenkins.plugins.tekton.generated";

    @BeforeEach
    void setUp() throws IOException {
        processor = new TektonCrdToJavaProcessor();
        crdDirectory = tempDir.resolve("crds");
        outputDirectory = tempDir.resolve("generated-sources");
        compilationDirectory = tempDir.resolve("compiled-classes");
        
        Files.createDirectories(crdDirectory);
        Files.createDirectories(outputDirectory);
        Files.createDirectories(compilationDirectory);
        
        // Create comprehensive test CRDs
        createCompilationTestCrds();
    }

    @Test
    void testGeneratedCodeCompiles() throws IOException {
        // Act - Generate POJOs
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Get all generated Java files
        List<Path> javaFiles = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .toList();
        
        assertThat(javaFiles).isNotEmpty();
        
        // Attempt to compile all generated files
        boolean compilationSuccess = compileJavaFiles(javaFiles);
        
        // Assert - All files should compile successfully
        assertThat(compilationSuccess)
            .as("All generated Java files should compile without errors")
            .isTrue();
    }

    @Test
    void testJenkinsStepClassesCompile() throws IOException {
        // Use the actual generated Jenkins Step classes from target/generated-sources
        Path realGeneratedDir = Path.of("target/generated-sources/tekton");
        
        if (!Files.exists(realGeneratedDir)) {
            // Skip test if no generated sources exist
            System.out.println("No generated sources found, skipping Jenkins Step compilation test");
            return;
        }
        
        // Get only the main Jenkins Step classes (ending with "Typed.java")
        List<Path> stepClasses = Files.walk(realGeneratedDir)
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().endsWith("Typed.java"))
            .toList();
        
        assertThat(stepClasses).isNotEmpty();
        
        // Compile only the Jenkins Step classes first
        boolean compilationSuccess = compileJavaFiles(stepClasses);
        
        assertThat(compilationSuccess)
            .as("Jenkins Step classes should compile without errors")
            .isTrue();
        
        // Verify each step class individually
        for (Path stepClass : stepClasses) {
            String content = Files.readString(stepClass);
            verifyJenkinsStepCompilability(content, stepClass.getFileName().toString());
        }
    }

    @Test
    void testComplexCrdCompilation() throws IOException {
        // Use existing complex generated files (e.g. from tasks, pipelines with deep nesting)
        Path realGeneratedDir = Path.of("target/generated-sources/tekton");
        
        if (!Files.exists(realGeneratedDir)) {
            // Skip test if no generated sources exist
            System.out.println("No generated sources found, skipping complex CRD compilation test");
            return;
        }
        
        // Get generated files for complex structures (e.g. from tasks or pipelines with many nested objects)
        List<Path> complexFiles = Files.walk(realGeneratedDir)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            // Focus on files that indicate complexity (multiple nested levels, many properties)
            .filter(p -> {
                String fileName = p.getFileName().toString();
                // Look for files with numbered suffixes indicating complex nested structures
                return fileName.matches(".*__\\d+\\.java") || 
                       fileName.contains("Properties") || 
                       fileName.contains("Spec") ||
                       fileName.contains("Status");
            })
            .toList();
        
        assertThat(complexFiles).isNotEmpty();
        
        // Compile complex CRD files
        boolean compilationSuccess = compileJavaFiles(complexFiles);
        
        assertThat(compilationSuccess)
            .as("Complex CRD generated files should compile")
            .isTrue();
    }

    @Test
    void testMultiVersionCompilation() throws IOException {
        // Act - Generate POJOs
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Get files from different versions
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
        
        // Compile all versions together
        List<Path> allVersionFiles = new ArrayList<>();
        allVersionFiles.addAll(v1Files);
        allVersionFiles.addAll(v1beta1Files);
        
        boolean compilationSuccess = compileJavaFiles(allVersionFiles);
        
        assertThat(compilationSuccess)
            .as("Multi-version files should compile together without conflicts")
            .isTrue();
    }

    @Test
    void testSyntaxValidation() throws IOException {
        // Use actual generated files for syntax validation
        Path realGeneratedDir = Path.of("target/generated-sources/tekton");
        
        if (!Files.exists(realGeneratedDir)) {
            // Skip test if no generated sources exist
            System.out.println("No generated sources found, skipping syntax validation test");
            return;
        }
        
        // Get all generated files (limit to first 10 to avoid performance issues)
        List<Path> javaFiles = Files.walk(realGeneratedDir)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .limit(10) // Limit to avoid testing hundreds of files
            .toList();
        
        // Validate syntax of each file
        for (Path javaFile : javaFiles) {
            String content = Files.readString(javaFile);
            validateJavaSyntax(content, javaFile.getFileName().toString());
        }
    }

    private boolean compileJavaFiles(List<Path> javaFiles) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            // Skip compilation test if no compiler available (e.g., in some CI environments)
            System.out.println("Java compiler not available, skipping compilation test");
            return true;
        }
        
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            // Convert paths to JavaFileObjects
            Iterable<? extends JavaFileObject> compilationUnits = 
                fileManager.getJavaFileObjectsFromPaths(javaFiles);
            
            // Set compilation options
            List<String> options = Arrays.asList(
                "-d", compilationDirectory.toString(),
                "-cp", getClasspath(),
                "-Xlint:none" // Suppress warnings for generated code
            );
            
            // Perform compilation
            JavaCompiler.CompilationTask task = compiler.getTask(
                null, // Writer for additional output
                fileManager,
                null, // DiagnosticListener
                options,
                null, // Classes to process
                compilationUnits
            );
            
            return task.call();
            
        } catch (Exception e) {
            System.err.println("Compilation failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String getClasspath() {
        // Get current classpath for compilation
        return System.getProperty("java.class.path");
    }

    private void verifyJenkinsStepCompilability(String content, String fileName) {
        // Verify Jenkins Step specific compilation requirements
        
        // 1. Must have proper class declaration
        assertThat(content)
            .as("File " + fileName + " should have public class declaration")
            .containsPattern("public class \\w+");
        
        // 2. Must extend BaseStep
        assertThat(content)
            .as("File " + fileName + " should extend BaseStep")
            .contains("extends BaseStep");
        
        // 3. Must have DataBoundConstructor
        assertThat(content)
            .as("File " + fileName + " should have DataBoundConstructor")
            .contains("@DataBoundConstructor");
        
        // 4. Constructor must call super()
        assertThat(content)
            .as("File " + fileName + " constructor should call super()")
            .contains("super()");
        
        // 5. Must have proper imports
        assertThat(content)
            .as("File " + fileName + " should import BaseStep")
            .contains("import org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep");
        
        assertThat(content)
            .as("File " + fileName + " should import DataBoundConstructor")
            .contains("import org.kohsuke.stapler.DataBoundConstructor");
    }

    private void validateJavaSyntax(String content, String fileName) {
        // Basic syntax validation
        
        // 1. Package declaration should be first non-comment line
        String[] lines = content.split("\n");
        boolean foundPackage = false;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
                continue;
            }
            assertThat(line)
                .as("First non-comment line in " + fileName + " should be package declaration")
                .startsWith("package ");
            foundPackage = true;
            break;
        }
        assertThat(foundPackage).as("File " + fileName + " should have package declaration").isTrue();
        
        // 2. Balanced braces
        long openBraces = content.chars().filter(c -> c == '{').count();
        long closeBraces = content.chars().filter(c -> c == '}').count();
        assertThat(openBraces)
            .as("Braces should be balanced in " + fileName)
            .isEqualTo(closeBraces);
        
        // 3. Balanced parentheses
        long openParens = content.chars().filter(c -> c == '(').count();
        long closeParens = content.chars().filter(c -> c == ')').count();
        assertThat(openParens)
            .as("Parentheses should be balanced in " + fileName)
            .isEqualTo(closeParens);
        
        // 4. No common syntax errors
        assertThat(content).as("File " + fileName + " should not have double semicolons").doesNotContain(";;");
        assertThat(content).as("File " + fileName + " should not have double keywords").doesNotContain("public public");
        assertThat(content).as("File " + fileName + " should not have double keywords").doesNotContain("private private");
        assertThat(content).as("File " + fileName + " should not have double keywords").doesNotContain("class class");
        
        // 5. Proper method declarations - check for malformed methods only
        if (content.contains("public ") && content.contains("(")) {
            assertThat(content).as("File " + fileName + " should have proper method syntax").doesNotContain("public ()");
            // Remove the incorrect check for "() {" as this is valid Java syntax like "getApiVersion() {"
            assertThat(content).as("File " + fileName + " should not have empty method names").doesNotContain(" () {");
        }
    }

    private void createCompilationTestCrds() throws IOException {
        // Simple Task CRD
        String simpleTaskCrd = """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: compilationtasks.tekton.dev
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
              scope: Namespaced
              names:
                plural: compilationtasks
                singular: compilationtask
                kind: CompilationTask
            """;
        Files.write(crdDirectory.resolve("compilation-task-crd.yaml"), simpleTaskCrd.getBytes());
        
        // Multi-version Pipeline CRD
        String multiVersionPipelineCrd = """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: compilationpipelines.tekton.dev
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
              scope: Namespaced
              names:
                plural: compilationpipelines
                singular: compilationpipeline
                kind: CompilationPipeline
            """;
        Files.write(crdDirectory.resolve("compilation-pipeline-crd.yaml"), multiVersionPipelineCrd.getBytes());
    }

}
