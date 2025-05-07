package org.waveywaves.jenkins.plugins.tekton.client;

import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A helper class for accessing the jx-pipeline-effective binary
 */
public class ToolUtils {
    private static final Logger LOGGER = Logger.getLogger(ToolUtils.class.getName());
    private static String jxPipelineFile = System.getenv("JX_PIPELINE_EFFECTIVE_PATH");

    /**
     * @return the file name location of the jx-pipeline-effective binary
     * @throws IOException
     * @param classLoader
     */
    public static synchronized String getJXPipelineBinary(ClassLoader classLoader) throws IOException {
        // Check if the file path is already cached
        if (jxPipelineFile == null) {
            // Create a temporary fil
            File tempFile = createTemporaryFile();

            // Determine the platform and resource path
            String platform = getPlatform();
            String resourcePath = getResourcePath(platform);

            // Copy the resource to the temporary file
            copyResourceToFile(classLoader, resourcePath, tempFile);

            // Mark the file as executable
            setFileExecutable(tempFile);

            jxPipelineFile = tempFile.getPath();
            LOGGER.info("Saved jx-pipeline-effective binary to " + jxPipelineFile);
        }
        return jxPipelineFile;
    }

    // Creates a temporary file
    private static File createTemporaryFile() throws IOException {
        File tempFile = File.createTempFile("jx-pipeline-effective-", "");
        boolean deleted = tempFile.delete();
        if (!deleted) {
            LOGGER.log(Level.WARNING, "Unable to delete temporary file " + tempFile);
        }
        tempFile.deleteOnExit();
        return tempFile;
    }

    // Determines the platform (mac, windows, linux)
    private static String getPlatform() {
        if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX) {
            return "mac";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            return "windows";
        }
        return "linux";  // Default platform
    }

    // Retrieves the resource path based on the platform
    private static String getResourcePath(String platform) {
        return "org/waveywaves/jenkins/plugins/tekton/client/jxp/" + platform + "/jx-pipeline-effective";
    }

    // Copies the resource to the temporary file
    private static void copyResourceToFile(ClassLoader classLoader, String resourcePath, File tempFile) throws IOException {
        InputStream in = classLoader.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("could not find resource on classpath: " + resourcePath);
        }

        String path = tempFile.getPath();
        try {
            Files.copy(in, tempFile.toPath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to copy jx-pipeline-effective to " + path + " due to " + e);
            throw new IOException("Failed to copy jx-pipeline-effective to " + path + " cause: " + e, e);
        }
    }

    // Marks the file as executable
    private static void setFileExecutable(File file) throws IOException {
        boolean chmodSuccess = file.setExecutable(true);
        if (!chmodSuccess) {
            throw new IOException("Failed to make the file executable: " + file.getPath());
        }
    }
}
