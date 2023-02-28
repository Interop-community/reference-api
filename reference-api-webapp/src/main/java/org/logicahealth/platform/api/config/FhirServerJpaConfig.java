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

package org.logicahealth.platform.api.config;

import java.util.Optional;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hl7.fhir.dstu2.model.Subscription;
import org.logicahealth.platform.api.SubscriptionProperties;
import org.logicahealth.platform.api.multitenant.db.DataSourceRepository;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.config.HapiFhirLocalContainerEntityManagerFactoryBean;
import ca.uhn.fhir.jpa.interceptor.CascadingDeleteInterceptor;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.jpa.subscription.channel.subscription.SubscriptionDeliveryHandlerFactory;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.IEmailSender;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.JavaMailEmailSender;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

@Configuration
@EnableTransactionManagement
public class FhirServerJpaConfig {

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private FhirContext fhirContext;

    @Autowired
    SubscriptionProperties subProperties;
    /**
     * Configure FHIR properties around the the JPA server via this bean
     */
    @Bean()
    public DaoConfig daoConfig() {
        DaoConfig retVal = new DaoConfig();
        retVal.setAllowExternalReferences(true);
        retVal.setSubscriptionEnabled(false);
        retVal.setAllowMultipleDelete(true);
        retVal.setIndexMissingFields(DaoConfig.IndexEnabledEnum.ENABLED);
        retVal.setReuseCachedSearchResultsForMillis(null);
        retVal.setAllowContainsSearches(true);
        retVal.setExpungeEnabled(true);
        retVal.setUseLegacySearchBuilder(true);

        if(subProperties != null) {
            // Subscriptions are enabled by channel type
            if (subProperties.getResthook_enabled()) {
              retVal.addSupportedSubscriptionType(org.hl7.fhir.dstu2.model.Subscription.SubscriptionChannelType.RESTHOOK);
            }
            if (subProperties.getEmail() != null) {
              retVal.addSupportedSubscriptionType(org.hl7.fhir.dstu2.model.Subscription.SubscriptionChannelType.EMAIL);

            }
            if (subProperties.getWebsocket_enabled()) {
              retVal.addSupportedSubscriptionType(org.hl7.fhir.dstu2.model.Subscription.SubscriptionChannelType.WEBSOCKET);
            }
          }

        return retVal;
    }

    @Bean
    public ModelConfig modelConfig() {
        ModelConfig modelConfig = daoConfig().getModelConfig();

        if(subProperties != null && subProperties.getEmail() != null)
        modelConfig.setEmailFromAddress(subProperties.getEmail().getFrom());

      // You can enable these if you want to support Subscriptions from your server
      if (subProperties != null && subProperties.getResthook_enabled() != null) {
        modelConfig.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.RESTHOOK);
      }

