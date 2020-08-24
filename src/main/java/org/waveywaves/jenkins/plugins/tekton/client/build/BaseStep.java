package org.waveywaves.jenkins.plugins.tekton.client.build;

import hudson.tasks.Builder;
import io.fabric8.tekton.client.TektonClient;
import jenkins.tasks.SimpleBuildStep;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;

public abstract class BaseStep extends Builder implements SimpleBuildStep {
    protected transient TektonClient tektonClient;

    public void setTektonClient(TektonClient tc) {
        this.tektonClient = tc;
    }
}
