package org.logicahealth.platform.api.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.jpa.subscription.match.deliver.email.IEmailSender;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.SubscriptionDeliveringEmailSubscriber;
import ca.uhn.fhir.jpa.subscription.model.CanonicalSubscription;
import ca.uhn.fhir.jpa.subscription.model.ResourceDeliveryMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
// import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionRegistry;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionLoader;
// import ca.uhn.fhir.jpa.model.sched.ScheduledJobDefinition;
// import org.apache.commons.lang3.time.DateUtils;
// import ca.uhn.fhir.jpa.model.sched.ISchedulerService;

import javax.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


public class SubscriptionProcessorConfig extends ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig {

  private static final Logger myLog = LoggerFactory.getLogger(SubscriptionProcessorConfig.class);
  
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
        public void handleMessage(ResourceDeliveryMessage theMessageIn) throws Exception {
        ResourceDeliveryMessage theMessage = (ResourceDeliveryMessage) new myResourceDeliveryMessage(theMessageIn);
        CanonicalSubscription subscription = theMessage.getSubscription();
        
        //setting header to the subject of the email
        if (subscription.getEmailDetails().getSubjectTemplate() == null)
            if(!subscription.getHeaders().isEmpty())
                subscription.getEmailDetails().setSubjectTemplate(String.join(", ", subscription.getHeaders()));


        theMessage.setSubscription(subscription);

        super.handleMessage(theMessage);
        }
  }

    public class mySubscriptionLoader extends SubscriptionLoader{
    
    @Override
    @PostConstruct
    public void scheduleJob() {
      System.out.println("Disabled sync job");
    }
  
  }

    @Override
    @Bean
    public SubscriptionLoader subscriptionLoader() {
        return (SubscriptionLoader) new mySubscriptionLoader();
    }


    public class myResourceDeliveryMessage extends ResourceDeliveryMessage{
      private String myPayloadString;
      private String myPayloadId;

      public myResourceDeliveryMessage(ResourceDeliveryMessage theMessage) {
       super();
       this.setSubscription(theMessage.getSubscription());
       this.myPayloadId = theMessage.getPayloadId();
       this.myPayloadString = String.format("Please log into portal to view details of FHIR resourceId: %s.", myPayloadId);
      }

      @Override 
      public String getPayloadString() {
        if (this.myPayloadString != null) {
          return this.myPayloadString;
        }
    
        return "";
      }
    }



  //   @Override
  //   @Bean
  //   public SubscriptionRegistry subscriptionRegistry() {
  //       return (SubscriptionRegistry) new mySubscriptionRegistry();
  //   }

  // public class mySubscriptionRegistry extends SubscriptionRegistry{

  //   @Override
  //   public void unregisterSubscriptionIfRegistered(String theSubscriptionId) {

  //     myLog.info("Blocking Unregistration Subscription: " + theSubscriptionId);

  //       // System.out.printf("%s is calling to unregister %s\n", 
  //       //     Arrays.toString(Thread.currentThread().getStackTrace()).replace(',','\n'), theSubscriptionId );

  //   }

  // }
}
