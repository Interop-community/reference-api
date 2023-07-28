package org.logicahealth.platform.api.bulk;

import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.bulk.api.IBulkDataExportSvc;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.apache.commons.lang3.StringUtils;

public class BulkDataExportProvider extends ca.uhn.fhir.jpa.bulk.provider.BulkDataExportProvider{
    
    @Autowired
	private IBulkDataExportSvc myBulkDataExportSvc;

    @Autowired
	private FhirContext myFhirContext;




    private String getServerBase(ServletRequestDetails theRequestDetails) {
        System.out.println(theRequestDetails.getServerBaseForRequest());
        System.out.println(theRequestDetails.getHeaders());
		return StringUtils.removeEnd(theRequestDetails.getServerBaseForRequest(), "/");
	}
    
}
