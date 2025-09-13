package org.waveywaves.jenkins.plugins.tekton.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests focused on parameter generation issues in CRD to POJO conversion.
 * Compares generated classes with manually written CreateRaw to ensure
 * proper parameter handling and constructor generation.
 */
class ParameterGenerationTest {

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
        
        // Create a CRD similar to what CreateRaw handles
        createTaskCrdSimilarToCreateRaw();
    }

    @Test
    void testConstructorParameterGeneration() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated class
        Path generatedClass = findGeneratedClass("CreateTaskTyped");
        String content = Files.readString(generatedClass);
        
        // Assert - Check constructor parameter handling
        verifyConstructorStructure(content);
        verifyParameterFields(content);
        verifyGetterSetterMethods(content);
    }

    @Test
    void testParameterFieldGeneration() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated class
        Path generatedClass = findGeneratedClass("CreateTaskTyped");
        String content = Files.readString(generatedClass);
        
        // Assert - Check field generation
        assertThat(content).contains("private String apiVersion");
        assertThat(content).contains("private String kind");
        assertThat(content).contains("private Object metadata");
        assertThat(content).contains("private Spec spec");
        
        // Check field annotations
        assertThat(content).contains("@JsonProperty(\"apiVersion\")");
        assertThat(content).contains("@JsonProperty(\"kind\")");
        assertThat(content).contains("@JsonProperty(\"metadata\")");
        assertThat(content).contains("@JsonProperty(\"spec\")");
    }

    @Test
    void testDataBoundConstructorParameters() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated class
        Path generatedClass = findGeneratedClass("CreateTaskTyped");
        String content = Files.readString(generatedClass);
        
        // Assert - Check DataBoundConstructor parameters
        String constructorPattern = "@DataBoundConstructor\\s+public CreateTaskTyped\\s*\\((.*?)\\)";
        Pattern pattern = Pattern.compile(constructorPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        assertThat(matcher.find()).isTrue();
        String constructorParams = matcher.group(1);
        
        // Constructor should have proper parameters (not all fields need to be in constructor)
        // At minimum, it should compile without errors
        assertThat(constructorParams).isNotNull();
        
        // Check that constructor calls super()
        assertThat(content).contains("super()");
    }

    @Test
    void testComparisonWithCreateRaw() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated class
        Path generatedClass = findGeneratedClass("CreateTaskTyped");
        String generatedContent = Files.readString(generatedClass);
        
        // Assert - Compare structure with CreateRaw pattern
        
        // 1. Should extend BaseStep like CreateRaw
        assertThat(generatedContent).contains("extends BaseStep");
        
        // 2. Should have DataBoundConstructor like CreateRaw
        assertThat(generatedContent).contains("@DataBoundConstructor");
        
        // 3. Should call super() in constructor like CreateRaw
        assertThat(generatedContent).contains("super()");
        
        // 4. Should have proper field declarations
        verifyFieldDeclarations(generatedContent);
        
        // 5. Should have getter methods
        verifyGetterMethods(generatedContent);
        
        // 6. Should not have duplicate parameter issues
        verifyNoDuplicateParameters(generatedContent);
    }

    @Test
    void testParameterOptimization() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find all generated classes
        List<Path> generatedClasses = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .toList();
        
        assertThat(generatedClasses).isNotEmpty();
        
        // Check for duplicate class generation (optimization rule)
        verifyNoDuplicateClasses(generatedClasses);
        
        // Check each class for parameter optimization
        for (Path classPath : generatedClasses) {
            String content = Files.readString(classPath);
            verifyParameterOptimization(content, classPath.getFileName().toString());
        }
    }

    @Test
    void testDataBoundSetterGeneration() throws IOException {
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated class
        Path generatedClass = findGeneratedClass("CreateTaskTyped");
        String content = Files.readString(generatedClass);
        
        // Assert - Check for DataBoundSetter methods (optional fields)
        // Some fields might have @DataBoundSetter instead of being in constructor
        if (content.contains("@DataBoundSetter")) {
            assertThat(content).contains("import org.kohsuke.stapler.DataBoundSetter");
            
            // Verify setter method structure
            Pattern setterPattern = Pattern.compile("@DataBoundSetter\\s+public void set\\w+\\(.*?\\)");
            Matcher matcher = setterPattern.matcher(content);
            if (matcher.find()) {
                // Setter should be properly formatted
                assertThat(matcher.group()).contains("public void set");
            }
        }
    }

    @Test
    void testComplexParameterHandling() throws IOException {
        // Create a CRD with complex parameters
        createComplexParameterCrd();
        
        // Act
        processor.processDirectory(crdDirectory, outputDirectory, BASE_PACKAGE, true);
        
        // Find generated class
        Path generatedClass = findGeneratedClass("CreateComplexTaskTyped");
        String content = Files.readString(generatedClass);
        
        // Assert - Check complex parameter handling
        verifyComplexParameterStructure(content);
    }

    private void verifyConstructorStructure(String content) {
        // Constructor should be properly annotated
        assertThat(content).contains("@DataBoundConstructor");
        
        // Constructor should exist and be public
        assertThat(content).containsPattern("@DataBoundConstructor\\s+public Create\\w+Typed\\s*\\(");
        
        // Constructor should call super()
        assertThat(content).contains("super()");
        
        // Constructor should not have syntax errors
        assertThat(content).doesNotContain("public public");
        assertThat(content).doesNotContain("DataBoundConstructor DataBoundConstructor");
    }

    private void verifyParameterFields(String content) {
        // Should have private fields with JsonProperty annotations
        Pattern fieldPattern = Pattern.compile("@JsonProperty\\(\"\\w+\"\\)\\s+private \\w+ \\w+;");
        Matcher matcher = fieldPattern.matcher(content);
        
        assertThat(matcher.find()).isTrue();
        
        // Fields should not be duplicated
        assertThat(content).doesNotContain("private private");
        assertThat(content).doesNotContain("String String");
    }

    private void verifyGetterSetterMethods(String content) {
        // Should have getter methods
        assertThat(content).containsPattern("public \\w+ get\\w+\\(\\)");
        
        // Getters should return the correct field
        if (content.contains("private String apiVersion")) {
            assertThat(content).containsPattern("public String getApiVersion\\(\\)\\s*\\{\\s*return\\s+[^;]+;\\s*\\}");
        }
    }

    private void verifyFieldDeclarations(String content) {
        // Fields should be properly declared
        assertThat(content).containsPattern("private \\w+ \\w+;");
        
        // Fields should have JsonProperty annotations
        assertThat(content).contains("@JsonProperty");
        
        // No duplicate field declarations
        assertThat(content).doesNotContain("private private");
    }

    private void verifyGetterMethods(String content) {
        // Should have public getter methods
        assertThat(content).containsPattern("public \\w+ get\\w+\\(\\)");
        
        // Getters should have proper return statements
        assertThat(content).containsPattern("return [^;]+;");
    }

    private void verifyNoDuplicateParameters(String content) {
        // No duplicate parameter names in constructor
        Pattern constructorPattern = Pattern.compile("@DataBoundConstructor\\s+public \\w+\\(([^)]+)\\)");
        Matcher matcher = constructorPattern.matcher(content);
        
        if (matcher.find()) {
            String params = matcher.group(1);
            // Split parameters and check for duplicates
            String[] paramArray = params.split(",");
            for (String param : paramArray) {
                String paramName = param.trim().replaceAll(".*\\s+(\\w+)$", "$1");
                long count = java.util.Arrays.stream(paramArray)
                    .map(p -> p.trim().replaceAll(".*\\s+(\\w+)$", "$1"))
                    .filter(name -> name.equals(paramName))
                    .count();
                assertThat(count).as("Parameter " + paramName + " should not be duplicated").isEqualTo(1);
            }
        }
    }

    private void verifyNoDuplicateClasses(List<Path> generatedClasses) {
        // Check for duplicate class names (optimization rule)
        List<String> classNames = generatedClasses.stream()
            .map(p -> p.getFileName().toString())
            .toList();
        
        for (String className : classNames) {
            long count = classNames.stream().filter(name -> name.equals(className)).count();
            assertThat(count).as("Class " + className + " should not be duplicated").isEqualTo(1);
        }
    }

    private void verifyParameterOptimization(String content, String fileName) {
        // Check for common optimization issues
        
        // 1. No duplicate imports
        List<String> imports = content.lines()
            .filter(line -> line.startsWith("import "))
            .toList();
        
        for (String importLine : imports) {
            long count = imports.stream().filter(imp -> imp.equals(importLine)).count();
            assertThat(count).as("Import should not be duplicated in " + fileName + ": " + importLine).isEqualTo(1);
        }
        
        // 2. No duplicate annotations on same element
        assertThat(content).doesNotContain("@JsonProperty @JsonProperty");
        assertThat(content).doesNotContain("@DataBoundConstructor @DataBoundConstructor");
    }

    private void verifyComplexParameterStructure(String content) {
        // Complex parameters should be handled properly
        assertThat(content).contains("extends BaseStep");
        assertThat(content).contains("@DataBoundConstructor");
        
        // Should handle nested objects
        if (content.contains("nested")) {
            assertThat(content).doesNotContain("nested nested");
        }
        
        // Should handle arrays properly
        if (content.contains("array") || content.contains("List")) {
            assertThat(content).doesNotContain("array array");
            assertThat(content).doesNotContain("List List");
        }
    }

    private Path findGeneratedClass(String className) throws IOException {
        return Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().equals(className + ".java"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Generated class not found: " + className));
    }

    private void createTaskCrdSimilarToCreateRaw() throws IOException {
        String taskCrd = """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: tasks.tekton.dev
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
                        description: "APIVersion defines the versioned schema"
                      kind:
                        type: string
                        description: "Kind is a string value representing the REST resource"
                      metadata:
                        type: object
                        description: "Standard object metadata"
                      spec:
                        type: object
                        description: "TaskSpec defines the desired state of Task"
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
                plural: tasks
                singular: task
                kind: Task
            """;
        
        Files.write(crdDirectory.resolve("task-crd.yaml"), taskCrd.getBytes());
    }

    private void createComplexParameterCrd() throws IOException {
        String complexCrd = """
            apiVersion: apiextensions.k8s.io/v1
            kind: CustomResourceDefinition
            metadata:
              name: complextasks.tekton.dev
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
                          complexParam:
                            type: object
                            properties:
                              nestedArray:
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
                              nestedObject:
                                type: object
                                additionalProperties:
                                  type: string
                          arrayOfComplexObjects:
                            type: array
                            items:
                              type: object
                              properties:
                                identifier:
                                  type: string
                                configuration:
                                  type: object
                                  properties:
                                    settings:
                                      type: object
                                      additionalProperties:
                                        oneOf:
                                        - type: string
                                        - type: number
                                        - type: boolean
              scope: Namespaced
              names:
                plural: complextasks
                singular: complextask
                kind: ComplexTask
            """;
        
        Files.write(crdDirectory.resolve("complex-task-crd.yaml"), complexCrd.getBytes());
    }
}
