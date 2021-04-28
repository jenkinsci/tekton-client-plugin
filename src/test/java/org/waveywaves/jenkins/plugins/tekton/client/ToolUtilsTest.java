package org.waveywaves.jenkins.plugins.tekton.client;

import org.junit.Test;

import java.io.File;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ToolUtilsTest {
    private static final Logger logger = Logger.getLogger(ToolUtilsTest.class.getName());

    @Test
    public void testToolUtils() throws Exception {
        String path = ToolUtils.getJXPipelineBinary(ToolUtilsTest.class.getClassLoader());
        assertThat(path).isNotEmpty();

        File file = new File(path);
        assertThat(file).isFile();
        logger.info("got jx pipeline binary " + path + " with size " + file.length());
    }
}
