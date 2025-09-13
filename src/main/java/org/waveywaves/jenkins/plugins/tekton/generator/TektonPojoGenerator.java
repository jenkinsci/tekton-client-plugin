package org.waveywaves.jenkins.plugins.tekton.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simplified CRD Java Generator for Maven integration.
 * Removes CLI dependencies and uses TektonCrdToJavaProcessor for Jenkins plugin integration.
 */
public class TektonPojoGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(TektonPojoGenerator.class);
    
    // Base class for all generated Jenkins pipeline steps
    private static final String BASE_STEP_CLASS = "org.waveywaves.jenkins.plugins.tekton.client.build.BaseStep";

    /**
     * Main method for Maven exec plugin integration.
     * 
     * Arguments:
     * args[0] - CRD directory path (e.g., "src/main/resources/crds")
     * args[1] - Output directory path (e.g., "target/generated-sources/tekton")  
     * args[2] - Base package name (e.g., "org.waveywaves.jenkins.plugins.tekton.generated")
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            String usage = "Usage: TektonPojoGenerator <crd-directory> <output-directory> <base-package>\n" +
                          "Example: TektonPojoGenerator src/main/resources/crds target/generated-sources/tekton org.example.generated";
            throw new IllegalArgumentException(usage);
        }

        try {
            // Parse arguments
            String crdDirPath = args[0];
            String outputDirPath = args[1]; 
            String basePackage = args[2];
            
            // Validate arguments
            Path crdDirectory = Paths.get(crdDirPath);
            Path outputDirectory = Paths.get(outputDirPath);
            
            logger.info("Starting Enhanced CRD Java code generation...");
            logger.info("CRD Directory: {}", crdDirectory.toAbsolutePath());
            logger.info("Output Directory: {}", outputDirectory.toAbsolutePath());
            logger.info("Base Package: {}", basePackage);
            
            // Also print to System.out to ensure visibility in Maven output
            System.out.println("=== Starting Enhanced CRD Java code generation ===");
            System.out.println("CRD Directory: " + crdDirectory.toAbsolutePath());
            System.out.println("Output Directory: " + outputDirectory.toAbsolutePath());
            System.out.println("Base Package: " + basePackage);

            // Validate input directory exists
            File crdDir = crdDirectory.toFile();
            if (!crdDir.exists() || !crdDir.isDirectory()) {
                throw new IllegalArgumentException("CRD directory does not exist or is not a directory: " + crdDirectory);
            }

            // Create output directory if needed
            File outputDir = outputDirectory.toFile();
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create output directory: " + outputDirectory);
                }
                logger.info("Created output directory: {}", outputDirectory);
                System.out.println("Created output directory: " + outputDirectory);
            }

            // Create and configure Enhanced CRD Processor
            TektonCrdToJavaProcessor processor = new TektonCrdToJavaProcessor();
            
            // Configure for Jenkins plugin integration
            configureJenkinsIntegration(processor, basePackage);
            
            // Process all CRD files with enhanced features
            processor.processDirectory(
                crdDirectory, 
                outputDirectory, 
                basePackage, 
                true  // Enable base class inheritance for Jenkins steps
            );
            
            logger.info("Enhanced Java code generation completed successfully!");
            System.out.println("=== Enhanced Java code generation completed successfully! ===");
            System.out.println("Generated Tekton POJOs and Jenkins Steps successfully!");
            
        } catch (Exception e) {
            logger.error("Error during enhanced code generation", e);
            throw new RuntimeException("Failed to generate Tekton POJOs", e);
        }
    }
    
    /**
     * Configure TektonCrdToJavaProcessor for Jenkins plugin integration.
     * Sets up base class mappings and class name mappings for Jenkins steps.
     */
    private static void configureJenkinsIntegration(TektonCrdToJavaProcessor processor, String basePackage) {
        logger.info("Configuring Jenkins plugin integration...");
        System.out.println("Configuring Jenkins plugin integration...");
        
        // Configure base class inheritance - all generated steps extend BaseStep
        String baseStepClass = BASE_STEP_CLASS;
        
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
        System.out.println("Jenkins integration configuration completed");
    }
}
