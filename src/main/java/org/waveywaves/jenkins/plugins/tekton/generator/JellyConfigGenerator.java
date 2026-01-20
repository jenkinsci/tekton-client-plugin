package org.waveywaves.jenkins.plugins.tekton.generator;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Generator for Jenkins Jelly UI configuration files.
 * Creates sophisticated UI forms with proper nesting, sections, and field grouping.
 */
public class JellyConfigGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(JellyConfigGenerator.class);
    

    /**
     * Generate Jelly config with proper structure and nesting.
     */
    public void generateJellyConfig(Class<?> clazz, Path outputPath) throws IOException {
        logger.info("Generating Jelly config for: {}", clazz.getSimpleName());
        
        StringBuilder jelly = new StringBuilder();
        
        // Header
        jelly.append("<?jelly escape-by-default='true'?>\n");
        jelly.append("<j:jelly xmlns:j=\"jelly:core\" xmlns:f=\"/lib/form\">\n");
        
        // Get all fields
        Field[] fields = clazz.getDeclaredFields();
        
        // Group fields by category
        Map<String, List<Field>> fieldGroups = groupFields(fields);
        
        // Generate UI for each group
        for (Map.Entry<String, List<Field>> entry : fieldGroups.entrySet()) {
            String groupName = entry.getKey();
            List<Field> groupFields = entry.getValue();
            
            if (groupFields.isEmpty()) {
                continue;
            }
            
            // Check if this should be a section
            if (shouldCreateSection(groupName, groupFields)) {
                jelly.append("    <f:section title=\"").append(groupName).append("\">\n");
                
                for (Field field : groupFields) {
                    if (shouldSkipField(field)) {
                        continue;
                    }
                    jelly.append(generateFieldControl(field, 2));
                }
                
                jelly.append("    </f:section>\n");
            } else {
                // Regular fields without section
                for (Field field : groupFields) {
                    if (shouldSkipField(field)) {
                        continue;
                    }
                    jelly.append(generateFieldControl(field, 1));
                }
            }
        }
        
        // Footer
        jelly.append("</j:jelly>\n");
        
        // Write file
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputPath, jelly.toString());
        
        logger.info("Generated Jelly config at: {}", outputPath);
        System.out.println("  [OK] Generated: " + outputPath);
    }
    
    /**
     * Group fields by their semantic categories.
     */
    private Map<String, List<Field>> groupFields(Field[] fields) {
        Map<String, List<Field>> groups = new LinkedHashMap<>();
        
        groups.put("Basic", new ArrayList<>());
        groups.put("Metadata", new ArrayList<>());
        groups.put("Spec", new ArrayList<>());
        groups.put("Advanced", new ArrayList<>());
        
        for (Field field : fields) {
            String fieldName = field.getName();
            
            // Determine group
            if (fieldName.equals("apiVersion") || fieldName.equals("kind")) {
                groups.get("Basic").add(field);
            } else if (fieldName.equals("metadata") || fieldName.toLowerCase().contains("metadata")) {
                groups.get("Metadata").add(field);
            } else if (fieldName.equals("spec") || fieldName.toLowerCase().contains("spec")) {
                groups.get("Spec").add(field);
            } else if (fieldName.equals("status")) {
                // Skip status field - it's read-only
                continue;
            } else {
                groups.get("Advanced").add(field);
            }
        }
        
        return groups;
    }
    
    /**
     * Determine if a group should be rendered as a section.
     */
    private boolean shouldCreateSection(String groupName, List<Field> fields) {
        // Create section if more than 2 fields
        return fields.size() > 2 && !groupName.equals("Basic");
    }
    
    /**
     * Generate control for a single field with proper indentation.
     */
    private String generateFieldControl(Field field, int indentLevel) {
        StringBuilder control = new StringBuilder();
        String indent = "    ".repeat(indentLevel);
        
        String fieldName = field.getName();
        String fieldTitle = toTitle(fieldName);
        Class<?> fieldType = field.getType();
        
        // Get description from annotation if available
        String description = getFieldDescription(field);
        
        // Determine control type
        if (List.class.isAssignableFrom(fieldType)) {
            // List - use repeatableProperty with block
            control.append(indent).append("<f:block>\n");
            control.append(indent).append("    <f:entry title=\"").append(fieldTitle).append("\"");
            if (description != null && !description.isEmpty()) {
                control.append(" description=\"").append(escapeXml(description)).append("\"");
            }
            control.append(">\n");
            control.append(indent).append("        <f:repeatableProperty field=\"").append(fieldName);
            control.append("\" add=\"Add ").append(fieldTitle).append("\"/>\n");
            control.append(indent).append("    </f:entry>\n");
            control.append(indent).append("</f:block>\n");
            
        } else if (isComplexType(fieldType)) {
            // Complex object - use nested property or advanced button
            control.append(indent).append("<f:optionalBlock title=\"").append(fieldTitle);
            control.append("\" field=\"").append(fieldName).append("\"");
            if (description != null && !description.isEmpty()) {
                control.append(" help=\"").append(escapeXml(description)).append("\"");
            }
            control.append(" inline=\"true\">\n");
            control.append(indent).append("    <f:nested>\n");
            control.append(indent).append("        <f:property field=\"").append(fieldName).append("\"/>\n");
            control.append(indent).append("    </f:nested>\n");
            control.append(indent).append("</f:optionalBlock>\n");
            
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            // Boolean - checkbox
            control.append(indent).append("<f:entry field=\"").append(fieldName);
            control.append("\" title=\"").append(fieldTitle).append("\"");
            if (description != null && !description.isEmpty()) {
                control.append(" description=\"").append(escapeXml(description)).append("\"");
            }
            control.append(">\n");
            control.append(indent).append("    <f:checkbox/>\n");
            control.append(indent).append("</f:entry>\n");
            
        } else {
            // Number, Map, String, or other simple type - use textbox
            control.append(indent).append("<f:entry field=\"").append(fieldName);
            control.append("\" title=\"").append(fieldTitle).append("\"");
            if (description != null && !description.isEmpty()) {
                control.append(" description=\"").append(escapeXml(description)).append("\"");
            }
            control.append(">\n");
            control.append(indent).append("    <f:textbox/>\n");
            control.append(indent).append("</f:entry>\n");
        }
        
        return control.toString();
    }
    
    /**
     * Get field description from JsonPropertyDescription annotation.
     */
    private String getFieldDescription(Field field) {
        JsonPropertyDescription desc = field.getAnnotation(JsonPropertyDescription.class);
        if (desc != null) {
            return desc.value();
        }
        return null;
    }
    
    /**
     * Escape XML special characters.
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;")
                   .replace("\n", " ");
    }
    
    /**
     * Check if field should be skipped.
     */
    private boolean shouldSkipField(Field field) {
        String fieldName = field.getName();
        
        // Skip internal fields
        if (fieldName.equals("additionalProperties") || 
            fieldName.startsWith("_") ||
            fieldName.equals("status") ||
            java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            return true;
        }
        
        // Check for JsonIgnore
        if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnore.class)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if type is complex.
     */
    private boolean isComplexType(Class<?> type) {
        if (type.isPrimitive() || 
            type.getName().startsWith("java.lang") ||
            type.getName().startsWith("java.util")) {
            return false;
        }
        
        return type.getName().contains("tekton.generated");
    }
    
    /**
     * Convert field name to title.
     */
    private String toTitle(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        
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
     * Generate for all Create*Typed classes.
     */
    public void generateAllConfigs(Path generatedSourcesDir, Path resourcesDir, ClassLoader classLoader) 
            throws IOException {
        
        logger.info("Scanning: {}", generatedSourcesDir);
        System.out.println("\nScanning for Create*Typed classes...");
        
        List<Path> typedClasses = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(generatedSourcesDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .filter(p -> p.getFileName().toString().matches("Create.*Typed\\.java"))
                 .forEach(typedClasses::add);
        }
        
        System.out.println("Found " + typedClasses.size() + " Create*Typed classes\n");
        
        int successCount = 0;
        int failCount = 0;
        
        for (Path javaFile : typedClasses) {
            try {
                String relativePath = generatedSourcesDir.relativize(javaFile).toString();
                String className = relativePath
                    .replace(File.separator, ".")
                    .replace(".java", "");
                
                System.out.println("Processing: " + className);
                
                // Load class
                Class<?> clazz = classLoader.loadClass(className);
                
                // Output path
                Path jellyPath = resourcesDir
                    .resolve(className.replace('.', File.separatorChar))
                    .resolve("config.jelly");
                
                // Generate
                generateJellyConfig(clazz, jellyPath);
                successCount++;
                
            } catch (Exception e) {
                logger.error("Failed to process: {}", javaFile, e);
                System.err.println("  [FAIL] Failed: " + javaFile.getFileName() + " - " + e.getMessage());
                failCount++;
            }
        }
        
        System.out.println("\nGeneration complete!");
        System.out.println("   Success: " + successCount);
        System.out.println("   Failed: " + failCount);
    }
    
    /**
     * Main method.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: JellyConfigGenerator <generated-sources-dir> <resources-dir>");
            System.err.println("Example: JellyConfigGenerator target/generated-sources/tekton src/main/resources");
            return;
        }
        
        Path generatedSourcesDir = Paths.get(args[0]);
        Path resourcesDir = Paths.get(args[1]);
        
        if (!Files.exists(generatedSourcesDir)) {
            System.err.println("Directory not found: " + generatedSourcesDir);
            return;
        }
        
        try {
            JellyConfigGenerator generator = new JellyConfigGenerator();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            
            generator.generateAllConfigs(generatedSourcesDir, resourcesDir, classLoader);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }
}

