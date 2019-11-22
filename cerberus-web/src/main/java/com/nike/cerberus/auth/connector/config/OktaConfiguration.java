package com.nike.cerberus.auth.connector.config;

import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.auth.connector.okta.OktaAuthConnector;
import com.nike.cerberus.auth.connector.okta.OktaConfigurationProperties;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.client.AuthenticationClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty("cerberus.userAuth.connector.okta.enabled")
@ComponentScan({"com.nike.cerberus.auth.connector.okta"})
public class OktaConfiguration {

  @Bean
  @ConfigurationProperties("cerberus.user-auth.connector.okta")
  public OktaConfigurationProperties oktaConfigurationProperties() {
    return new OktaConfigurationProperties();
  }

  @Bean
  AuthenticationClient authenticationClient(OktaConfigurationProperties oktaConfigurationProperties) {
    System.setProperty("okta.client.token", oktaConfigurationProperties.getApiKey());
    return AuthenticationClients.builder()
      .setOrgUrl(oktaConfigurationProperties.getBaseUrl())
      .build();
  }

  @Bean
  AuthConnector authConnector(OktaAuthConnector oktaAuthConnector) {
    return oktaAuthConnector;
  }
}
