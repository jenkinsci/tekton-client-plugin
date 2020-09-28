package org.waveywaves.jenkins.plugins.tekton.client;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TektonUtils {
    private static final Logger logger = Logger.getLogger(TektonUtils.class.getName());

    private static TektonClient tektonClient;
    private static KubernetesClient kubernetesClient;

    public enum TektonResourceType {
        task,
        taskrun,
        pipeline,
        pipelinerun,
        pipelineresource
    }

    public synchronized static void initializeKubeClients(String serverUrl) {
        if (serverUrl != null && !serverUrl.isEmpty()) {
            logger.info("ServerUrl has been passed to Tekton Client ");
        }
        tektonClient = new DefaultTektonClient();
        kubernetesClient = new DefaultKubernetesClient();
        String namespace = tektonClient.getNamespace();
        logger.info("Running in namespace "+namespace);
    }

    public synchronized static void shutdownKubeClients() {
        if (tektonClient != null) {
            tektonClient.close();
            tektonClient = null;
        }
        if (kubernetesClient != null) {
            kubernetesClient.close();
            kubernetesClient = null;
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

    public synchronized static TektonClient getTektonClient(){
        return tektonClient;
    }

    public synchronized static KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }
}
