package org.waveywaves.jenkins.plugins.tekton.client;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import org.waveywaves.jenkins.plugins.tekton.client.global.ClusterConfig;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TektonUtils {
    private static final Logger logger = Logger.getLogger(TektonUtils.class.getName());
    public static final String DEFAULT_CLIENT_KEY = "default";
    private static Map<String,TektonClient> tektonClientMap = new HashMap<>();
    private static Map<String,KubernetesClient> kubernetesClientMap = new HashMap<>();

    public enum TektonResourceType {
        task,
        taskrun,
        pipeline,
        pipelinerun
    }

    public synchronized static void initializeKubeClients(Config config) {
        tektonClientMap = new HashMap<>();
        kubernetesClientMap = new HashMap<>();

        logger.info("Initializing Kube and Tekton Clients");
        
        // For Kind clusters and test environments, ensure SSL certificate trust is enabled
        if (isKindCluster(config.getMasterUrl()) || isTestEnvironment()) {
            config.setTrustCerts(true);
            config.setDisableHostnameVerification(true);
            logger.info("Enabling SSL certificate trust for default cluster");
        }
        
        TektonClient tektonClient = new DefaultTektonClient(config);
        KubernetesClient kubernetesClient = new DefaultKubernetesClient(config);

        tektonClientMap.put(DEFAULT_CLIENT_KEY, tektonClient);
        kubernetesClientMap.put(DEFAULT_CLIENT_KEY, kubernetesClient);
        logger.info("Added Clients for " + DEFAULT_CLIENT_KEY);
    }

    public synchronized static void initializeKubeClients(List<ClusterConfig> clusterConfigs) {
        tektonClientMap = new HashMap<>();
        kubernetesClientMap = new HashMap<>();

        logger.info("Initializing Kube and Tekton Clients");
        if (clusterConfigs.size() > 0) {
            for (ClusterConfig cc: clusterConfigs) {
                ConfigBuilder configBuilder = new ConfigBuilder()
                        .withMasterUrl(cc.getMasterUrl())
                        .withNamespace(cc.getDefaultNamespace());
                
                // For Kind clusters and test environments, trust self-signed certificates
                if (isKindCluster(cc.getMasterUrl()) || isTestEnvironment()) {
                    configBuilder.withTrustCerts(true).withDisableHostnameVerification(true);
                    logger.info("Enabling SSL certificate trust for cluster: " + cc.getName());
                }

                Config config = configBuilder.build();
                TektonClient tektonClient = new DefaultTektonClient(config);
                KubernetesClient kubernetesClient = new DefaultKubernetesClient(config);

                tektonClientMap.put(cc.getName(), tektonClient);
                kubernetesClientMap.put(cc.getName(), kubernetesClient);
                logger.info("Added Clients for " + cc.getName());
            }
        }

        if (!tektonClientMap.containsKey(DEFAULT_CLIENT_KEY)) {
            tektonClientMap.put(DEFAULT_CLIENT_KEY, new DefaultTektonClient());
            kubernetesClientMap.put(DEFAULT_CLIENT_KEY, new DefaultKubernetesClient());
            logger.info("Added Default Clients");
        }
    }

    public synchronized static void shutdownKubeClients() {
        if (!tektonClientMap.isEmpty() && !kubernetesClientMap.isEmpty()) {
            for (TektonClient c : tektonClientMap.values()) {
                if (c != null) {
                    c.close();
                }
            }
            for (KubernetesClient c : kubernetesClientMap.values()) {
                if (c != null) {
                    c.close();
                }
            }
        }
    }

    public static List<TektonResourceType> getKindFromInputStream(InputStream inputStream, String inputType) {
        List<TektonResourceType> kind = new ArrayList<TektonResourceType>();
        try {
            int nBytes = inputStream.available();
            byte[] bytes = new byte[nBytes];
            inputStream.read(bytes, 0, nBytes);
            String readInput = new String(bytes, StandardCharsets.UTF_8);
            logger.info("Creating from "+ inputType);

            String[] yamlLineByLine = readInput.split(System.lineSeparator());
            for (int i=0; i < yamlLineByLine.length; i++){
                String yamlLine = yamlLineByLine[i];
                if (yamlLine.startsWith("kind")){
                    String kindName = yamlLine.split(":")[1].trim().toLowerCase();
                    kind.add(TektonResourceType.valueOf(kindName));
                }
            }
        } catch(IOException e){
            logger.warning("IOException occurred "+e.toString());
        }

        return kind;
    }

    /**
     * Parses the Tekton apiVersion from YAML/JSON manifest data (e.g. "tekton.dev/v1" or "tekton.dev/v1beta1").
     *
     * @param data raw manifest bytes
     * @return "v1", "v1beta1", or null if not found / not tekton.dev (caller may treat null as v1beta1 for backward compatibility)
     */
    public static String getApiVersionFromData(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        String content = new String(data, StandardCharsets.UTF_8);
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("apiVersion:")) {
                int colon = trimmed.indexOf(':');
                String value = colon >= 0 ? trimmed.substring(colon + 1).trim() : "";
                if (value.contains("tekton.dev/v1beta1")) {
                    return "v1beta1";
                }
                if (value.contains("tekton.dev/v1")) {
                    return "v1";
                }
                return null;
            }
        }
        return null;
    }

    public static InputStream urlToByteArrayStream(URL url) {
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = url.openStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String response = "";
            StringBuffer sb = new StringBuffer();
            for (String line; (line = reader.readLine()) != null; response = sb.append(line).append("\n").toString());
            inputStream = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e){
            logger.warning("IOException occurred "+ e.toString());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.warning("IOException occurred "+ e.toString());
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warning("IOException occurred "+ e.toString());
                }
            }
        }

        return inputStream;
    }

    public synchronized static Map<String,TektonClient> getTektonClientMap(){
        return tektonClientMap;
    }

    public synchronized static Map<String,KubernetesClient> getKubernetesClientMap() {
        return kubernetesClientMap;
    }

    public synchronized static TektonClient getTektonClient(String name){
        return tektonClientMap.get(name);
    }

    public synchronized static KubernetesClient getKubernetesClient(String name) {
        return kubernetesClientMap.get(name);
    }
    
    /**
     * Determines if the cluster URL is a Kind cluster (localhost or 127.0.0.1)
     */
    private static boolean isKindCluster(String masterUrl) {
        if (masterUrl == null) {
            return false;
        }
        return masterUrl.contains("localhost") || masterUrl.contains("127.0.0.1");
    }
    
    /**
     * Determines if we're running in a test environment
     */
    private static boolean isTestEnvironment() {
        // Check for test environment indicators
        return System.getProperty("java.class.path").contains("junit") || 
               System.getProperty("java.class.path").contains("surefire") ||
               "true".equals(System.getProperty("jenkins.test.mode"));
    }
}

