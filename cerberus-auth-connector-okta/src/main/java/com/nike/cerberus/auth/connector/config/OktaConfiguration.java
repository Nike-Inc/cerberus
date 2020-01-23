/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
@ConditionalOnProperty("cerberus.auth.user.connector.okta.enabled")
@ComponentScan({"com.nike.cerberus.auth.connector.okta"})
public class OktaConfiguration {

  @Bean
  @ConfigurationProperties("cerberus.auth.user.connector.okta")
  public OktaConfigurationProperties oktaConfigurationProperties() {
    return new OktaConfigurationProperties();
  }

  @Bean
  AuthenticationClient authenticationClient(
      OktaConfigurationProperties oktaConfigurationProperties) {
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
