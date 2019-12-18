package com.nike.cerberus.event;

import static com.nike.cerberus.CerberusHttpHeaders.*;

import com.nike.cerberus.util.SdbAccessRequest;
import com.nike.wingtips.Span;
import com.nike.wingtips.Tracer;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

  private final SdbAccessRequest sdbAccessRequest;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;
  private final BuildProperties buildProperties;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public AuditLoggingFilter(
      SdbAccessRequest sdbAccessRequest,
      AuditLoggingFilterDetails auditLoggingFilterDetails,
      BuildProperties buildProperties,
      ApplicationEventPublisher applicationEventPublisher) {

    this.sdbAccessRequest = sdbAccessRequest;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
    this.buildProperties = buildProperties;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  private String getTraceId() {
    Span span = Tracer.getInstance().getCurrentSpan();
    return span == null ? AuditableEventContext.UNKNOWN : span.getTraceId();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    filterChain.doFilter(request, response);

    // TODO Handle if principal is null or empty
    String principal;
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    principal = authentication.getName();

    var eventContext =
        AuditableEventContext.builder()
            .name(auditLoggingFilterDetails.getClassName() + " Endpoint Called")
            .principal(principal)
            .method(request.getMethod())
            .statusCode(response.getStatus())
            .success(auditLoggingFilterDetails.isSuccess())
            .path(request.getServletPath())
            .ipAddress(getXForwardedClientIp(request))
            .xForwardedFor(getXForwardedCompleteHeader(request))
            .clientVersion(getClientVersion(request))
            .version(buildProperties.getVersion())
            .originatingClass(auditLoggingFilterDetails.getClassName())
            .traceId(getTraceId())
            .action(auditLoggingFilterDetails.getAction());

    Optional.ofNullable(sdbAccessRequest.getSdbSlug()).ifPresent(eventContext::sdbNameSlug);

    AuditableEvent event = new AuditableEvent(this, eventContext.build());

    applicationEventPublisher.publishEvent(event);
  }
}
