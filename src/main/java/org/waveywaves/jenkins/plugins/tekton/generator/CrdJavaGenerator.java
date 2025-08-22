package org.waveywaves.jenkins.plugins.tekton.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simplified CRD Java Generator for Maven integration.
 * Removes CLI dependencies and uses EnhancedCrdProcessor for Jenkins plugin integration.
 */
public class CrdJavaGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(CrdJavaGenerator.class);
    
    // Make base class configurable through system properties or constants
    private static final String DEFAULT_BASE_STEP_CLASS = "org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep";
    
    // Allow override via system property
    private static final String BASE_STEP_CLASS = System.getProperty("tekton.generator.base.class", DEFAULT_BASE_STEP_CLASS);

    /**
     * Main method for Maven exec plugin integration.
     * 
     * Arguments:
     * args[0] - CRD directory path (e.g., "src/main/resources/crds")
     * args[1] - Output directory path (e.g., "target/generated-sources/tekton")  
     * args[2] - Base package name (e.g., "org.waveywaves.jenkins.plugins.tekton.generated")
     * args[3] - (Optional) Base step class name (overrides default/system property)
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: CrdJavaGenerator <crd-directory> <output-directory> <base-package> [base-step-class]");
            System.err.println("Example: CrdJavaGenerator src/main/resources/crds target/generated-sources/tekton org.example.generated");
            System.err.println("Example with custom base class: CrdJavaGenerator src/main/resources/crds target/generated-sources/tekton org.example.generated org.custom.MyBaseStep");
            System.exit(1);
        }

        try {
            // Parse arguments
            String crdDirPath = args[0];
            String outputDirPath = args[1]; 
            String basePackage = args[2];
            String baseStepClass = args.length > 3 ? args[3] : BASE_STEP_CLASS;
            
            // Validate arguments
            Path crdDirectory = Paths.get(crdDirPath);
            Path outputDirectory = Paths.get(outputDirPath);
            
            logger.info("Starting Enhanced CRD Java code generation...");
            logger.info("CRD Directory: {}", crdDirectory.toAbsolutePath());
            logger.info("Output Directory: {}", outputDirectory.toAbsolutePath());
            logger.info("Base Package: {}", basePackage);
            logger.info("Base Step Class: {}", baseStepClass);

            // Validate input directory exists
            File crdDir = crdDirectory.toFile();
            if (!crdDir.exists() || !crdDir.isDirectory()) {
                logger.error("CRD directory does not exist or is not a directory: {}", crdDirectory);
                System.exit(1);
            }

            // Create output directory if needed
            File outputDir = outputDirectory.toFile();
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                if (!created) {
                    logger.error("Failed to create output directory: {}", outputDirectory);
                    System.exit(1);
                }
                logger.info("Created output directory: {}", outputDirectory);
            }

            // Create and configure Enhanced CRD Processor
            EnhancedCrdProcessor processor = new EnhancedCrdProcessor();
            
            // Configure for Jenkins plugin integration with configurable base class
            configureJenkinsIntegration(processor, basePackage, baseStepClass);
            
            // Process all CRD files with enhanced features
            processor.processDirectory(
                crdDirectory, 
                outputDirectory, 
                basePackage, 
                true  // Enable base class inheritance for Jenkins steps
            );
            
            logger.info("Enhanced Java code generation completed successfully!");
            System.out.println("✅ Generated Tekton POJOs and Jenkins Steps successfully!");
            
        } catch (Exception e) {
            logger.error("Error during enhanced code generation", e);
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Configure EnhancedCrdProcessor for Jenkins plugin integration.
     * Sets up base class mappings and class name mappings for Jenkins steps.
     */
    private static void configureJenkinsIntegration(EnhancedCrdProcessor processor, String basePackage, String baseStepClass) {
        logger.info("Configuring Jenkins plugin integration...");
        logger.info("Using base step class: {}", baseStepClass);
        
        // Configure base class inheritance - all generated steps extend the specified base class
        processor.addBaseClassMapping("tasks", baseStepClass, baseStepClass);
        processor.addBaseClassMapping("pipelines", baseStepClass, baseStepClass);
        processor.addBaseClassMapping("taskruns", baseStepClass, baseStepClass);
        processor.addBaseClassMapping("pipelineruns", baseStepClass, baseStepClass);
        processor.addBaseClassMapping("stepactions", baseStepClass, baseStepClass);
        processor.addBaseClassMapping("customruns", baseStepClass, baseStepClass);
        
        // Configure Jenkins step class names
        processor.addClassNameMapping("tasks", "CreateTaskTyped");
        processor.addClassNameMapping("pipelines", "CreatePipelineTyped");  
        processor.addClassNameMapping("taskruns", "CreateTaskRunTyped");
        processor.addClassNameMapping("pipelineruns", "CreatePipelineRunTyped");
        processor.addClassNameMapping("stepactions", "CreateStepActionTyped");
        processor.addClassNameMapping("customruns", "CreateCustomRunTyped");
        
        logger.info("Jenkins integration configuration completed");
    }
}