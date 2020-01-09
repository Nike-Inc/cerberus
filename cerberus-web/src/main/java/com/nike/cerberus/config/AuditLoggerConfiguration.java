package com.nike.cerberus.config;

import com.nike.cerberus.event.AuditableEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("cerberus.audit.logger.enabled")
@ComponentScan({"com.nike.cerberus.event.listener"})
public class AuditLoggerConfiguration implements ApplicationListener<AuditableEvent> {

  @Override
  public void onApplicationEvent(AuditableEvent event) {}
}
