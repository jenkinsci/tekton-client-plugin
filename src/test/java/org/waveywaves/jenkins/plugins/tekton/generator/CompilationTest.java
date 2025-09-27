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
