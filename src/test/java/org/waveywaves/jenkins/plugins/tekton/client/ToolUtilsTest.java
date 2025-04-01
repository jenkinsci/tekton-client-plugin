package org.waveywaves.jenkins.plugins.tekton.client;

import org.junit.Test;

import java.io.File;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for the ToolUtils class.
 */
public class ToolUtilsTest {

    private static final Logger logger = Logger.getLogger(ToolUtilsTest.class.getName());

    @Test
    public void testToolUtils() throws Exception {
        String path = ToolUtils.getJXPipelineBinary(ToolUtilsTest.class.getClassLoader());

        assertThat(path).isNotEmpty();

        // Create a File object for the path
        File file = new File(path);

        // Assert that the file exists and is indeed a file
        assertThat(file).isFile();

        // Log the binary file path and its size
        logger.info("Got jx-pipeline-effective binary at " + path + " with size " + file.length());

        // Ensure the file is executable (added check to confirm the file is executable)
        assertThat(file.canExecute()).isTrue();
    }
}
