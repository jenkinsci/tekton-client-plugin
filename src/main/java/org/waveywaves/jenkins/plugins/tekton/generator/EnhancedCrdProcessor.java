package org.waveywaves.jenkins.plugins.tekton.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

/**
 * Enhanced CRD Processor that can generate POJOs extending from base classes.
 * This is specifically designed for integration with Jenkins plugins like tekton-client-plugin.
 * Generates classes like CreateRaw, ApplyTask, etc. that extend BaseStep.
 */
public class EnhancedCrdProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedCrdProcessor.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();
    
    // Configuration for base class inheritance
    private final Map<String, String> baseClassMapping = new HashMap<>();
    private final Map<String, String> baseClassImports = new HashMap<>();
    private final Map<String, String> classNameMapping = new HashMap<>();
    
    public EnhancedCrdProcessor() {
        // Default base class mappings for Jenkins plugin integration
        setupDefaultBaseClassMappings();
        setupDefaultClassNameMappings();
    }
    
    /**
     * Setup default base class mappings for Jenkins plugin integration.
     */
    private void setupDefaultBaseClassMappings() {
        // Map CRD types to Jenkins base classes - using BaseStep instead of BaseTektonStep
        baseClassMapping.put("tasks", "org.jenkinsci.plugins.workflow.steps.BaseStep");
        baseClassMapping.put("pipelines", "org.jenkinsci.plugins.workflow.steps.BaseStep");
        baseClassMapping.put("taskruns", "org.jenkinsci.plugins.workflow.steps.BaseStep");
        baseClassMapping.put("pipelineruns", "org.jenkinsci.plugins.workflow.steps.BaseStep");
        baseClassMapping.put("stepactions", "org.jenkinsci.plugins.workflow.steps.BaseStep");
        
        // Import statements for base classes
        baseClassImports.put("tasks", "org.jenkinsci.plugins.workflow.steps.BaseStep");
        baseClassImports.put("pipelines", "org.jenkinsci.plugins.workflow.steps.BaseStep");
        baseClassImports.put("taskruns", "org.jenkinsci.plugins.workflow.steps.BaseStep");
        baseClassImports.put("pipelineruns", "org.jenkinsci.plugins.workflow.steps.BaseStep");
        baseClassImports.put("stepactions", "org.jenkinsci.plugins.workflow.steps.BaseStep");
    }
    
    /**
     * Setup default class name mappings for more specific step names.
     */
    private void setupDefaultClassNameMappings() {
        // Map CRD types to specific step class names
        classNameMapping.put("tasks", "CreateTask");
        classNameMapping.put("pipelines", "CreatePipeline");
        classNameMapping.put("taskruns", "CreateTaskRun");
        classNameMapping.put("pipelineruns", "CreatePipelineRun");
        classNameMapping.put("stepactions", "CreateStepAction");
        classNameMapping.put("customtasks", "CreateCustomTask");
        // classNameMapping.put("jenkinstasks", "CreateRaw");
        // classNameMapping.put("simpletasks", "CreateSimpleTask");
    }
    
    /**
     * Add custom base class mapping.
     * 
     * @param crdType The CRD type (e.g., "tasks", "pipelines")
     * @param baseClass The fully qualified base class name
     * @param importStatement The import statement for the base class
     */
    public void addBaseClassMapping(String crdType, String baseClass, String importStatement) {
        baseClassMapping.put(crdType, baseClass);
        baseClassImports.put(crdType, importStatement);
    }
    
    /**
     * Add custom class name mapping.
     * 
     * @param crdType The CRD type (e.g., "tasks", "pipelines")
     * @param className The specific class name to use (e.g., "CreateRaw", "ApplyTask")
     */
    public void addClassNameMapping(String crdType, String className) {
        classNameMapping.put(crdType, className);
    }
    
    /**
     * Process CRD directory with enhanced generation capabilities.
     * 
     * @param crdDirectory Directory containing CRD YAML files
     * @param outputDirectory Output directory for generated Java classes
     * @param basePackage Base package name for generated classes
     * @param enableBaseClassInheritance Whether to enable base class inheritance
     * @throws IOException If processing fails
     */
    public void processDirectory(Path crdDirectory, Path outputDirectory, String basePackage, boolean enableBaseClassInheritance) throws IOException {
        logger.info("Processing CRD directory: {} with base class inheritance: {}", crdDirectory, enableBaseClassInheritance);

        try (Stream<Path> files = Files.walk(crdDirectory)) {
            List<Path> yamlFiles = files
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".yaml") || 
                               path.toString().toLowerCase().endsWith(".yml"))
                .toList();

            logger.info("Found {} YAML files", yamlFiles.size());

            for (Path yamlFile : yamlFiles) {
                try {
                    processCrdFile(yamlFile, outputDirectory, basePackage, enableBaseClassInheritance);
                } catch (Exception e) {
                    logger.error("Error processing file: {}", yamlFile, e);
                    // Continue processing other files
                }
            }
        }
    }
    
    /**
     * Process a single CRD file.
     */
    private void processCrdFile(Path crdFile, Path outputDirectory, String basePackage, boolean enableBaseClassInheritance) throws IOException {
        logger.info("Processing CRD file: {}", crdFile.getFileName());

        // Parse the YAML file
        JsonNode crdRoot = yamlMapper.readTree(crdFile.toFile());
        
        // Validate this is a CRD
        if (!isCrd(crdRoot)) {
            logger.warn("File {} is not a valid CRD, skipping", crdFile.getFileName());
            return;
        }

        String crdName = extractCrdName(crdRoot);
        logger.info("Processing CRD: {}", crdName);

        // Extract all versions and their schemas
        JsonNode versions = crdRoot.at("/spec/versions");
        if (!versions.isArray()) {
            logger.warn("No versions found in CRD {}, skipping", crdName);
            return;
        }

        for (JsonNode version : versions) {
            processVersion(crdName, version, outputDirectory, basePackage, enableBaseClassInheritance);
        }
    }
    
    /**
     * Process a specific version of a CRD.
     */
    private void processVersion(String crdName, JsonNode version, Path outputDirectory, String basePackage, boolean enableBaseClassInheritance) {
        String versionName = version.at("/name").asText();
        JsonNode schema = version.at("/schema/openAPIV3Schema");
        
        if (schema.isMissingNode()) {
            logger.warn("No schema found for version {} of CRD {}", versionName, crdName);
            return;
        }

        try {
            logger.info("Generating classes for CRD {} version {} with base class inheritance: {}", 
                       crdName, versionName, enableBaseClassInheritance);
            
            // Create package directory for this CRD and version
            String packageName = String.format("%s.%s.%s", basePackage, sanitizePackageName(crdName), versionName);
            
            // Get the specific class name for this CRD type
            String className = getClassNameForCrd(crdName);
            
            // Generate Java classes from the schema
            if (enableBaseClassInheritance) {
                generateJavaClassesWithInheritance(schema, outputDirectory, packageName, className, crdName);
            } else {
                generateJavaClasses(schema, outputDirectory, packageName, className);
            }
            
        } catch (Exception e) {
            logger.error("Error generating classes for CRD {} version {}", crdName, versionName, e);
        }
    }
    
    /**
     * Get the specific class name for a CRD type.
     */
    private String getClassNameForCrd(String crdName) {
        String className = classNameMapping.get(crdName);
        if (className != null) {
            return className;
        }
        // Fallback to default naming if no specific mapping exists
        return toPascalCase(crdName);
    }
    
    /**
     * Generate Java classes with base class inheritance.
     */
    private void generateJavaClassesWithInheritance(JsonNode schema, Path outputDirectory, String packageName, String className, String crdName) throws IOException {
        // Convert JsonNode to JSON string for jsonschema2pojo
        String schemaJson = jsonMapper.writeValueAsString(schema);
        
        // Configure the code generator
        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isGenerateBuilders() {
                return true;
            }
            
            @Override
            public boolean isUsePrimitives() {
                return false;
            }
            
            @Override
            public boolean isIncludeJsr303Annotations() {
                return true;
            }
            
            @Override
            public String getTargetPackage() {
                return packageName;
            }
            
            @Override
            public boolean isIncludeHashcodeAndEquals() {
                return false; // Disable problematic equals/hashCode generation
            }
        };

        SchemaMapper mapper = new SchemaMapper(
            new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()),
            new SchemaGenerator()
        );

        // Generate the Java classes
        File outputDir = outputDirectory.toFile();
        try {
            JCodeModel codeModel = new JCodeModel();
            mapper.generate(
                codeModel,
                className,
                packageName,
                schemaJson
            );
            
            // Post-process to add base class inheritance
            postProcessForInheritance(codeModel, packageName, className, crdName);
            
            codeModel.build(outputDir);
        } catch (Exception e) {
            logger.error("Failed to generate classes for {}: {}", className, e.getMessage());
            throw new IOException("Code generation failed", e);
        }

        logger.info("Generated Java classes with inheritance in package: {}", packageName);
    }
    
    /**
     * Post-process generated classes to add base class inheritance.
     */
    private void postProcessForInheritance(JCodeModel codeModel, String packageName, String className, String crdName) {
        // Get the base class for this CRD type
        String baseClass = baseClassMapping.get(crdName);
        String baseClassImport = baseClassImports.get(crdName);
        
        if (baseClass == null) {
            logger.warn("No base class mapping found for CRD type: {}", crdName);
            return;
        }
        
        // Find the generated class
        JPackage pkg = codeModel._package(packageName);
        JDefinedClass generatedClass = pkg._getClass(className);
        
        if (generatedClass == null) {
            logger.warn("Generated class not found: {}", className);
            return;
        }
        
        // Add base class import
        JClass baseClassRef = codeModel.directClass(baseClass);
        generatedClass._extends(baseClassRef);
        
        // Add import statement
        generatedClass.owner().directClass(baseClassImport);
        
        // Add Jenkins-specific annotations and methods
        addJenkinsSpecificFeatures(generatedClass, crdName);
        
        logger.info("Added inheritance from {} to class {}", baseClass, className);
    }
    
    /**
     * Add Jenkins-specific features to the generated class.
     */
    private void addJenkinsSpecificFeatures(JDefinedClass generatedClass, String crdName) {
        try {
            JMethod constructor = generatedClass.constructor(JMod.PUBLIC);
            constructor.annotate(generatedClass.owner().ref("org.kohsuke.stapler.DataBoundConstructor"));
            constructor.body().invoke("super");
                
        } catch (Exception e) {
            logger.warn("Could not add Jenkins-specific features: {}", e.getMessage());
        }
    }
    
    /**
     * Generate Java classes without inheritance (original method).
     */
    private void generateJavaClasses(JsonNode schema, Path outputDirectory, String packageName, String className) throws IOException {
        // Convert JsonNode to JSON string for jsonschema2pojo
        String schemaJson = jsonMapper.writeValueAsString(schema);
        
        // Configure the code generator
        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isGenerateBuilders() {
                return true;
            }
            
            @Override
            public boolean isUsePrimitives() {
                return false;
            }
            
            @Override
            public boolean isIncludeJsr303Annotations() {
                return true;
            }
            
            @Override
            public String getTargetPackage() {
                return packageName;
            }
            
            @Override
            public boolean isIncludeHashcodeAndEquals() {
                return false; // Disable problematic equals/hashCode generation
            }
        };

        SchemaMapper mapper = new SchemaMapper(
            new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()),
            new SchemaGenerator()
        );

        // Generate the Java classes
        File outputDir = outputDirectory.toFile();
        try {
            JCodeModel codeModel = new JCodeModel();
            mapper.generate(
                codeModel,
                className,
                packageName,
                schemaJson
            );
            codeModel.build(outputDir);
        } catch (Exception e) {
            logger.error("Failed to generate classes for {}: {}", className, e.getMessage());
            throw new IOException("Code generation failed", e);
        }

        logger.info("Generated Java classes in package: {}", packageName);
    }

    private boolean isCrd(JsonNode root) {
        return "CustomResourceDefinition".equals(root.at("/kind").asText()) &&
               root.at("/apiVersion").asText().startsWith("apiextensions.k8s.io/");
    }

    private String extractCrdName(JsonNode crd) {
        String fullName = crd.at("/metadata/name").asText();
        // Extract the resource name (e.g., "pipelines" from "pipelines.tekton.dev")
        return fullName.split("\\.")[0];
    }

    private String sanitizePackageName(String name) {
        // Convert to lowercase and replace non-alphanumeric characters with underscores
        return name.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            } else {
                capitalizeNext = true;
            }
        }
        
        return result.toString();
    }
} 