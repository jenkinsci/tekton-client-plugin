package org.waveywaves.jenkins.plugins.tekton.client;

import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.Task;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TektonUtils {
    private static final Logger logger = Logger.getLogger(TektonUtils.class.getName());

    private static TektonClient tektonClient;

    public enum TektonResourceType {
        task,
        taskrun,
        pipeline,
        pipelinerun
    }


    public synchronized static void initializeTektonClient(String serverUrl) {
        if (serverUrl != null && !serverUrl.isEmpty()) {
            logger.info("ServerUrl has been passed to Tekton Client ");
        }
        tektonClient = new DefaultTektonClient();
        String namespace = tektonClient.getNamespace();
        logger.info("Running in namespace "+namespace);
    }

    public synchronized static void shutdownTektonClient() {
        if (tektonClient != null) {
            tektonClient.close();
            tektonClient = null;
        }
    }

    public static List<TektonResourceType> getKindFromInputStream(InputStream inputStream, String inputType) throws IOException {
        int nBytes = inputStream.available();
        byte[] bytes = new byte[nBytes];
        inputStream.read(bytes, 0, nBytes);
        String readInput = new String(bytes, StandardCharsets.UTF_8);
        logger.info("Creating from "+ inputType);

        List<TektonResourceType> kind = new ArrayList<TektonResourceType>();
        String[] yamlLineByLine = readInput.split(System.lineSeparator());
        for (int i=0; i < yamlLineByLine.length; i++){
            String yamlLine = yamlLineByLine[i];
            if (yamlLine.startsWith("kind")){
                String kindName = yamlLine.split(":")[1].trim().toLowerCase();
                 kind.add(TektonResourceType.valueOf(kindName));
            }
        }
        return kind;
    }

    public static InputStream urlToByteArrayStream(URL url) throws IOException {
        InputStream inputStream;
        inputStream = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String response = new String();
        for (String line; (line = reader.readLine()) != null; response += line+"\n");
        inputStream = new ByteArrayInputStream(response.getBytes());
        reader.close();

        return inputStream;
    }

    public synchronized static TektonClient getTektonClient(){
        return tektonClient;
    }
}
