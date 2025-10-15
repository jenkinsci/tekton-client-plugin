package org.waveywaves.jenkins.plugins.tekton.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generator for Jenkins Jelly UI configuration files based on generated POJO classes.
 * This tool analyzes the structure of generated POJOs and creates appropriate
 * Jelly files with proper form controls (textbox, repeatableProperty, nested forms, etc.)
 */
public class JellyConfigGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(JellyConfigGenerator.class);
    
    /**
     * Generate Jelly config file for a specific POJO class.
     * 
     * @param clazz The class to generate config for
     * @param outputPath The output path for the jelly file
     * @throws IOException If file operations fail
     */
    public void generateJellyConfig(Class<?> clazz, Path outputPath) throws IOException {
        logger.info("Generating Jelly config for class: {}", clazz.getName());
        
        StringBuilder jellyContent = new StringBuilder();
        
        // Add Jelly header
        jellyContent.append("<?jelly escape-by-default='true'?>\n");
        jellyContent.append("<j:jelly xmlns:j=\"jelly:core\" xmlns:f=\"/lib/form\">\n");
        
        // Process each field in the class
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            // Skip additionalProperties and other internal fields
            if (shouldSkipField(field)) {
                continue;
            }
            
            String fieldName = field.getName();
            String fieldTitle = toTitle(fieldName);
            Class<?> fieldType = field.getType();
            
            // Generate appropriate UI control based on field type
            String control = generateControl(field, fieldName, fieldTitle, fieldType);
            jellyContent.append(control);
        }
        
        // Add Jelly footer
        jellyContent.append("</j:jelly>\n");
        
        // Write to file
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputPath, jellyContent.toString());
        
        logger.info("Generated Jelly config at: {}", outputPath);
    }
    
    /**
     * Determine if a field should be skipped in the UI.
     */
    private boolean shouldSkipField(Field field) {
        String fieldName = field.getName();
        
        // Skip internal fields
        if (fieldName.equals("additionalProperties") || 
            fieldName.startsWith("_") ||
            java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            return true;
        }
        
        // Check for JsonIgnore annotation
        if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnore.class)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Generate the appropriate UI control for a field.
     */
    private String generateControl(Field field, String fieldName, String fieldTitle, Class<?> fieldType) {
        StringBuilder control = new StringBuilder();
        
        // Check if it's a List/Collection
        if (List.class.isAssignableFrom(fieldType) || 
            java.util.Collection.class.isAssignableFrom(fieldType)) {
            
            // Use repeatableProperty for lists
            control.append("    <f:entry field=\"").append(fieldName).append("\" title=\"").append(fieldTitle).append("\">\n");
            control.append("        <f:repeatableProperty field=\"").append(fieldName);
            control.append("\" add=\"Add ").append(fieldTitle).append("\"/>\n");
            control.append("    </f:entry>\n");
            
        } else if (isComplexType(fieldType)) {
            // For complex nested objects, use advanced button or nested property
            control.append("    <f:entry field=\"").append(fieldName).append("\" title=\"").append(fieldTitle).append("\">\n");
            control.append("        <f:property field=\"").append(fieldName).append("\"/>\n");
            control.append("    </f:entry>\n");
            
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            // Use checkbox for booleans
            control.append("    <f:entry field=\"").append(fieldName).append("\" title=\"").append(fieldTitle).append("\">\n");
            control.append("        <f:checkbox/>\n");
            control.append("    </f:entry>\n");
            
        } else {
            // Numeric types and default: textbox for all simple types
            control.append("    <f:entry field=\"").append(fieldName).append("\" title=\"").append(fieldTitle).append("\">\n");
            control.append("        <f:textbox/>\n");
            control.append("    </f:entry>\n");
        }
        
        return control.toString();
    }
    
    /**
     * Check if a type is complex (should be rendered as nested form).
     */
    private boolean isComplexType(Class<?> type) {
        // Skip primitive types and common Java types
        if (type.isPrimitive() || 
            type.getName().startsWith("java.lang") ||
            type.getName().startsWith("java.util")) {
            return false;
        }
        
        // Check if it's a generated POJO (in tekton.generated package)
        if (type.getName().contains("tekton.generated")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Convert field name to title (camelCase to Title Case).
     */
    private String toTitle(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        
        // Convert camelCase to Title Case
        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(fieldName.charAt(0)));
        
        for (int i = 1; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append(' ');
            }
            result.append(c);
        }
        
        return result.toString();
    }
    
    /**
     * Generate Jelly configs for all Create*Typed classes in a package.
     * 
     * @param generatedSourcesDir The generated-sources directory
     * @param resourcesOutputDir The resources output directory
     * @param classLoader ClassLoader to load the generated classes
     */
    public void generateAllJellyConfigs(Path generatedSourcesDir, Path resourcesOutputDir, ClassLoader classLoader) 
            throws IOException {
        
        logger.info("Scanning for Create*Typed classes in: {}", generatedSourcesDir);
        
        // Find all Create*Typed.java files
        List<Path> typedClasses = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(generatedSourcesDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .filter(p -> p.getFileName().toString().matches("Create.*Typed\\.java"))
                 .forEach(typedClasses::add);
        }
        
        logger.info("Found {} Create*Typed classes", typedClasses.size());
        
        for (Path javaFile : typedClasses) {
            try {
                // Convert file path to class name
                String relativePath = generatedSourcesDir.relativize(javaFile).toString();
                String className = relativePath
                    .replace(File.separator, ".")
                    .replace(".java", "");
                
                logger.info("Processing class: {}", className);
                
                // Load the class
                Class<?> clazz = classLoader.loadClass(className);
                
                // Determine output path for jelly file
                // Format: resources/org/waveywaves/jenkins/plugins/tekton/generated/.../ClassName/config.jelly
                Path jellyOutputPath = resourcesOutputDir
                    .resolve(className.replace('.', File.separatorChar))
                    .resolve("config.jelly");
                
                // Generate jelly config
                generateJellyConfig(clazz, jellyOutputPath);
                
            } catch (ClassNotFoundException e) {
                logger.warn("Could not load class from file: {}", javaFile, e);
            } catch (Exception e) {
                logger.error("Error processing file: {}", javaFile, e);
            }
        }
        
        logger.info("Jelly config generation completed!");
    }
    
    /**
     * Main method for command-line execution.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: JellyConfigGenerator <generated-sources-dir> <resources-output-dir>");
            System.err.println("Example: JellyConfigGenerator target/generated-sources/tekton src/main/resources");
            System.exit(1);
        }
        
        Path generatedSourcesDir = Paths.get(args[0]);
        Path resourcesOutputDir = Paths.get(args[1]);
        
        if (!Files.exists(generatedSourcesDir)) {
            System.err.println("Generated sources directory does not exist: " + generatedSourcesDir);
            System.exit(1);
        }
        
        try {
            JellyConfigGenerator generator = new JellyConfigGenerator();
            
            // Use the current thread's classloader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            
            generator.generateAllJellyConfigs(generatedSourcesDir, resourcesOutputDir, classLoader);
            
            System.out.println("Successfully generated Jelly configs!");
            
        } catch (Exception e) {
            System.err.println("Error generating Jelly configs: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

