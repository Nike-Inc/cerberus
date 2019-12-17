package com.nike.cerberus.event;

import static com.nike.cerberus.CerberusHttpHeaders.*;

import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AuditUtils {

  /**
   * Method that can build the base Auditable event, used internally here for endpoints that extend
   * AuditableController
   *
   * @return an AuditableEvent Builder with base attributes populated
   */
  public static AuditableEvent.Builder createBaseAuditableEvent(String className) {

    var request =
        Optional.ofNullable(RequestContextHolder.getRequestAttributes())
            .filter(
                requestAttributes ->
                    ServletRequestAttributes.class.isAssignableFrom(requestAttributes.getClass()))
            .map(requestAttributes -> ((ServletRequestAttributes) requestAttributes))
            .map(ServletRequestAttributes::getRequest)
            .orElseThrow(() -> new RuntimeException("Failed to get request from context"));

    return AuditableEvent.Builder.create()
        .withName(className + " Endpoint Called")
        .withPrincipal(SecurityContextHolder.getContext().getAuthentication())
        .withMethod(request.getMethod())
        .withPath(request.getServletPath())
        .withIpAddress(getXForwardedClientIp(request))
        .withXForwardedFor(getXForwardedCompleteHeader(request))
        .withClientVersion(getClientVersion(request))
        .withOriginatingClass(className);
  }
}
