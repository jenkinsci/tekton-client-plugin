package org.waveywaves.jenkins.plugins.tekton.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * A helper class for testing logging output
 */
public class FakeLogFilter implements Filter {
    private List<LogRecord> records = new ArrayList<>();

    @Override
    public boolean isLoggable(LogRecord record) {
        records.add(record);
        return true;
    }

    /**
     * @return the records we have seen so far
     */
    public List<LogRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }
}
