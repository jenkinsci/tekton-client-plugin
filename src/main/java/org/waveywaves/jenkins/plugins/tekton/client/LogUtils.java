package org.waveywaves.jenkins.plugins.tekton.client;

import com.google.common.io.LineReader;

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
        LineReader reader = new LineReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (error) {
                logger.info(line);
            } else {
                logger.log(Level.WARNING, line);
            }
        }
    }
}
