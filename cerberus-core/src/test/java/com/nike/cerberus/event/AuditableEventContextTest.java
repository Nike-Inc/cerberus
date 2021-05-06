package com.nike.cerberus.event;

import com.nike.cerberus.domain.CerberusAuthToken;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AuditableEventContextTest {

  private AuditableEventContext auditableEventContext;

  @Before
  public void setup() {
    auditableEventContext = new AuditableEventContext.AuditableEventContextBuilder().build();
  }

  @Test
  public void testCheckAuthTokenIsEmptyIfPrincipleIsNotInstanceOfCerberusAuthToken() {
    auditableEventContext.setPrincipal(new Object());
    Optional<CerberusAuthToken> principalAsCerberusPrincipal =
        auditableEventContext.getPrincipalAsCerberusPrincipal();
    Assert.assertFalse(principalAsCerberusPrincipal.isPresent());
  }

  @Test
  public void testCheckAuthTokenIsEmptyIfPrincipleIsInstanceOfCerberusAuthToken() {
    CerberusAuthToken cerberusAuthToken = CerberusAuthToken.Builder.create().build();
    auditableEventContext.setPrincipal(cerberusAuthToken);
    Optional<CerberusAuthToken> principalAsCerberusPrincipal =
        auditableEventContext.getPrincipalAsCerberusPrincipal();
    Assert.assertTrue(principalAsCerberusPrincipal.isPresent());
    Assert.assertSame(cerberusAuthToken, principalAsCerberusPrincipal.get());
  }

  @Test
  public void testGetPrincipalNameIfPrincipleIsInstanceOfCerberusAuthToken() {
    String cerberusPrinciple = "cerberusPrinciple";
    CerberusAuthToken cerberusAuthToken =
        CerberusAuthToken.Builder.create().withPrincipal(cerberusPrinciple).build();
    auditableEventContext.setPrincipal(cerberusAuthToken);
    String principalName = auditableEventContext.getPrincipalName();
    Assert.assertEquals(cerberusPrinciple, principalName);
  }

  @Test
  public void testGetPrincipalNameIfPrincipleIsInstanceOfString() {
    String stringPrinciple = "stringPrinciple";
    auditableEventContext.setPrincipal(stringPrinciple);
    String principalName = auditableEventContext.getPrincipalName();
    Assert.assertEquals(stringPrinciple, principalName);
  }

  @Test
  public void testGetPrincipalNameIfPrincipleIsNull() {
    String principalName = auditableEventContext.getPrincipalName();
    Assert.assertEquals("Unknown", principalName);
  }

  @Test
  public void testGetPrincipalNameIfPrincipleIsInstanceOfObject() {
    Object principle = new Object();
    auditableEventContext.setPrincipal(principle);
    String principalName = auditableEventContext.getPrincipalName();
    Assert.assertEquals(principle.toString(), principalName);
  }

  @Test
  public void testGetEventsAsString() {
    auditableEventContext =
        new AuditableEventContext.AuditableEventContextBuilder()
            .eventName("eventName")
            .principal("principal")
            .action("action")
            .method("method")
            .statusCode(0)
            .success(true)
            .path("path")
            .ipAddress("ipaddress")
            .xForwardedFor("xforwarder")
            .clientVersion("1")
            .version("0")
            .originatingClass("originating")
            .sdbNameSlug("sdbname")
            .traceId("traceId")
            .timestamp(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"))
            .build();
    String eventAsString = auditableEventContext.getEventAsString();
    String events =
        "eventName, Principal: principal, Action: 'action', Method: method, Status Code: 0, Was Success: true, Path: path, IP Address: ipaddress, X-Forwarded-For: xforwarder, Client Version: 1, Cerberus Version: 0, Originating Class: originating, SDB Name Slug: sdbname, Trace ID: traceId, Event Timestamp: Dec 3 2007, 10:15:30 AM +0100";
    Assert.assertEquals(events, eventAsString);
  }
}
