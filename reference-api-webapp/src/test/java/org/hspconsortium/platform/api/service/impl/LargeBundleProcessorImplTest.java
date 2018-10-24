package org.hspconsortium.platform.api.service.impl;

import org.apache.commons.io.IOUtils;
import org.hspconsortium.platform.api.service.LargeBundleProcessor;
import org.junit.Test;

import java.io.IOException;

public class LargeBundleProcessorImplTest {


    private static final String NOTIFICATION_EMAIL = "mike@interopion.com";
    private static final String TARGET_URL = "";

    @Test
    public void processLargeBundleTest() throws IOException, InterruptedException {
        String bundleString = IOUtils.toString(this.getClass().getResourceAsStream("/sandbox-export.json"), "UTF-8");

        LargeBundleProcessor largeBundleProcessor = new LargeBundleProcessorImpl();

        largeBundleProcessor.processLargeBundle(bundleString, TARGET_URL, NOTIFICATION_EMAIL);

        System.out.println("Test logic is complete, processing should be happening on a new thread.");
    }
}