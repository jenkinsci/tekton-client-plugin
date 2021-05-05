package org.waveywaves.jenkins.plugins.tekton.client.build;

import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksPublisher;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FakeChecksPublisher extends ChecksPublisher {

    private int counter = 0;
    private List<ChecksDetails> details = new ArrayList<>();

    @Override
    public void publish(ChecksDetails detail) {
        details.add(detail);
        counter++;
    }

    public int getCounter() {
        return counter;
    }

    public void validate() {
        for (ChecksDetails c: details) {
            System.out.println("[FakeChecksPublisher] " + c);
            assertThat(c, is(notNullValue()));
            assertThat(c.getName().get(), is("tekton"));
            assertThat(c.getConclusion(), is(notNullValue()));
            assertThat(c.getStatus(), is(notNullValue()));

            assertThat(c.getOutput(), is(notNullValue()));
            assertThat(c.getOutput().get(), is(notNullValue()));
            assertThat(c.getOutput().get().getTitle().get(), is(notNullValue()));
            assertThat(c.getOutput().get().getSummary().get(), is(notNullValue()));
            //assertThat(c, is(notNullValue()));
        }
    }
}
