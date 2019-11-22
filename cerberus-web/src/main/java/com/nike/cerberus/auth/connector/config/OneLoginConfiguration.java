package com.nike.cerberus.auth.connector.config;

import com.nike.cerberus.auth.connector.onelogin.OneLoginConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty("cerberus.userAuth.connector.oneLogin.enabled")
@ComponentScan({"com.nike.cerberus.auth.connector.onelogin"})
public class OneLoginConfiguration {

  @Bean
  @ConfigurationProperties("cerberus.user-auth.connector.one-login")
  public OneLoginConfigurationProperties oneLoginConfigurationProperties() {
    return new OneLoginConfigurationProperties();
  }
}
