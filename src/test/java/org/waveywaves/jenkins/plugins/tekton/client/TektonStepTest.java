package org.waveywaves.jenkins.plugins.tekton.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

// These are the imports you were missing:
import org.waveywaves.jenkins.plugins.tekton.client.build.create.CreateRaw;
import org.waveywaves.jenkins.plugins.tekton.client.build.create.TektonStep;
import org.waveywaves.jenkins.plugins.tekton.client.build.delete.DeleteRaw;

public class TektonStepTest {

    @Test
    public void checkCreateRawNamespace() {
        // Validating namespace field for PR #495
        // We add "dummy" text here because CreateRaw needs inputs to work
        CreateRaw createStep = new CreateRaw("dummy-input", "yaml");
        String sampleNs = "test-ns-1";

        createStep.setNamespace(sampleNs);

        assertEquals(sampleNs, createStep.getNamespace(), "Namespace should match in CreateRaw");
    }

    @Test
    public void checkDeleteRawNamespace() {
        // We add "dummy" text here because DeleteRaw needs inputs to work
        DeleteRaw deleteStep = new DeleteRaw("pod", "my-cluster", null);
        String sampleNs = "delete-ns-1";

        deleteStep.setNamespace(sampleNs);

        assertEquals(sampleNs, deleteStep.getNamespace(), "Namespace should match in DeleteRaw");
    }

    @Test
    public void testGetNamespace() {
        // 1. Arrange: Create a step with a dummy namespace matching your exact constructor
        String expectedNamespace = "my-test-namespace";
        TektonStep step = new TektonStep(
                "test-name",      // name
                "test-image",     // image
                null,             // args
                null,             // command
                null,             // envs
                "test-script",    // script
                false,            // tty
                "test-dir",       // workingDir
                expectedNamespace // your new namespace!
        );

        // 2. Act & Assert: Verify the getter returns the exact same string
        assertEquals(expectedNamespace, step.getNamespace());
    }
}