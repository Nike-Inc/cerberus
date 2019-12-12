package com.nike.cerberus.event;

import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.EventProcessorService;
import com.nike.cerberus.util.SdbAccessRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static com.nike.cerberus.CerberusHttpHeaders.*;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

  private final EventProcessorService eventProcessorService;
  private final SdbAccessRequest sdbAccessRequest;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Autowired
  public AuditLoggingFilter(EventProcessorService eventProcessorService,
                            SdbAccessRequest sdbAccessRequest,
                            AuditLoggingFilterDetails auditLoggingFilterDetails) {

    this.eventProcessorService = eventProcessorService;
    this.sdbAccessRequest = sdbAccessRequest;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    filterChain.doFilter(request, response);

    var principal = Optional.ofNullable((CerberusPrincipal) SecurityContextHolder.getContext().getAuthentication());

    var event = AuditableEvent.Builder.create()
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
