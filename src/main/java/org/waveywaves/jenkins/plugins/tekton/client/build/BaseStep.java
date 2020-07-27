package org.waveywaves.jenkins.plugins.tekton.client.build;

import hudson.tasks.Builder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.tekton.client.TektonClient;
import jenkins.tasks.SimpleBuildStep;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils;
import org.waveywaves.jenkins.plugins.tekton.client.TektonUtils.TektonResourceType;

public abstract class BaseStep extends Builder implements SimpleBuildStep {
    protected transient MixedOperation resourceSpecificClient = null;
    protected transient TektonClient tektonClient = TektonUtils.getTektonClient();
}
