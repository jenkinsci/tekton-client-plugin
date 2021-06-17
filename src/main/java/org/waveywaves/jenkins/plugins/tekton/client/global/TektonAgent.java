package org.waveywaves.jenkins.plugins.tekton.client.global;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.ComputerLauncher;

import java.io.IOException;

public class TektonAgent extends AbstractCloudSlave {
    public TektonAgent(@NonNull String name, String remoteFS, ComputerLauncher launcher) throws Descriptor.FormException, IOException {
        super(name, remoteFS, launcher);
    }

    @Override
    public AbstractCloudComputer createComputer() {
        return null;
    }

    @Override
    protected void _terminate(TaskListener taskListener) throws IOException, InterruptedException {

    }
}
