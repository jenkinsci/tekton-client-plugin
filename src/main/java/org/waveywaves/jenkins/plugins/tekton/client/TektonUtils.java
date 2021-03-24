package org.waveywaves.jenkins.plugins.tekton.client;

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

    public synchronized static void initializeKubeClients(List<ClusterConfig> clusterConfigs) {
        tektonClientMap = new HashMap<>();
        kubernetesClientMap = new HashMap<>();

        logger.info("Initializing Kube and Tekton Clients");
        if (clusterConfigs.size() > 0) {
            for (ClusterConfig cc: clusterConfigs) {
                ConfigBuilder configBuilder = new ConfigBuilder();
                configBuilder.withMasterUrl(cc.getMasterUrl());
                configBuilder.withNamespace(cc.getDefaultNamespace());

                TektonClient tektonClient = new DefaultTektonClient(configBuilder.build());
                KubernetesClient kubernetesClient = new DefaultKubernetesClient(configBuilder.build());

                tektonClientMap.put(cc.getName(), tektonClient);
                kubernetesClientMap.put(cc.getName(), kubernetesClient);
                logger.info("Added Clients for " + cc.getName());
            }
        } else {
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
}

