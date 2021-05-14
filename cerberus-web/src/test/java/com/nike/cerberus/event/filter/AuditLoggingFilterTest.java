package com.nike.cerberus.event.filter;

import static com.nike.cerberus.CerberusHttpHeaders.UNKNOWN;

import com.nike.cerberus.event.AuditableEvent;
import com.nike.cerberus.event.AuditableEventContext;
import com.nike.cerberus.util.SdbAccessRequest;
import java.io.IOException;
import java.time.OffsetDateTime;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditLoggingFilterTest {
  @Mock private SdbAccessRequest sdbAccessRequest;
  @Mock private AuditLoggingFilterDetails auditLoggingFilterDetails;
  @Mock private BuildProperties buildProperties;
  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @InjectMocks private AuditLoggingFilter auditLoggingFilter;

  @Captor private ArgumentCaptor<AuditableEvent> auditableEventArgumentCaptor;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testShouldNotFilterReturnFalseIfServletPathIsNotMatched() {
    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpServletRequest.getServletPath()).thenReturn("/sample/*");
    boolean isFiltered = auditLoggingFilter.shouldNotFilter(httpServletRequest);
    Assert.assertFalse(isFiltered);
  }

  @Test
  public void testShouldNotFilterReturnTrueIfServletPathIsMatched() {
    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpServletRequest.getServletPath()).thenReturn("/dashboard/resource");
    boolean isFiltered = auditLoggingFilter.shouldNotFilter(httpServletRequest);
    Assert.assertTrue(isFiltered);
  }

  @Test
  public void testDoFilterInternal() throws ServletException, IOException {
    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChain = Mockito.mock(FilterChain.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    Mockito.when(httpServletRequest.getMethod()).thenReturn("GET");
    Mockito.when(httpServletRequest.getServletPath()).thenReturn("/servletPath");
    Mockito.when(httpServletResponse.getStatus()).thenReturn(200);
    Mockito.when(auditLoggingFilterDetails.getAction()).thenReturn("action");
    Mockito.when(auditLoggingFilterDetails.getSdbNameSlug()).thenReturn("sdbNameSlug");
    Mockito.when(buildProperties.getVersion()).thenReturn("version");
    auditLoggingFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);
    Mockito.verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    Mockito.verify(applicationEventPublisher).publishEvent(auditableEventArgumentCaptor.capture());
    AuditableEvent auditableEvent = auditableEventArgumentCaptor.getValue();
    AuditableEventContext expectedAuditableEventContext =
        getExpectedAuditableEventContext(OffsetDateTime.MAX, "action", "sdbNameSlug");
    AuditableEventContext actualAuditableEventContext = auditableEvent.getAuditableEventContext();
    actualAuditableEventContext.setTimestamp(OffsetDateTime.MAX);
    Assert.assertEquals(
        expectedAuditableEventContext.toString(), actualAuditableEventContext.toString());
  }

  @Test
  public void testDoFilterInternalActionIsEmptyInAuditLoggingFilterDetails()
      throws ServletException, IOException {
    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChain = Mockito.mock(FilterChain.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    Mockito.when(httpServletRequest.getMethod()).thenReturn("GET");
    Mockito.when(httpServletRequest.getServletPath()).thenReturn("/servletPath");
    Mockito.when(httpServletResponse.getStatus()).thenReturn(200);
    Mockito.when(sdbAccessRequest.getSdbSlug()).thenReturn("sdbNameSlug");
    Mockito.when(buildProperties.getVersion()).thenReturn("version");
    auditLoggingFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);
    Mockito.verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    Mockito.verify(applicationEventPublisher).publishEvent(auditableEventArgumentCaptor.capture());
    AuditableEvent auditableEvent = auditableEventArgumentCaptor.getValue();
    AuditableEventContext expectedAuditableEventContext =
        getExpectedAuditableEventContext(
            OffsetDateTime.MAX, "Unknown read /servletPath", "sdbNameSlug");
    AuditableEventContext actualAuditableEventContext = auditableEvent.getAuditableEventContext();
    actualAuditableEventContext.setTimestamp(OffsetDateTime.MAX);
    Assert.assertEquals(
        expectedAuditableEventContext.toString(), actualAuditableEventContext.toString());
  }

  private AuditableEventContext getExpectedAuditableEventContext(
      OffsetDateTime offsetDateTime, String action, String sdbNameSlug) {
    AuditableEventContext auditableEventContext =
        AuditableEventContext.builder()
            .eventName("Audit Logging Filter Event")
            .principal("Unknown")
            .action(action)
            .method("GET")
            .statusCode(200)
            .success(true)
            .path("/servletPath")
            .ipAddress(UNKNOWN)
            .xForwardedFor(UNKNOWN)
            .clientVersion(UNKNOWN)
            .version("version")
            .originatingClass(AuditLoggingFilter.class.getSimpleName())
            .traceId(AuditableEventContext.UNKNOWN)
            .sdbNameSlug(sdbNameSlug)
            .timestamp(offsetDateTime)
            .build();
    return auditableEventContext;
  }
}
