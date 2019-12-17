package com.nike.cerberus.event;

import static com.nike.cerberus.CerberusHttpHeaders.*;

import com.nike.cerberus.service.EventProcessorService;
import com.nike.cerberus.util.SdbAccessRequest;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

  private final EventProcessorService eventProcessorService;
  private final SdbAccessRequest sdbAccessRequest;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Autowired
  public AuditLoggingFilter(
      EventProcessorService eventProcessorService,
      SdbAccessRequest sdbAccessRequest,
      AuditLoggingFilterDetails auditLoggingFilterDetails) {

    this.eventProcessorService = eventProcessorService;
    this.sdbAccessRequest = sdbAccessRequest;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    filterChain.doFilter(request, response);

    var event =
        AuditableEvent.Builder.create()
            .withName("TODO")
            .withPrincipal(SecurityContextHolder.getContext().getAuthentication())
            .withMethod(request.getMethod())
            .withPath(request.getServletPath())
            .withIpAddress(getXForwardedClientIp(request))
            .withXForwardedFor(getXForwardedCompleteHeader(request))
            .withClientVersion(getClientVersion(request))
            .withOriginatingClass(this.getClass().getSimpleName());

    eventProcessorService.ingestEvent(event.build());
  }
}
