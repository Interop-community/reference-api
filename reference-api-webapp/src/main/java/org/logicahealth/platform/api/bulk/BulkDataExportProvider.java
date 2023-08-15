package org.logicahealth.platform.api.bulk;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.bulk.api.IBulkDataExportSvc;
import ca.uhn.fhir.jpa.bulk.model.BulkExportResponseJson;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.PreferHeader;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.ArrayUtil;
import ca.uhn.fhir.util.JsonUtil;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.InstantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

import org.logicahealth.platform.api.bulk.BulkExportJobRunnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.logicahealth.platform.api.bulk.BulkDataExportSvcImpl;
public class BulkDataExportProvider extends ca.uhn.fhir.jpa.bulk.provider.BulkDataExportProvider{
    
	private static final Logger ourLog = LoggerFactory.getLogger(BulkDataExportProvider.class);


    @Autowired
	private IBulkDataExportSvc myBulkDataExportSvc;

	@Value("${hspc.platform.api.fhir.bulk.schedulerEnabled}")
    private boolean schedulerEnabled;

	@Override
	@Operation(name = JpaConstants.OPERATION_EXPORT, global = false /* set to true once we can handle this */, manualResponse = true, idempotent = true)
	public void export(
		@OperationParam(name = JpaConstants.PARAM_EXPORT_OUTPUT_FORMAT, min = 0, max = 1, typeName = "string") IPrimitiveType<String> theOutputFormat,
		@OperationParam(name = JpaConstants.PARAM_EXPORT_TYPE, min = 0, max = 1, typeName = "string") IPrimitiveType<String> theType,
		@OperationParam(name = JpaConstants.PARAM_EXPORT_SINCE, min = 0, max = 1, typeName = "instant") IPrimitiveType<Date> theSince,
		@OperationParam(name = JpaConstants.PARAM_EXPORT_TYPE_FILTER, min = 0, max = 1, typeName = "string") IPrimitiveType<String> theTypeFilter,
		ServletRequestDetails theRequestDetails
	) {

		String preferHeader = theRequestDetails.getHeader(Constants.HEADER_PREFER);
		PreferHeader prefer = RestfulServerUtils.parsePreferHeader(null, preferHeader);
		if (prefer.getRespondAsync() == false) {
			throw new InvalidRequestException("Must request async processing for $export");
		}
		String outputFormat = theOutputFormat != null ? theOutputFormat.getValueAsString() : null;

		Set<String> resourceTypes = null;
		if (theType != null) {
			resourceTypes = ArrayUtil.commaSeparatedListToCleanSet(theType.getValueAsString());
		}

		Date since = null;
		if (theSince != null) {
			since = theSince.getValue();
		}

		Set<String> filters = null;
		if (theTypeFilter != null) {
			filters = ArrayUtil.commaSeparatedListToCleanSet(theTypeFilter.getValueAsString());
		}

		String cacheControlHeader = theRequestDetails.getHeader(Constants.HEADER_CACHE_CONTROL);
		Boolean useCache = (cacheControlHeader != null && cacheControlHeader.equals(Constants.CACHE_CONTROL_NO_CACHE)) ? false : true;


		IBulkDataExportSvc.JobInfo outcome = myBulkDataExportSvc.submitJob(outputFormat, resourceTypes, since, filters);
		if(!schedulerEnabled){		
			((BulkDataExportSvcImpl) myBulkDataExportSvc).startWithoutScheduler();
		}


		ourLog.info("useCache in BulkExport: " + useCache);
		if (!useCache) {
			((BulkDataExportSvcImpl) myBulkDataExportSvc).cancelAndPurgeJob(outcome.getJobId());
			outcome = myBulkDataExportSvc.submitJob(outputFormat, resourceTypes, since, filters);
			if(!schedulerEnabled){		
				((BulkDataExportSvcImpl) myBulkDataExportSvc).startWithoutScheduler();
			}
			}


		String serverBase = getServerBase(theRequestDetails);
		String pollLocation = serverBase + "/" + JpaConstants.OPERATION_EXPORT_POLL_STATUS + "?" + JpaConstants.PARAM_EXPORT_POLL_STATUS_JOB_ID + "=" + outcome.getJobId();

		HttpServletResponse response = theRequestDetails.getServletResponse();

		// Add standard headers
		theRequestDetails.getServer().addHeadersToResponse(response);

		// Successful 202 Accepted
		response.addHeader(Constants.HEADER_CONTENT_LOCATION, pollLocation);
		response.setStatus(Constants.STATUS_HTTP_202_ACCEPTED);
	}

    private String getServerBase(ServletRequestDetails theRequestDetails) {
		return StringUtils.removeEnd(theRequestDetails.getServerBaseForRequest(), "/");
	}
    
}
