package com.nike.cerberus.event;

import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Data
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuditLoggingFilterDetails {

  // TODO remove once testing is complete
  private String metadata = "Routed through AuditLoggingFilter";

  private String className;

  private String principalName;

  private String action;

  private boolean success = true;
}
