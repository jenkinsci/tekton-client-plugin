package org.waveywaves.jenkins.plugins.tekton.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;

/**
 */
class LogUtilsTest {
    String expected = "hello\nworld";
    boolean verbose = System.getenv("TEST_VERBOSE") == "true";

    @Test
    void testLogUtilsInfo() throws Exception {
        assertLogOutput(false, Level.INFO);
    }

    @Test
    void testLogUtilsError() throws Exception {
        assertLogOutput(true, Level.WARNING);
    }

    protected void assertLogOutput(boolean errorFlag, Level expectedLevel) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(expected.getBytes());
        Logger log = Logger.getLogger(getClass().getName());
        FakeLogFilter filter = new FakeLogFilter();
        log.setFilter(filter);
        LogUtils.logStream(in, log, errorFlag);


        List<LogRecord> records = filter.getRecords();

        assertThat(records).hasSize(2).extracting("message").containsSequence("hello", "world");

        for (LogRecord record : records) {
            assertThat(record.getLevel()).isEqualTo(expectedLevel);
            if (verbose) {
                System.out.println("got log level " + record.getLevel() + " message: " + record.getMessage());
            }
        }
    }
}
