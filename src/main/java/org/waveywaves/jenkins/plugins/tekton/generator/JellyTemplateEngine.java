package org.waveywaves.jenkins.plugins.tekton.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Template engine for generating Jelly UI files with advanced field type support.
 * Provides templates for different Jenkins form field types and Tekton-specific patterns.
 */
public class JellyTemplateEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(JellyTemplateEngine.class);
    
    // Base Jelly template structure
    public static final String JELLY_HEADER = 
        "<?jelly escape-by-default='true'?>\n" +
        "<j:jelly xmlns:j=\"jelly:core\" xmlns:f=\"/lib/form\">\n";
    
    public static final String JELLY_FOOTER = "</j:jelly>\n";
    
    // Field templates for different types
    private static final Map<String, String> FIELD_TEMPLATES = new HashMap<>();
    
    static {
        // Basic field types
        FIELD_TEMPLATES.put("STRING", 
            "    <f:entry field=\"{field}\" title=\"{title}\">\n" +
            "        <f:textbox/>\n" +
            "    </f:entry>\n");
        
        FIELD_TEMPLATES.put("BOOLEAN", 
            "    <f:entry field=\"{field}\" title=\"{title}\">\n" +
            "        <f:checkbox/>\n" +
            "    </f:entry>\n");
        
        FIELD_TEMPLATES.put("NUMBER", 
            "    <f:entry field=\"{field}\" title=\"{title}\">\n" +
            "        <f:number/>\n" +
            "    </f:entry>\n");
        
        FIELD_TEMPLATES.put("TEXTAREA", 
            "    <f:entry field=\"{field}\" title=\"{title}\">\n" +
            "        <f:expandableTextbox/>\n" +
            "    </f:entry>\n");
        
        // Collection types
        FIELD_TEMPLATES.put("LIST", 
            "    <f:entry title=\"{title}\">\n" +
            "        <f:repeatableProperty field=\"{field}\" add=\"Add {title}\"/>\n" +
            "    </f:entry>\n");
        
        FIELD_TEMPLATES.put("MAP", 
            "    <f:entry title=\"{title}\">\n" +
            "        <f:repeatableProperty field=\"{field}\" add=\"Add {title}\"/>\n" +
            "    </f:entry>\n");
        
        // Tekton-specific templates
        FIELD_TEMPLATES.put("TEKTON_RESOURCE_REF", 
            "    <f:entry field=\"{field}\" title=\"{title}\">\n" +
            "        <f:textbox/>\n" +
            "        <f:description>Reference to a Tekton resource</f:description>\n" +
            "    </f:entry>\n");
        
        FIELD_TEMPLATES.put("TEKTON_PARAM", 
            "    <f:entry title=\"{title}\">\n" +
            "        <f:repeatableProperty field=\"{field}\" add=\"Add Parameter\"/>\n" +
            "    </f:entry>\n");
        
        FIELD_TEMPLATES.put("TEKTON_WORKSPACE", 
            "    <f:entry title=\"{title}\">\n" +
            "        <f:repeatableProperty field=\"{field}\" add=\"Add Workspace\"/>\n" +
            "    </f:entry>\n");
        
        // Kubernetes-specific templates
        FIELD_TEMPLATES.put("K8S_LABEL_SELECTOR", 
            "    <f:entry field=\"{field}\" title=\"{title}\">\n" +
            "        <f:expandableTextbox/>\n" +
            "        <f:description>Kubernetes label selector (key=value format)</f:description>\n" +
            "    </f:entry>\n");
        
        FIELD_TEMPLATES.put("K8S_RESOURCE_REQUIREMENTS", 
            "    <f:entry title=\"{title}\">\n" +
            "        <f:repeatableProperty field=\"{field}\" add=\"Add Resource Requirement\"/>\n" +
            "    </f:entry>\n");
        
        // Complex object template
        FIELD_TEMPLATES.put("OBJECT", 
            "    <f:entry field=\"{field}\" title=\"{title}\">\n" +
            "        <f:expandableTextbox/>\n" +
            "        <f:description>Complex object configuration</f:description>\n" +
            "    </f:entry>\n");
    }
    
    /**
     * Generate complete Jelly file content
     */
    public String generateJellyFile(Map<String, FieldTemplate> fields) {
        StringBuilder jelly = new StringBuilder();
        jelly.append(JELLY_HEADER);
        
        // Add common metadata fields first
        jelly.append(generateMetadataSection());
        
        // Add spec section if there are spec fields
        boolean hasSpecFields = fields.keySet().stream().anyMatch(k -> k.startsWith("spec"));
        if (hasSpecFields) {
            jelly.append("    <f:section title=\"Specification\">\n");
        }
        
        // Generate field entries
        for (Map.Entry<String, FieldTemplate> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            FieldTemplate template = entry.getValue();
            
            String fieldEntry = generateFieldEntry(fieldName, template);
            jelly.append(fieldEntry);
        }
        
        if (hasSpecFields) {
            jelly.append("    </f:section>\n");
        }
        
        jelly.append(JELLY_FOOTER);
        return jelly.toString();
    }
    
    /**
     * Generate metadata section for Tekton resources
     */
    private String generateMetadataSection() {
        return "    <f:section title=\"Metadata\">\n" +
               "        <f:entry field=\"name\" title=\"Name\">\n" +
               "            <f:textbox/>\n" +
               "        </f:entry>\n" +
               "        <f:entry field=\"generateName\" title=\"Generate Name\">\n" +
               "            <f:textbox/>\n" +
               "        </f:entry>\n" +
               "        <f:entry field=\"namespace\" title=\"Namespace\">\n" +
               "            <f:textbox/>\n" +
               "        </f:entry>\n" +
               "        <f:entry title=\"Cluster Name\" field=\"clusterName\">\n" +
               "            <f:select name=\"inputType\"></f:select>\n" +
               "        </f:entry>\n" +
               "    </f:section>\n";
    }
    
    /**
     * Generate individual field entry
     */
    public String generateFieldEntry(String fieldName, FieldTemplate template) {
        String templateKey = determineTemplateKey(fieldName, template);
        String templateString = FIELD_TEMPLATES.getOrDefault(templateKey, FIELD_TEMPLATES.get("STRING"));
        
        // Replace placeholders
        return templateString
            .replace("{field}", fieldName)
            .replace("{title}", template.getDisplayName());
    }
    
    /**
     * Determine which template to use based on field characteristics
     */
    private String determineTemplateKey(String fieldName, FieldTemplate template) {
        String type = template.getType().toUpperCase();
        
        // Tekton-specific field mappings
        if (fieldName.contains("param") || fieldName.equals("params")) {
            return "TEKTON_PARAM";
        }
        if (fieldName.contains("workspace") || fieldName.equals("workspaces")) {
            return "TEKTON_WORKSPACE";
        }
        if (fieldName.contains("ref") && !fieldName.contains("Ref")) {
            return "TEKTON_RESOURCE_REF";
        }
        
        // Kubernetes-specific mappings
        if (fieldName.contains("selector") || fieldName.contains("Selector")) {
            return "K8S_LABEL_SELECTOR";
        }
        if (fieldName.contains("resource") && (fieldName.contains("limit") || fieldName.contains("request"))) {
            return "K8S_RESOURCE_REQUIREMENTS";
        }
        
        // Generic type mappings
        if (type.contains("BOOLEAN")) {
            return "BOOLEAN";
        }
        if (type.contains("INT") || type.contains("LONG") || type.contains("DOUBLE") || type.contains("FLOAT")) {
            return "NUMBER";
        }
        if (type.contains("LIST")) {
            return "LIST";
        }
        if (type.contains("MAP")) {
            return "MAP";
        }
        if (template.isLargeText()) {
            return "TEXTAREA";
        }
        if (template.isComplexObject()) {
            return "OBJECT";
        }
        
        return "STRING"; // Default fallback
    }
    
    /**
     * Template configuration for a field
     */
    public static class FieldTemplate {
        private final String type;
        private final String displayName;
        private final boolean largeText;
        private final boolean complexObject;
        private final String description;
        
        public FieldTemplate(String type, String displayName) {
            this(type, displayName, false, false, null);
        }
        
        public FieldTemplate(String type, String displayName, boolean largeText, boolean complexObject, String description) {
            this.type = type;
            this.displayName = displayName;
            this.largeText = largeText;
            this.complexObject = complexObject;
            this.description = description;
        }
        
        public String getType() { return type; }
        public String getDisplayName() { return displayName; }
        public boolean isLargeText() { return largeText; }
        public boolean isComplexObject() { return complexObject; }
        public String getDescription() { return description; }
        
        // Builder pattern for easy configuration
        public static FieldTemplate string(String displayName) {
            return new FieldTemplate("String", displayName);
        }
        
        public static FieldTemplate bool(String displayName) {
            return new FieldTemplate("Boolean", displayName);
        }
        
        public static FieldTemplate number(String displayName) {
            return new FieldTemplate("Integer", displayName);
        }
        
        public static FieldTemplate list(String displayName) {
            return new FieldTemplate("List", displayName);
        }
        
        public static FieldTemplate textarea(String displayName) {
            return new FieldTemplate("String", displayName, true, false, null);
        }
        
        public static FieldTemplate object(String displayName) {
            return new FieldTemplate("Object", displayName, false, true, null);
        }
    }
}
