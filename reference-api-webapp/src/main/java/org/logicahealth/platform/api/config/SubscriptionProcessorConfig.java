package org.logicahealth.platform.api.config;

import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.jpa.subscription.match.deliver.email.IEmailSender;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.SubscriptionDeliveringEmailSubscriber;
import ca.uhn.fhir.jpa.subscription.model.CanonicalSubscription;
import ca.uhn.fhir.jpa.subscription.model.ResourceDeliveryMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

public class SubscriptionProcessorConfig extends ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig {

    @Override
    @Bean
	@Scope("prototype")
	public SubscriptionDeliveringEmailSubscriber subscriptionDeliveringEmailSubscriber(IEmailSender theEmailSender) {
        return (SubscriptionDeliveringEmailSubscriber) new mySubscriptionDeliveringEmailSubscriber(theEmailSender);
	}

    public class mySubscriptionDeliveringEmailSubscriber extends SubscriptionDeliveringEmailSubscriber{

        @Autowired
        public mySubscriptionDeliveringEmailSubscriber(IEmailSender theEmailSender) {
        super(theEmailSender);
        }

        @Override
        public void handleMessage(ResourceDeliveryMessage theMessage) throws Exception {
        CanonicalSubscription subscription = theMessage.getSubscription();
        
        //setting header to the subject of the email
        if (subscription.getEmailDetails().getSubjectTemplate() == null)
            if(!subscription.getHeaders().isEmpty())
                subscription.getEmailDetails().setSubjectTemplate(String.join(", ", subscription.getHeaders()));


        theMessage.setSubscription(subscription);

        super.handleMessage(theMessage);
        }
  }
}
