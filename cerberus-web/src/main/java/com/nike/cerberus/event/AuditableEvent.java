/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.event;

import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.wingtips.Span;
import com.nike.wingtips.Tracer;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** An event that can be used to describe what a principal is doing with the API */
public class AuditableEvent implements Event {

  public static final String UNKNOWN = "_unknown";

  private Object principal;
  private String traceId;
  private String ipAddress;
  private String xForwardedFor;
  private String clientVersion;
  private String method;
  private String path;
  private String action;
  private String name;
  private String originatingClass;
  private OffsetDateTime timestamp;
  private String sdbNameSlug;
  private boolean success = true;

  public Optional<CerberusPrincipal> getPrincipalAsCerberusPrincipal() {
    return principal instanceof CerberusPrincipal
        ? Optional.of((CerberusPrincipal) principal)
        : Optional.empty();
  }

  public String getPrincipalName() {
    return principal instanceof CerberusPrincipal
        ? ((CerberusPrincipal) principal).getName()
        : principal instanceof String
            ? (String) principal
            : principal != null ? principal.toString() : "Unknown";
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getxForwardedFor() {
    return xForwardedFor;
  }

  public String getClientVersion() {
    return clientVersion;
  }

  public String getMethodAsString() {
    return method == null ? UNKNOWN : method.toString();
  }

  public String getPath() {
    return path;
  }

  public String getAction() {
    return action;
  }

  public String getName() {
    return name;
  }

  public String getOriginatingClass() {
    return originatingClass;
  }

  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public String getSdbNameSlug() {
    return sdbNameSlug;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getTraceId() {
    return traceId;
  }

  @Override
  public String getEventAsString() {
    return "Event: "
        + name
        + ", "
        + "Principal: "
        + getPrincipalName()
        + ", "
        + "IP Address: "
        + ipAddress
        + ", "
        + "X-Forwarded-For: "
        + xForwardedFor
        + ", "
        + "Client Version: "
        + clientVersion
        + ", "
        + "Method: "
        + method
        + ", "
        + "Path: "
        + path
        + ", "
        + "Action: "
        + '\''
        + action
        + "\', "
        + "Originating Class: "
        + originatingClass
        + ", "
        + "SDB Name Slug: "
        + sdbNameSlug
        + ", "
        + "Was Success: "
        + success
        + ", "
        + "Trace ID: "
        + traceId
        + ", "
        + "Event Timestamp: "
        + timestamp.format(DateTimeFormatter.ofPattern("MMM d yyyy, hh:mm:ss a Z"));
  }

  public static final class Builder {
    private Object principal;
    private String traceId;
    private String ipAddress;
    private String xForwardedFor;
    private String clientVersion;
    private String method;
    private String path;
    private String action;
    private String name;
    private String originatingClass;
    private String sdbNameSlug = UNKNOWN;
    private boolean success = true;

    private Builder() {
      Span span = Tracer.getInstance().getCurrentSpan();
      traceId = span == null ? AuditableEvent.UNKNOWN : span.getTraceId();
    }

    public static Builder create() {
      return new Builder();
    }

    public Builder withPrincipal(Object principal) {
      this.principal = principal;
      return this;
    }

    public Builder withIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
      return this;
    }

    public Builder withXForwardedFor(String xForwardedFor) {
      this.xForwardedFor = xForwardedFor;
      return this;
    }

    public Builder withClientVersion(String clientVersion) {
      this.clientVersion = clientVersion;
      return this;
    }

    public Builder withMethod(String method) {
      this.method = method;
      return this;
    }

    public Builder withPath(String path) {
      this.path = path;
      return this;
    }

    public Builder withAction(String action) {
      this.action = action;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withOriginatingClass(String originatingClass) {
      this.originatingClass = originatingClass;
      return this;
    }

    public Builder withSdbNameSlug(String sdbNameSlug) {
      this.sdbNameSlug = sdbNameSlug;
      return this;
    }

    public Builder withSuccess(boolean success) {
      this.success = success;
      return this;
    }

    public Builder withTraceId(String traceId) {
      this.traceId = traceId;
      return this;
    }

    public AuditableEvent build() {
      AuditableEvent auditableEvent = new AuditableEvent();
      auditableEvent.action = this.action;
      auditableEvent.name = this.name;
      auditableEvent.clientVersion = this.clientVersion;
      auditableEvent.ipAddress = this.ipAddress;
      auditableEvent.method = this.method;
      auditableEvent.principal = this.principal;
      auditableEvent.originatingClass = this.originatingClass;
      auditableEvent.path = this.path;
      auditableEvent.xForwardedFor = this.xForwardedFor;
      auditableEvent.timestamp = OffsetDateTime.now(ZoneId.of("UTC"));
      auditableEvent.sdbNameSlug = sdbNameSlug;
      auditableEvent.success = success;
      auditableEvent.traceId = traceId;
      return auditableEvent;
    }
  }
}
