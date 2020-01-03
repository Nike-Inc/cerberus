package com.nike.cerberus.event;

import static com.nike.cerberus.CerberusHttpHeaders.*;

import com.google.common.collect.ImmutableMap;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.util.SdbAccessRequest;
import com.nike.wingtips.Span;
import com.nike.wingtips.Tracer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

  private final SdbAccessRequest sdbAccessRequest;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;
  private final BuildProperties buildProperties;
  private final ApplicationEventPublisher applicationEventPublisher;

  private static final List<String> LOGGING_NOT_TRIGGERED_BLACKLIST = List.of("/dashboard/**");

  private static final Map<String, String> READABLE_METHOD_ACTIONS =
      ImmutableMap.of(
          "GET", "read",
          "PUT", "wrote",
          "POST", "wrote",
          "DELETE", "deleted");

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

  private boolean isResponseSuccessful(int statusCode) {
    HttpStatus status = HttpStatus.valueOf(statusCode);
    return status.is2xxSuccessful();
  }

  private String getAction(Object principal, String method, String path) {
    if (auditLoggingFilterDetails.getAction() != null
        && !auditLoggingFilterDetails.getAction().isEmpty()) {
      return auditLoggingFilterDetails.getAction();
    }
    String readableAction = READABLE_METHOD_ACTIONS.getOrDefault(method, method);
    String principalName =
        principal instanceof CerberusAuthToken
            ? ((CerberusAuthToken) principal).getPrincipal()
            : principal instanceof String
                ? (String) principal
                : principal != null ? principal.toString() : "Unknown";
    return principalName + " " + readableAction + " " + path;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    List<RequestMatcher> blackListMatchers =
        LOGGING_NOT_TRIGGERED_BLACKLIST.stream()
            .map(AntPathRequestMatcher::new)
            .collect(Collectors.toList());
    var blackListMatcher = new OrRequestMatcher(blackListMatchers);
    return blackListMatcher.matches(request);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    filterChain.doFilter(request, response);

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = Optional.ofNullable(authentication.getPrincipal()).orElse("Unknown");

    var eventContext =
        AuditableEventContext.builder()
            .eventName("Audit Logging Filter Event")
            .principal(principal)
            .action(getAction(principal, request.getMethod(), request.getServletPath()))
            .method(request.getMethod())
            .statusCode(response.getStatus())
            .success(isResponseSuccessful(response.getStatus()))
            .path(request.getServletPath())
            .ipAddress(getXForwardedClientIp(request))
            .xForwardedFor(getXForwardedCompleteHeader(request))
            .clientVersion(getClientVersion(request))
            .version(buildProperties.getVersion())
            .originatingClass(this.getClass().getSimpleName())
            .traceId(getTraceId());

    Optional.ofNullable(sdbAccessRequest.getSdbSlug()).ifPresent(eventContext::sdbNameSlug);

    AuditableEvent event = new AuditableEvent(this, eventContext.build());

    applicationEventPublisher.publishEvent(event);
  }
}
