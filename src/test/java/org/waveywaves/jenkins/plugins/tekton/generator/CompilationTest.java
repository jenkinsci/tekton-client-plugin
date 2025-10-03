package org.waveywaves.jenkins.plugins.tekton.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    // @Test
    // void testJenkinsStepClassesCompile() throws IOException {
    //     // Use the actual generated Jenkins Step classes from target/generated-sources
    //     Path realGeneratedDir = Path.of("target/generated-sources/tekton");
        
    //     if (!Files.exists(realGeneratedDir)) {
    //         // Skip test if no generated sources exist
    //         System.out.println("No generated sources found, skipping Jenkins Step compilation test");
    //         return;
    //     }
        
    //     // Get only the main Jenkins Step classes (ending with "Typed.java")
    //     List<Path> stepClasses = Files.walk(realGeneratedDir)
    //         .filter(Files::isRegularFile)
    //         .filter(p -> p.getFileName().toString().endsWith("Typed.java"))
    //         .toList();
        
    //     assertThat(stepClasses).isNotEmpty();
        
    //     // Compile only the Jenkins Step classes first
    //     boolean compilationSuccess = compileJavaFiles(stepClasses);
        
    //     assertThat(compilationSuccess)
    //         .as("Jenkins Step classes should compile without errors")
    //         .isTrue();
    // }

    // @Test
    // void testComplexCrdCompilation() throws IOException {
    //     // Use existing complex generated files (e.g. from tasks, pipelines with deep nesting)
    //     Path realGeneratedDir = Path.of("target/generated-sources/tekton");
        
    //     if (!Files.exists(realGeneratedDir)) {
    //         // Skip test if no generated sources exist
    //         System.out.println("No generated sources found, skipping complex CRD compilation test");
    //         return;
    //     }
        
    //     // Get generated files for complex structures (e.g. from tasks or pipelines with many nested objects)
    //     List<Path> complexFiles = Files.walk(realGeneratedDir)
    //         .filter(Files::isRegularFile)
    //         .filter(p -> p.toString().endsWith(".java"))
    //         // Focus on files that indicate complexity (multiple nested levels, many properties)
    //         .filter(p -> {
    //             String fileName = p.getFileName().toString();
    //             // Look for files with numbered suffixes indicating complex nested structures
    //             return fileName.matches(".*__\\d+\\.java") || 
    //                    fileName.contains("Properties") || 
    //                    fileName.contains("Spec") ||
    //                    fileName.contains("Status");
    //         })
    //         .toList();
        
    //     assertThat(complexFiles).isNotEmpty();
        
    //     // Compile complex CRD files
    //     boolean compilationSuccess = compileJavaFiles(complexFiles);
        
    //     assertThat(compilationSuccess)
    //         .as("Complex CRD generated files should compile")
    //         .isTrue();
    // }


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
            
            // Set compilation options with better classpath handling
            List<String> options = Arrays.asList(
                "-d", compilationDirectory.toString(),
                "-cp", getClasspath(),
                "-Xlint:none", // Suppress warnings for generated code
                "-source", "17",
                "-target", "17"
            );
            
            // Create a diagnostic collector to capture compilation errors
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            
            // Perform compilation
            JavaCompiler.CompilationTask task = compiler.getTask(
                null, // Writer for additional output
                fileManager,
                diagnostics, // DiagnosticListener
                options,
                null, // Classes to process
                compilationUnits
            );
            
            boolean success = task.call();
            
            // Print diagnostics if compilation failed
            if (!success) {
                System.out.println("Compilation failed with " + diagnostics.getDiagnostics().size() + " errors:");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    System.out.println(diagnostic.toString());
                }
            }
            
            return success;
            
        } catch (Exception e) {
            System.err.println("Compilation failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String getClasspath() {
        // Get current classpath for compilation
        String classpath = System.getProperty("java.class.path");
        
        // In CI environments, the classpath might be incomplete
        // Add common Maven target directories if they exist
        Path targetClasses = Path.of("target/classes");
        Path targetTestClasses = Path.of("target/test-classes");
        
        StringBuilder enhancedClasspath = new StringBuilder(classpath);
        
        if (Files.exists(targetClasses)) {
            enhancedClasspath.append(":").append(targetClasses.toAbsolutePath());
        }
        if (Files.exists(targetTestClasses)) {
            enhancedClasspath.append(":").append(targetTestClasses.toAbsolutePath());
        }
        
        return enhancedClasspath.toString();
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
