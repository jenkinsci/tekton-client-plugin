package org.waveywaves.jenkins.plugins.tekton.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Generates Jelly UI configuration files for Jenkins forms based on Tekton POJO classes.
 * Auto-creates config.jelly files that render proper form fields for each generated POJO.
 */
public class JellyFileGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(JellyFileGenerator.class);
    
    // Jelly templates for different field types
    private static final String JELLY_HEADER = 
        "<?jelly escape-by-default='true'?>\n" +
        "<j:jelly xmlns:j=\"jelly:core\" xmlns:f=\"/lib/form\">\n";
    
    private static final String JELLY_FOOTER = "</j:jelly>\n";
    
    // Field type mappings
    private static final Map<String, String> FIELD_TYPE_MAPPING = new HashMap<>();
    static {
        FIELD_TYPE_MAPPING.put("String", "textbox");
        FIELD_TYPE_MAPPING.put("boolean", "checkbox");
        FIELD_TYPE_MAPPING.put("Boolean", "checkbox");
        FIELD_TYPE_MAPPING.put("int", "number");
        FIELD_TYPE_MAPPING.put("Integer", "number");
        FIELD_TYPE_MAPPING.put("long", "number");
        FIELD_TYPE_MAPPING.put("Long", "number");
        FIELD_TYPE_MAPPING.put("List", "repeatableProperty");
        FIELD_TYPE_MAPPING.put("Map", "repeatableProperty");
        FIELD_TYPE_MAPPING.put("Object", "expandableTextbox");
    }
    
    /**
     * Generate Jelly files for all generated Tekton POJOs
     */
    public void generateJellyFiles(String generatedSourceDir, String resourcesDir) throws IOException {
        logger.info("Starting Jelly file generation...");
        
        Path generatedPath = Paths.get(generatedSourceDir);
        Path resourcesPath = Paths.get(resourcesDir);
        
        if (!Files.exists(generatedPath)) {
            logger.warn("Generated sources directory doesn't exist: {}", generatedSourceDir);
            return;
        }
        
        // Find all *Typed.java files (Jenkins steps)
        Files.walk(generatedPath)
            .filter(path -> path.toString().endsWith("Typed.java"))
            .forEach(javaFile -> {
                try {
                    generateJellyForJavaClass(javaFile, resourcesPath);
                } catch (IOException e) {
                    logger.error("Failed to generate Jelly for {}: {}", javaFile, e.getMessage());
                }
            });
        
        logger.info("Jelly file generation completed!");
    }
    
    /**
     * Generate Jelly config file for a specific Java class
     */
    private void generateJellyForJavaClass(Path javaFilePath, Path resourcesPath) throws IOException {
        String className = javaFilePath.getFileName().toString().replace(".java", "");
        logger.debug("Generating Jelly for class: {}", className);
        
        // Parse the Java file to extract field information
        List<FieldInfo> fields = extractFieldsFromJavaFile(javaFilePath);
        
        // Generate Jelly content
        String jellyContent = generateJellyContent(fields);
        
        // Determine output path
        String relativePath = extractPackagePath(javaFilePath);
        Path jellyOutputDir = resourcesPath.resolve(relativePath).resolve(className);
        Path jellyFile = jellyOutputDir.resolve("config.jelly");
        
        // Create directories if needed
        Files.createDirectories(jellyOutputDir);
        
        // Write Jelly file
        Files.write(jellyFile, jellyContent.getBytes());
        logger.info("Generated Jelly file: {}", jellyFile);
    }
    
    /**
     * Extract package path from Java file path to match resource structure
     */
    private String extractPackagePath(Path javaFilePath) {
        String pathStr = javaFilePath.toString();
        
        // Extract package path from generated sources
        // e.g. "target/generated-sources/tekton/org/waveywaves/.../tasks/v1/CreateTaskTyped.java"
        int tektonIndex = pathStr.indexOf("/tekton/");
        if (tektonIndex > 0) {
            String packagePath = pathStr.substring(tektonIndex + 8); // +8 to skip "/tekton/"
            packagePath = packagePath.substring(0, packagePath.lastIndexOf('/'));
            return packagePath.replace('/', File.separatorChar);
        }
        
        return "";
    }
    
    /**
     * Extract field information from Java source file
     */
    private List<FieldInfo> extractFieldsFromJavaFile(Path javaFilePath) throws IOException {
        List<FieldInfo> fields = new ArrayList<>();
        
        // Read Java file and extract @DataBoundConstructor parameters
        String content = Files.readString(javaFilePath);
        
        // Simple regex-based parsing (could be improved with proper AST parsing)
        // Look for @DataBoundConstructor method parameters
        String[] lines = content.split("\\n");
        boolean inConstructor = false;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.contains("@DataBoundConstructor")) {
                inConstructor = true;
                continue;
            }
            
            if (inConstructor && line.contains("public " + extractClassName(javaFilePath))) {
                // Parse constructor parameters
                int startParen = line.indexOf('(');
                int endParen = line.lastIndexOf(')');
                
                if (startParen > 0 && endParen > startParen) {
                    String params = line.substring(startParen + 1, endParen);
                    if (!params.trim().isEmpty()) {
                        parseConstructorParameters(params, fields);
                    }
                }
                break;
            }
        }
        
        // If no constructor found, look for getter methods
        if (fields.isEmpty()) {
            parseGetterMethods(content, fields);
        }
        
        return fields;
    }
    
    /**
     * Parse constructor parameters to extract field information
     */
    private void parseConstructorParameters(String params, List<FieldInfo> fields) {
        String[] paramList = params.split(",");
        
        for (String param : paramList) {
            param = param.trim();
            if (param.isEmpty()) continue;
            
            String[] parts = param.split("\\s+");
            if (parts.length >= 2) {
                String type = parts[parts.length - 2];
                String name = parts[parts.length - 1];
                
                // Clean up type (remove generics for now)
                if (type.contains("<")) {
                    type = type.substring(0, type.indexOf('<'));
                }
                
                fields.add(new FieldInfo(name, type, getFieldLabel(name)));
            }
        }
    }
    
    /**
     * Parse getter methods as fallback for field extraction
     */
    private void parseGetterMethods(String content, List<FieldInfo> fields) {
        String[] lines = content.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            
            // Look for public getter methods
            if (line.startsWith("public ") && line.contains(" get") && line.contains("()")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    String returnType = parts[1];
                    String methodName = parts[2];
                    
                    if (methodName.startsWith("get") && methodName.contains("(")) {
                        String fieldName = methodName.substring(3, methodName.indexOf('('));
                        fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
                        
                        // Clean up return type
                        if (returnType.contains("<")) {
                            returnType = returnType.substring(0, returnType.indexOf('<'));
                        }
                        
                        fields.add(new FieldInfo(fieldName, returnType, getFieldLabel(fieldName)));
                    }
                }
            }
        }
    }
    
    /**
     * Generate human-readable label from field name
     */
    private String getFieldLabel(String fieldName) {
        // Convert camelCase to Title Case
        StringBuilder label = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i == 0) {
                label.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                label.append(' ').append(c);
            } else {
                label.append(c);
            }
        }
        return label.toString();
    }
    
    /**
     * Extract class name from file path
     */
    private String extractClassName(Path javaFilePath) {
        return javaFilePath.getFileName().toString().replace(".java", "");
    }
    
    /**
     * Generate Jelly XML content from field information
     */
    private String generateJellyContent(List<FieldInfo> fields) {
        StringBuilder jelly = new StringBuilder();
        jelly.append(JELLY_HEADER);
        
        for (FieldInfo field : fields) {
            jelly.append(generateFieldEntry(field));
        }
        
        jelly.append(JELLY_FOOTER);
        return jelly.toString();
    }
    
    /**
     * Generate individual field entry for Jelly form
     */
    private String generateFieldEntry(FieldInfo field) {
        String fieldType = FIELD_TYPE_MAPPING.getOrDefault(field.type, "textbox");
        
        StringBuilder entry = new StringBuilder();
        entry.append("    <f:entry field=\"").append(field.name)
             .append("\" title=\"").append(field.label).append("\">\n");
        
        switch (fieldType) {
            case "textbox":
                entry.append("        <f:textbox/>\n");
                break;
            case "checkbox":
                entry.append("        <f:checkbox/>\n");
                break;
            case "number":
                entry.append("        <f:number/>\n");
                break;
            case "expandableTextbox":
                entry.append("        <f:expandableTextbox/>\n");
                break;
            case "repeatableProperty":
                entry.append("        <f:repeatableProperty field=\"").append(field.name)
                     .append("\" add=\"Add ").append(field.label).append("\"/>\n");
                break;
            default:
                entry.append("        <f:textbox/>\n");
                break;
        }
        
        entry.append("    </f:entry>\n");
        return entry.toString();
    }
    
    /**
     * Internal class to hold field information
     */
    private static class FieldInfo {
        final String name;
        final String type;
        final String label;
        
        FieldInfo(String name, String type, String label) {
            this.name = name;
            this.type = type;
            this.label = label;
        }
    }
}
