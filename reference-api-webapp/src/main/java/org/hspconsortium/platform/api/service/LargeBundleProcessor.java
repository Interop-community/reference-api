package org.hspconsortium.platform.api.service;

import org.hl7.fhir.instance.model.api.IBaseBundle;

public interface LargeBundleProcessor {

    void processLargeBundle(String bundle, String targetUrl, String notificationEmail);

}
