package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.DescriptorExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public abstract class AbstractTektonResource extends AbstractDescribableImpl<AbstractTektonResource> {
    protected String kind;
    protected AbstractTektonResource(String kind) {
        this.kind = kind;
    }

    public static final DescriptorExtensionList<AbstractTektonResource, Descriptor<AbstractTektonResource>> all() {
        return Jenkins.getInstance().getDescriptorList(AbstractTektonResource.class);
    }

    public Descriptor<AbstractTektonResource> getDescriptor(){
        return Jenkins.getInstance().getDescriptor(getClass());
    }

    public static class TektonResourceDescriptor extends Descriptor<AbstractTektonResource> {}
}
