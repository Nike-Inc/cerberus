package com.nike.cerberus.event;

import static com.nike.cerberus.CerberusHttpHeaders.*;

import com.nike.cerberus.service.EventProcessorService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

  private final EventProcessorService eventProcessorService;
  private final SdbAccessRequest sdbAccessRequest;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;
  private final BuildProperties buildProperties;

  @Autowired
  public AuditLoggingFilter(
      EventProcessorService eventProcessorService,
      SdbAccessRequest sdbAccessRequest,
      AuditLoggingFilterDetails auditLoggingFilterDetails,
      BuildProperties buildProperties) {

    this.eventProcessorService = eventProcessorService;
    this.sdbAccessRequest = sdbAccessRequest;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
    this.buildProperties = buildProperties;
  }

  private String getTraceId() {
    Span span = Tracer.getInstance().getCurrentSpan();
    return span == null ? AuditableEvent.UNKNOWN : span.getTraceId();
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

    //    if (authentication instanceof CerberusPrincipal) {
    //      principal = authentication.getName();
    //    }
    //    else {
    //      principal = authentication.getName();
    ////              auditLoggingFilterDetails.getClassName();
    //    }

    var event =
        AuditableEvent.builder()
            .metadata(auditLoggingFilterDetails.getMetadata())
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

    Optional.ofNullable(sdbAccessRequest.getSdbSlug()).ifPresent(event::sdbNameSlug);

    eventProcessorService.ingestEvent(event.build());
  }
}
