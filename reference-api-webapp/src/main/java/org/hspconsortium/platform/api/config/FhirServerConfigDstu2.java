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

package org.hspconsortium.platform.api.config;


import ca.uhn.fhir.jpa.api.svc.ISearchCoordinatorSvc;
import ca.uhn.fhir.jpa.config.BaseJavaConfigDstu2;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import org.hspconsortium.platform.api.search.LogicaSearchCoordinatorSvcImpl;
import org.hspconsortium.platform.api.search.MockDstu2TermDeferredStorageSvc;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dstu2")
public class FhirServerConfigDstu2 extends BaseJavaConfigDstu2 {

    @Value("${hspc.platform.api.fhir.maxPageSize}")
    private Integer maxPageSize;

    @Value("${hspc.platform.api.fhir.defaultPageSize}")
    private Integer defaultPageSize;

    @Bean(autowire = Autowire.BY_TYPE)
    @Primary
    public ISearchCoordinatorSvc searchCoordinatorSvc() {
        return new LogicaSearchCoordinatorSvcImpl();
    }

    @Bean
    public MockDstu2TermDeferredStorageSvc myDeferredStorageSvc(){
        return new MockDstu2TermDeferredStorageSvc();
    }

    @Bean
    public DatabaseBackedPagingProvider databaseBackedPagingProvider(){
        DatabaseBackedPagingProvider pagingProvider = super.databaseBackedPagingProvider();
        pagingProvider.setDefaultPageSize(defaultPageSize);
        pagingProvider.setMaximumPageSize(maxPageSize);
        return pagingProvider;
    }
}
