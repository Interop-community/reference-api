/**
 * * #%L
 * *
 * * %%
 * * Copyright (C) 2014-2020 Healthcare Services Platform Consortium
 * * %%
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * *
 * *      http://www.apache.org/licenses/LICENSE-2.0
 * *
 * * Unless required by applicable law or agreed to in writing, software
 * * distributed under the License is distributed on an "AS IS" BASIS,
 * * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * * See the License for the specific language governing permissions and
 * * limitations under the License.
 * * #L%
 */

package org.logicahealth.platform.api;

import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity(debug = false)
@EnableCaching
@SpringBootApplication
@EnableAsync
@EnableEncryptableProperties
@EnableBatchProcessing
public class HSPCReferenceApiMultitenantApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(HSPCReferenceApiMultitenantApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(HSPCReferenceApiMultitenantApplication.class);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @ConditionalOnMissingBean
    public ScheduledExecutorFactoryBean scheduledExecutorService() {
        ScheduledExecutorFactoryBean b = new ScheduledExecutorFactoryBean();
        b.setPoolSize(5);
        return b;
    }

    @Bean()
    public PartitionSettings getPartitionSettings() {
        var partitionSettings = new PartitionSettings();
        partitionSettings.setPartitioningEnabled(false);
        return partitionSettings;
    }

//    @Bean(name="hapiJpaTaskExecutor")
//    public AsyncTaskExecutor taskScheduler() {
//        ConcurrentTaskScheduler retVal = new ConcurrentTaskScheduler();
//        retVal.setConcurrentExecutor(scheduledExecutorService().getObject());
//        retVal.setScheduledExecutor(scheduledExecutorService().getObject());
//        return retVal;
//    }

}
