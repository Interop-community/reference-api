/**
 *  * #%L
 *  *
 *  * %%
 *  * Copyright (C) 2014-2020 Healthcare Services Platform Consortium
 *  * %%
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  * #L%
 */

package org.hspconsortium.platform.api.smart.fhir;

import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.r5.JpaConformanceProviderR5;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.Meta;

import javax.servlet.http.HttpServletRequest;

public class HspcConformanceProviderR5 extends JpaConformanceProviderR5 {
    private MetadataRepositoryR5 metadataRepository;

    public HspcConformanceProviderR5(RestfulServer theRestfulServer, IFhirSystemDao<Bundle, Meta> theSystemDao, DaoConfig theDaoConfig, MetadataRepositoryR5 metadataRepository, ISearchParamRegistry theSearchParamRegistry) {
        super(theRestfulServer, theSystemDao, theDaoConfig, theSearchParamRegistry);
        this.metadataRepository = metadataRepository;
    }

    public void setMetadataRepository(MetadataRepositoryR5 metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    @Override
    public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        CapabilityStatement capabilityStatement = super.getServerConformance(theRequest, theRequestDetails);
        if (theRequest.getRequestURI().split("/")[2].equals("data")) { // If someone can think of something better, please implement
            return this.metadataRepository.addCapabilityStatement(capabilityStatement);
        }
        return capabilityStatement;
    }
}
