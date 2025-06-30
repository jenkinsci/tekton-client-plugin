package org.waveywaves.jenkins.plugins.tekton.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Some helper methods to log errors
 */
public class LogUtils {

    public static void logStream(InputStream in, Logger logger, boolean error) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (error) {
                    logger.log(Level.WARNING, line);
                } else {
                    logger.info(line);
                }
            }
        }
    }
}