package org.waveywaves.jenkins.plugins.tekton.client.build.create;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class TektonParam extends AbstractDescribableImpl<TektonParam> {
    private final String name;
    private final String description;
    private Param param;

    @DataBoundConstructor
    public TektonParam(final String name,
                       final String description,
                       Param param) {
        this.name = name;
        this.description = description;
        this.param = param;
    }

    public TektonParam readResolve() {
        if (param == null) {
            this.param = new StringParam("");
        }
        return this;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Param getParam() {
        return this.param;
    }

    public static DescriptorExtensionList<Param, Descriptor<Param>> getParamDescriptors() {
        return Jenkins.getInstance().getDescriptorList(Param.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TektonParam> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "param";
        }

        public static DescriptorExtensionList<Param, ParamDescriptor> getParamDescriptors() {
            return Jenkins.getInstance().getDescriptorList(Param.class);
        }
    }

    public static abstract class Param extends AbstractDescribableImpl<Param> {
        protected String name;
        protected Param(String name) { this.name = name; }

        public Descriptor<Param> getDescriptor() {
            return Jenkins.getInstance().getDescriptor(getClass());
        }
    }

    public static class ParamDescriptor extends Descriptor<Param> {}

    public static class StringParam extends Param {
        private final String value;
        @DataBoundConstructor public StringParam(final String value) {
            super("string");
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Extension
        public static final class DescriptorImpl extends ParamDescriptor {}
    }

    public static class ArrayParam extends Param {
        private boolean yellow;
        @DataBoundConstructor public ArrayParam(boolean yellow) {
            super("array");
            this.yellow = yellow;
        }
        @Extension
        public static final class DescriptorImpl extends ParamDescriptor {}
    }
}
