package org.logicahealth.platform.api.provider;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.dao.IFulltextSearchSvc;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.util.ParametersUtil;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.concurrent.Executor;

public class JpaSystemProviderR4 extends ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4 {

	@Autowired()
	@Qualifier("mySystemDaoR4")
	private IFhirSystemDao<Bundle, Meta> mySystemDao;

	@Autowired(required = false)
	private IFulltextSearchSvc mySearchDao;

	@Autowired
	@Qualifier("taskExecutor")
	private Executor executor;

	@Override
	@Operation(name = MARK_ALL_RESOURCES_FOR_REINDEXING, idempotent = true, returnParameters = {
		@OperationParam(name = "status")
	})
	public IBaseResource markAllResourcesForReindexing(
		@OperationParam(name="type", min = 0, max = 1, typeName = "code") IPrimitiveType<String> theType
	) {

		if (theType != null && isNotBlank(theType.getValueAsString())) {
			getResourceReindexingSvc().markAllResourcesForReindexing(theType.getValueAsString());
		} else {
			getResourceReindexingSvc().markAllResourcesForReindexing();
		}

		executor.execute(() -> {
			getResourceReindexingSvc().runReindexingPass();
		});

		IBaseParameters retVal = ParametersUtil.newInstance(getContext());

		IPrimitiveType<?> string = ParametersUtil.createString(getContext(), "Marked resources");
		ParametersUtil.addParameterToParameters(getContext(), retVal, "status", string);

		return retVal;
	}

}
