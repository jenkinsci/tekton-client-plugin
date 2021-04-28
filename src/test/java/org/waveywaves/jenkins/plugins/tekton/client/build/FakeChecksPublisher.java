package org.waveywaves.jenkins.plugins.tekton.client.build;

import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksPublisher;

public class FakeChecksPublisher extends ChecksPublisher {

    private int counter = 0;

    @Override
    public void publish(ChecksDetails details) {
        counter++;
    }

    public int getCounter() {
        return counter;
    }
}
