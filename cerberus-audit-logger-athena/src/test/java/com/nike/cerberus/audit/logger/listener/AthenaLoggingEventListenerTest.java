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

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testIfAuditableContextIsMissingInAuditEvent() {
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    Mockito.verify(auditLogger, Mockito.never()).info(Mockito.anyString());
  }

  @Test
  public void testIfAuditableContextIsPresentInAuditEvent() {
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getTraceId()).thenReturn("traceId");
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
    Mockito.when(auditableEventContext.getXForwardedFor()).thenReturn("xforwarder");
    Mockito.when(auditableEventContext.getVersion()).thenReturn("version");
    Mockito.when(auditableEventContext.getClientVersion()).thenReturn("clientVersion");
    Mockito.when(auditableEventContext.getMethod()).thenReturn("post");
    Mockito.when(auditableEventContext.getStatusCode()).thenReturn(200);
    Mockito.when(auditableEventContext.getPath()).thenReturn("/path");
    Mockito.when(auditableEventContext.getAction()).thenReturn("action");
    Mockito.when(auditableEventContext.isSuccess()).thenReturn(true);
    Mockito.when(auditableEventContext.getEventName()).thenReturn("eventName");
    Mockito.when(auditableEventContext.getOriginatingClass()).thenReturn("originatingClass");
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    Mockito.verify(auditLogger).info(Mockito.anyString());
  }

  @Test(expected = NullPointerException.class)
  public void testIfEventTimeStampIsMissingInContext() {
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
    athenaLoggingEventListener.onApplicationEvent(auditableEvent);
  }

  @Test
  public void testIfPrincipleNameIsMissingInContext() {
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
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
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
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
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
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
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
    Mockito.when(auditableEventContext.getXForwardedFor()).thenReturn("xforwarder");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
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
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
    Mockito.when(auditableEventContext.getXForwardedFor()).thenReturn("xforwarder");
    Mockito.when(auditableEventContext.getVersion()).thenReturn("version");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
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
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
    Mockito.when(auditableEventContext.getXForwardedFor()).thenReturn("xforwarder");
    Mockito.when(auditableEventContext.getVersion()).thenReturn("version");
    Mockito.when(auditableEventContext.getClientVersion()).thenReturn("clientVersion");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
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
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
    Mockito.when(auditableEventContext.getXForwardedFor()).thenReturn("xforwarder");
    Mockito.when(auditableEventContext.getVersion()).thenReturn("version");
    Mockito.when(auditableEventContext.getClientVersion()).thenReturn("clientVersion");
    Mockito.when(auditableEventContext.getMethod()).thenReturn("post");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
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
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
    Mockito.when(auditableEventContext.getXForwardedFor()).thenReturn("xforwarder");
    Mockito.when(auditableEventContext.getVersion()).thenReturn("version");
    Mockito.when(auditableEventContext.getClientVersion()).thenReturn("clientVersion");
    Mockito.when(auditableEventContext.getMethod()).thenReturn("post");
    Mockito.when(auditableEventContext.getPath()).thenReturn("/path");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
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
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
    Mockito.when(auditableEventContext.getXForwardedFor()).thenReturn("xforwarder");
    Mockito.when(auditableEventContext.getVersion()).thenReturn("version");
    Mockito.when(auditableEventContext.getClientVersion()).thenReturn("clientVersion");
    Mockito.when(auditableEventContext.getMethod()).thenReturn("post");
    Mockito.when(auditableEventContext.getPath()).thenReturn("/path");
    Mockito.when(auditableEventContext.getAction()).thenReturn("action");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
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
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
    Mockito.when(auditableEventContext.getXForwardedFor()).thenReturn("xforwarder");
    Mockito.when(auditableEventContext.getVersion()).thenReturn("version");
    Mockito.when(auditableEventContext.getClientVersion()).thenReturn("clientVersion");
    Mockito.when(auditableEventContext.getMethod()).thenReturn("post");
    Mockito.when(auditableEventContext.getPath()).thenReturn("/path");
    Mockito.when(auditableEventContext.getAction()).thenReturn("action");
    Mockito.when(auditableEventContext.getEventName()).thenReturn("eventName");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
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
    AuditableEvent auditableEvent = Mockito.mock(AuditableEvent.class);
    AuditableEventContext auditableEventContext = Mockito.mock(AuditableEventContext.class);
    Mockito.when(auditableEventContext.getPrincipalAsCerberusPrincipal())
        .thenReturn(Optional.empty());
    Mockito.when(auditableEventContext.getTimestamp())
        .thenReturn(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"));
    Mockito.when(auditableEventContext.getPrincipalName()).thenReturn("pricinpleName");
    Mockito.when(auditableEventContext.getIpAddress()).thenReturn("ipAddress");
    Mockito.when(auditableEventContext.getXForwardedFor()).thenReturn("xforwarder");
    Mockito.when(auditableEventContext.getVersion()).thenReturn("version");
    Mockito.when(auditableEventContext.getClientVersion()).thenReturn("clientVersion");
    Mockito.when(auditableEventContext.getMethod()).thenReturn("post");
    Mockito.when(auditableEventContext.getPath()).thenReturn("/path");
    Mockito.when(auditableEventContext.getAction()).thenReturn("action");
    Mockito.when(auditableEventContext.getEventName()).thenReturn("eventName");
    Mockito.when(auditableEventContext.getOriginatingClass()).thenReturn("originatingClass");
    Mockito.when(auditableEvent.getAuditableEventContext()).thenReturn(auditableEventContext);
    String exceptionMessage = "";
    try {
      athenaLoggingEventListener.onApplicationEvent(auditableEvent);
    } catch (NullPointerException nullPointerException) {
      exceptionMessage = nullPointerException.getMessage();
    }
    Assert.assertTrue(exceptionMessage.contains("trace_id"));
  }
}
