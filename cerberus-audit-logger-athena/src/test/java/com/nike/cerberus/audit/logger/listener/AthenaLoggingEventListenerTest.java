package com.nike.cerberus.audit.logger.listener;

import com.nike.cerberus.event.AuditableEvent;
import com.nike.cerberus.event.AuditableEventContext;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

public class AthenaLoggingEventListenerTest {

  @Mock private Logger auditLogger;

  @InjectMocks private AthenaLoggingEventListener athenaLoggingEventListener;

  private AuditableEvent auditableEvent;

  private AuditableEventContext auditableEventContext;

  @Before
  public void setup() {
    auditableEventContext = Mockito.mock(AuditableEventContext.class);
    auditableEvent = Mockito.mock(AuditableEvent.class);
    MockitoAnnotations.initMocks(this);
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
  }

  private void mockAuditableEvent() {
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
  }

  private void mockAuditableEventContextPrincipalName() {
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
  }

  private void mockAuditableEventContextIpAddress() {
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
  }

  private void mockAuditableEventContextXForwarded() {
    Mockito.when(auditableEventContext.getXForwardedFor()).thenReturn("xforwarder");
  }

  private void mockAuditableEventContextClientVersion() {
    Mockito.when(auditableEventContext.getClientVersion()).thenReturn("clientVersion");
  }

  private void mockAuditableEventContextCerberusVerion() {
    Mockito.when(auditableEventContext.getVersion()).thenReturn("version");
  }

  private void mockAuditableEventContextPost() {
    Mockito.when(auditableEventContext.getMethod()).thenReturn("post");
  }

  private void mockAuditableEventContextPath() {
    Mockito.when(auditableEventContext.getPath()).thenReturn("path");
  }

  @Test
  public void testIfAuditableContextIsMissingInAuditEvent() {
    athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    Mockito.verify(auditLogger, Mockito.never()).info(Mockito.anyString());
  }

  @Test
  public void testIfAuditableContextIsPresentInAuditEvent() {
    mockAuditableEventContextPrincipalName();
    mockAuditableEvent();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();

    Mockito.when(auditableEventContext.getTraceId()).thenReturn("traceId");
    Mockito.when(auditableEventContext.getStatusCode()).thenReturn(200);
    Mockito.when(auditableEventContext.getAction()).thenReturn("action");
    Mockito.when(auditableEventContext.isSuccess()).thenReturn(true);
    Mockito.when(auditableEventContext.getEventName()).thenReturn("eventName");
    Mockito.when(auditableEventContext.getOriginatingClass()).thenReturn("originatingClass");

    athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    Mockito.verify(auditLogger).info(Mockito.anyString());
  }

  @Test(expected = NullPointerException.class)
  public void testIfEventTimeStampIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();
    athenaLoggingEventListener.onApplicationEvent(auditableEvent);
  }

  @Test
  public void testIfPrincipleNameIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();

    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("principal_name"));
  }

  @Test
  public void testIfIpAddressIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();

    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("ip_address"));
  }

  @Test
  public void testIfXForwardedForIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();

    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("x_forwarded_for"));
  }

  @Test
  public void testIfCerberusVersionIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();

    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("cerberus_version"));
  }

  @Test
  public void testIfClientVersionIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();

    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("client_version"));
  }

  @Test
  public void testIfHttpMethodIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPath();

    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("http_method"));
  }

  @Test
  public void testIfPathIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("path"));
  }

  @Test
  public void testIfActionIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();

    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("action"));
  }

  @Test
  public void testIfEventNameIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();

    Mockito.when(auditableEventContext.getAction()).thenReturn("action");
    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("name"));
  }

  @Test
  public void testIfOriginatingClassIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();

    Mockito.when(auditableEventContext.getAction()).thenReturn("action");
    Mockito.when(auditableEventContext.getEventName()).thenReturn("eventName");
    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("originating_class"));
  }

  @Test
  public void testIfTraceIdIsMissingInContext() {
    mockAuditableEvent();
    mockAuditableEventContextPrincipalName();
    mockAuditableEventContextIpAddress();
    mockAuditableEventContextXForwarded();
    mockAuditableEventContextClientVersion();
    mockAuditableEventContextCerberusVerion();
    mockAuditableEventContextPost();
    mockAuditableEventContextPath();

    Mockito.when(auditableEventContext.getAction()).thenReturn("action");
    Mockito.when(auditableEventContext.getEventName()).thenReturn("eventName");
    Mockito.when(auditableEventContext.getOriginatingClass()).thenReturn("originatingClass");
    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("trace_id"));
  }
}
