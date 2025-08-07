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
 * Enhanced CRD Processor for Jenkins Plugin Integration.
 * 
 * This processor extends basic CRD → POJO generation with:
 * 1. Base class inheritance (extend BaseStep for Jenkins pipeline steps)
 * 2. Custom class naming for Jenkins steps  
 * 3. Type-safe API generation
 * 4. Jenkins-specific annotations
 * 
 * Flow:
 * CRD YAML → OpenAPI Schema → Enhanced POJOs + Jenkins Steps
 */
public class EnhancedCrdProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedCrdProcessor.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();
    
    // Configuration Maps for Jenkins Integration
    private final Map<String, String> baseClassMapping = new HashMap<>();      // CRD type → Base class
    private final Map<String, String> baseClassImports = new HashMap<>();     // CRD type → Import statement  
    private final Map<String, String> classNameMapping = new HashMap<>();     // CRD type → Jenkins step name
    
    public EnhancedCrdProcessor() {
        // Initialize default configurations
        setupDefaultConfigurations();
    }
    
    /**
     * Setup default configurations for Jenkins plugin integration.
     */
    private void setupDefaultConfigurations() {
        logger.debug("Setting up default Jenkins integration configurations");
        
        // Default base class mappings - all extend BaseStep
        String defaultBaseStep = "org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep";
        
        baseClassMapping.put("tasks", defaultBaseStep);
        baseClassMapping.put("pipelines", defaultBaseStep);
        baseClassMapping.put("taskruns", defaultBaseStep);
        baseClassMapping.put("pipelineruns", defaultBaseStep);
        baseClassMapping.put("stepactions", defaultBaseStep);
        baseClassMapping.put("customruns", defaultBaseStep);
        
        // Default import mappings
        baseClassImports.put("tasks", defaultBaseStep);
        baseClassImports.put("pipelines", defaultBaseStep);
        baseClassImports.put("taskruns", defaultBaseStep);
        baseClassImports.put("pipelineruns", defaultBaseStep);
        baseClassImports.put("stepactions", defaultBaseStep);
        baseClassImports.put("customruns", defaultBaseStep);
        
        // Default Jenkins step class names
        classNameMapping.put("tasks", "CreateTaskTyped");
        classNameMapping.put("pipelines", "CreatePipelineTyped");
        classNameMapping.put("taskruns", "CreateTaskRunTyped");
        classNameMapping.put("pipelineruns", "CreatePipelineRunTyped");
        classNameMapping.put("stepactions", "CreateStepActionTyped");
        classNameMapping.put("customruns", "CreateCustomRunTyped");
    }
    
    /**
     * Add custom base class mapping for specific CRD types.
     * 
     * @param crdType The CRD type (e.g., "tasks", "pipelines")
     * @param baseClass The fully qualified base class name
     * @param importStatement The import statement for the base class
     */
    public void addBaseClassMapping(String crdType, String baseClass, String importStatement) {
        logger.debug("Adding base class mapping: {} → {}", crdType, baseClass);
        baseClassMapping.put(crdType, baseClass);
        baseClassImports.put(crdType, importStatement);
    }
    
    /**
     * Add custom class name mapping for specific CRD types.
     * 
     * @param crdType The CRD type (e.g., "tasks", "pipelines")
     * @param className The Jenkins step class name (e.g., "CreateTaskTyped")
     */
    public void addClassNameMapping(String crdType, String className) {
        logger.debug("Adding class name mapping: {} → {}", crdType, className);
        classNameMapping.put(crdType, className);
    }
    
    /**
     * Main processing method - Process CRD directory with enhanced generation.
     * 
     * @param crdDirectory Directory containing CRD YAML files
     * @param outputDirectory Output directory for generated Java classes
     * @param basePackage Base package name for generated classes
     * @param enableBaseClassInheritance Whether to enable base class inheritance
     * @throws IOException If processing fails
     */
    public void processDirectory(Path crdDirectory, Path outputDirectory, String basePackage, 
                                boolean enableBaseClassInheritance) throws IOException {
        
        logger.info("🚀 Starting Enhanced CRD Processing...");
        logger.info("📁 CRD Directory: {}", crdDirectory);
        logger.info("📁 Output Directory: {}", outputDirectory);
        logger.info("📦 Base Package: {}", basePackage);
        logger.info("🔧 Base Class Inheritance: {}", enableBaseClassInheritance);

        // Find all YAML files in CRD directory
        try (Stream<Path> files = Files.walk(crdDirectory)) {
            List<Path> yamlFiles = files
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.toString().toLowerCase();
                    return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
                })
                .toList();

            logger.info("📄 Found {} CRD YAML files", yamlFiles.size());

            // Process each CRD file
            int processedCount = 0;
            int errorCount = 0;
            
            for (Path yamlFile : yamlFiles) {
                try {
                    logger.info("Processing: {}", yamlFile.getFileName());
                    
                    processCrdFile(yamlFile, outputDirectory, basePackage, enableBaseClassInheritance);
                    processedCount++;
                    
                    logger.info("Successfully processed: {}", yamlFile.getFileName());
                    
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Error processing file: {}", yamlFile, e);
                    // Continue processing other files
                }
            }
            
            // Summary
            logger.info("📊 Processing Summary:");
            logger.info("   ✅ Successfully processed: {} files", processedCount);
            logger.info("   ❌ Errors: {} files", errorCount);
            logger.info("   📁 Generated code location: {}", outputDirectory);
            
            if (errorCount == 0) {
                logger.info("🎉 All CRD files processed successfully!");
            } else {
                logger.warn("⚠️  {} files had errors, but {} files processed successfully", errorCount, processedCount);
            }
        }
    }
    
    /**
     * Process a single CRD file and generate Java classes.
     */
    private void processCrdFile(Path yamlFile, Path outputDirectory, String basePackage, 
                               boolean enableBaseClassInheritance) throws IOException {
        
        // 1. Parse YAML file
        JsonNode crdNode = yamlMapper.readTree(yamlFile.toFile());
        
        // 2. Extract CRD metadata
        String kind = extractKind(crdNode);
        String version = extractVersion(crdNode);
        String crdType = kind.toLowerCase();
        
        logger.debug("📋 CRD Details - Kind: {}, Version: {}, Type: {}", kind, version, crdType);
        
        // 3. Extract OpenAPI schema
        JsonNode schema = extractOpenApiSchema(crdNode);
        if (schema == null) {
            logger.warn("⚠️  No OpenAPI schema found in: {}", yamlFile.getFileName());
            return;
        }
        
        // 4. Generate package structure
        String targetPackage = basePackage + "." + crdType + "." + version;
        logger.debug("📦 Target package: {}", targetPackage);
        
        // 5. Generate POJOs using jsonschema2pojo
        generateJavaClasses(schema, outputDirectory, targetPackage, kind);
        
        // 6. Generate Jenkins Steps (if enabled)
        if (enableBaseClassInheritance && classNameMapping.containsKey(crdType)) {
            generateJenkinsStep(outputDirectory, basePackage, crdType, kind, version);
        }
    }
    
    /**
     * Extract CRD kind from YAML.
     */
    private String extractKind(JsonNode crdNode) {
        JsonNode spec = crdNode.path("spec");
        JsonNode names = spec.path("names");
        return names.path("kind").asText("Unknown");
    }
    
    /**
     * Extract API version from CRD.
     */
    private String extractVersion(JsonNode crdNode) {
        JsonNode spec = crdNode.path("spec");
        JsonNode versions = spec.path("versions");
        
        if (versions.isArray() && versions.size() > 0) {
            // Get the first version (usually the latest)
            return versions.get(0).path("name").asText("v1");
        }
        
        return "v1";
    }
    
    /**
     * Extract OpenAPI schema from CRD.
     */
    private JsonNode extractOpenApiSchema(JsonNode crdNode) {
        JsonNode spec = crdNode.path("spec");
        JsonNode versions = spec.path("versions");
        
        if (versions.isArray() && versions.size() > 0) {
            JsonNode version = versions.get(0);
            JsonNode schema = version.path("schema").path("openAPIV3Schema");
            
            if (!schema.isMissingNode()) {
                return schema;
            }
        }
        
        return null;
    }
    
    /**
     * Generate Java POJOs using jsonschema2pojo library.
     */
    private void generateJavaClasses(JsonNode schema, Path outputDirectory, String targetPackage, String rootClassName) throws IOException {
        
        logger.debug("🏗️  Generating POJOs for package: {}", targetPackage);
        
        // Create code model
        JCodeModel codeModel = new JCodeModel();
        
        // Configure generation
        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isGenerateBuilders() {
                return true; // Enable builder pattern
            }
            
            @Override
            public boolean isUsePrimitives() {
                return false; // Use wrapper classes
            }
            
            @Override
            public boolean isIncludeJsr303Annotations() {
                return true; // Include validation annotations
            }
            
            @Override
            public boolean isIncludeJsr305Annotations() {
                return true; // Include nullability annotations
            }
        };
        
        // Create schema mapper
        SchemaMapper mapper = new SchemaMapper(
            new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), 
            new SchemaGenerator()
        );
        
        // Generate classes
        mapper.generate(codeModel, rootClassName, targetPackage, schema);
        
        // Write generated code to output directory
        codeModel.build(outputDirectory.toFile());
        
        logger.debug("✅ POJOs generated successfully for: {}", rootClassName);
    }
    
    /**
     * Generate Jenkins pipeline step that extends BaseStep.
     */
    private void generateJenkinsStep(Path outputDirectory, String basePackage, String crdType, 
                                   String kind, String version) throws IOException {
        
        String stepClassName = classNameMapping.get(crdType);
        String baseClass = baseClassMapping.get(crdType);
        
        if (stepClassName == null || baseClass == null) {
            logger.debug("⏭️  Skipping Jenkins step generation for: {} (no mapping configured)", crdType);
            return;
        }
        
        logger.debug("🔧 Generating Jenkins step: {} extends {}", stepClassName, baseClass);
        
        // For now, we'll log the intent - full Jenkins step generation would require
        // more complex code generation with Jenkins annotations, UI forms, etc.
        logger.info("📝 Jenkins Step Planned: {} for {} (extends {})", stepClassName, kind, baseClass);
        
        // TODO: Implement full Jenkins step generation with:
        // - @DataBoundConstructor
        // - @Symbol annotation
        // - Jenkins UI form (config.jelly)
        // - Descriptor class
        // - Integration with generated POJOs
    }
}
