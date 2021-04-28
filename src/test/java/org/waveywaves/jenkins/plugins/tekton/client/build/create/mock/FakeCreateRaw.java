package org.waveywaves.jenkins.plugins.tekton.client.build.create.mock;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 *
 */
public class FakeCreateRaw extends CreateRawMock {
    private String name = "fakeName";
    private String lastResource;

    public FakeCreateRaw(String input, String inputType) {
        super(input, inputType);
    }

    @Override
    public String createTaskRun(InputStream inputStream) {
        return createResource(inputStream);
    }

    @Override
    public String createTask(InputStream inputStream) {
        return createResource(inputStream);
    }

    @Override
    public String createPipeline(InputStream inputStream) {
        return createResource(inputStream);
    }

    @Override
    public String createPipelineRun(InputStream inputStream) {
        return createResource(inputStream);
    }


    protected String createResource(InputStream inputStream) {
        try {
            lastResource = IOUtils.toString(inputStream, Charset.defaultCharset());
            return name;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLastResource() {
        return lastResource;
    }
}