      if (subProperties  != null && subProperties.getEmail() != null) {
        modelConfig.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.EMAIL);
      }
        return modelConfig;
    }

    @Bean
    public HapiFhirLocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, MultiTenantConnectionProvider multiTenantConnectionProvider, CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
        var retVal = new HapiFhirLocalContainerEntityManagerFactoryBean();
        setLocalContainerEntityManagerFactoryBeanProperties(retVal, dataSource, multiTenantConnectionProvider, currentTenantIdentifierResolver);
        return retVal;
    }

    private void setLocalContainerEntityManagerFactoryBeanProperties(LocalContainerEntityManagerFactoryBean bean, DataSource dataSource, MultiTenantConnectionProvider multiTenantConnectionProvider, CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
        bean.setPersistenceUnitName("HAPI_PU");
        bean.setDataSource(dataSource);
        String[] packageLocations = {"ca.uhn.fhir.jpa.entity", "ca.uhn.fhir.jpa.model.entity"};
        bean.setPackagesToScan(packageLocations);
        bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        bean.setJpaProperties(jpaProperties(multiTenantConnectionProvider, currentTenantIdentifierResolver));
        bean.afterPropertiesSet();
    }

    @Bean
    public DataSource dataSource() {
        return dataSourceRepository.getDefaultDataSource();
    }

    private Properties jpaProperties(MultiTenantConnectionProvider multiTenantConnectionProvider, CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
        Properties extraProperties = new Properties();
        extraProperties.put("hibernate.dialect", org.hibernate.dialect.MySQL57Dialect.class.getName());
        extraProperties.put("hibernate.format_sql", "true");
        extraProperties.put("hibernate.show_sql", "false");
        extraProperties.put("hibernate.hbm2ddl.auto", "none");
        extraProperties.put("hibernate.jdbc.batch_size", "20");
        extraProperties.put("hibernate.cache.use_query_cache", "false");
        extraProperties.put("hibernate.cache.use_second_level_cache", "false");
        extraProperties.put("hibernate.cache.use_structured_entries", "false");
        extraProperties.put("hibernate.cache.use_minimal_puts", "false");
        extraProperties.put("hibernate.search.default.directory_provider", "filesystem");
        extraProperties.put("hibernate.search.default.indexBase", "target/lucenefiles");
        extraProperties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
        extraProperties.put("hibernate.search.autoregister_listeners", "false"); // set to false to disable lucene
        // multi-tenant properties
        extraProperties.put(Environment.MULTI_TENANT, MultiTenancyStrategy.DATABASE);
        extraProperties.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        extraProperties.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);

        return extraProperties;
    }

    /**
     * Do some fancy logging to create a nice access log that has details about each incoming request.
     */
    public LoggingInterceptor loggingInterceptor() {
        LoggingInterceptor retVal = new LoggingInterceptor();
        retVal.setLoggerName("fhirtest.access");
        retVal.setMessageFormat(
                "Path[${servletPath}] Source[${requestHeader.x-forwarded-for}] Operation[${operationType} ${operationName} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}] ResponseEncoding[${responseEncodingNoDefault}]");
        retVal.setLogExceptions(true);
        retVal.setErrorMessageFormat("ERROR - ${requestVerb} ${requestUrl}");
        return retVal;
    }

    /**
     * This interceptor adds some pretty syntax highlighting in responses when a browser is detected
     */
    @Bean(autowire = Autowire.BY_TYPE)
    public ResponseHighlighterInterceptor responseHighlighterInterceptor() {
        ResponseHighlighterInterceptor retVal = new ResponseHighlighterInterceptor();
        return retVal;
    }

    @Bean("hapiTransactionManager")
    @Primary
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager retVal = new JpaTransactionManager();
        retVal.setEntityManagerFactory(entityManagerFactory);
        return retVal;
    }

    @Bean
    public CascadingDeleteInterceptor cascadingDeleteInterceptor (DaoRegistry theDaoRegistry, IInterceptorBroadcaster theInterceptorBroadcaster) {
        return new CascadingDeleteInterceptor(fhirContext, theDaoRegistry, theInterceptorBroadcaster);
    }

    @Bean()
    public IEmailSender emailSender(Optional<SubscriptionDeliveryHandlerFactory> subscriptionDeliveryHandlerFactory) {
      if (subProperties != null && subProperties.getEmail() != null) {
        JavaMailEmailSender retVal = new JavaMailEmailSender();

        SubscriptionProperties.Email email = subProperties.getEmail();
        retVal.setSmtpServerHostname(email.getHost());
        retVal.setSmtpServerPort(email.getPort());
        retVal.setSmtpServerUsername(email.getUsername());
        retVal.setSmtpServerPassword(email.getPassword());
        retVal.setAuth(email.getAuth());
        retVal.setStartTlsEnable(email.getStartTlsEnable());
        retVal.setStartTlsRequired(email.getStartTlsRequired());
        retVal.setQuitWait(email.getQuitWait());

        if(subscriptionDeliveryHandlerFactory.isPresent())
         subscriptionDeliveryHandlerFactory.get().setEmailSender(retVal);


        return retVal;
      }

      return null;
    }

}
