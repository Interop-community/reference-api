package org.logicahealth.platform.api;

import org.springframework.boot.context.properties.ConfigurationProperties;
// import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@ConfigurationProperties("hspc.platform.api.fhir.subscription")
@Configuration
public class SubscriptionProperties {

    
    public Boolean getResthook_enabled() {
        return resthook_enabled;
      }
  
      public void setResthook_enabled(Boolean resthook_enabled) {
        this.resthook_enabled = resthook_enabled;
      }
  
      public Boolean getWebsocket_enabled() {
        return websocket_enabled;
      }
  
      public void setWebsocket_enabled(Boolean websocket_enabled) {
        this.websocket_enabled = websocket_enabled;
      }
  
      private Boolean resthook_enabled = false;
      private Boolean websocket_enabled = false;
      private Email email = null;
  
      public Email getEmail() {
        return email;
      }
  
      public void setEmail(Email email) {
        this.email = email;
      }
  
  
      public static class Email {
        public String getFrom() {
          return from;
        }
  
        public void setFrom(String from) {
          this.from = from;
        }
  
        public String getHost() {
          return host;
        }
  
        public void setHost(String host) {
          this.host = host;
        }
  
        public Integer getPort() {
          return port;
        }
  
        public void setPort(Integer port) {
          this.port = port;
        }
  
        public String getUsername() {
          return username;
        }
  
        public void setUsername(String username) {
          this.username = username;
        }
  
        public String getPassword() {
          return password;
        }
  
        public void setPassword(String password) {
          this.password = password;
        }
  
        public Boolean getAuth() {
          return auth;
        }
  
        public void setAuth(Boolean auth) {
          this.auth = auth;
        }
  
        public Boolean getStartTlsEnable() {
          return startTlsEnable;
        }
  
        public void setStartTlsEnable(Boolean startTlsEnable) {
          this.startTlsEnable = startTlsEnable;
        }
  
        public Boolean getStartTlsRequired() {
          return startTlsRequired;
        }
  
        public void setStartTlsRequired(Boolean startTlsRequired) {
          this.startTlsRequired = startTlsRequired;
        }
  
        public Boolean getQuitWait() {
          return quitWait;
        }
  
        public void setQuitWait(Boolean quitWait) {
          this.quitWait = quitWait;
        }
  
        private String from;
        private String host;
        private Integer port = 25;
        private String username;
        private String password;
        private Boolean auth = false;
        private Boolean startTlsEnable = false;
        private Boolean startTlsRequired = false;
        private Boolean quitWait = false;

    }
    
}
