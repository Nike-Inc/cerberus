/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.audit.logger.listener;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.event.AuditableEvent;
import com.nike.cerberus.event.AuditableEventContext;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Event listener that only cares about auditable events and outputs to a special audit log appender
 * in a flat json format that is optimized for use with AWS Athena
 */
@Slf4j
@ConditionalOnProperty("cerberus.events.athenaLoggingEventListener.enabled")
@Component
public class AthenaLoggingEventListener implements ApplicationListener<AuditableEvent> {

  private static final String ATHENA_AUDIT_LOGGER_NAME = "athena-audit-logger";
  private static final DateTimeFormatter ATHENA_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final String PARTY_LIKE_ITS_99 = "1999-01-01T01:00:00+00:00";
  private final ObjectMapper om = new ObjectMapper();

  protected final Logger auditLogger;

  @Autowired
  public AthenaLoggingEventListener(Logger auditLogger) {
    this.auditLogger = auditLogger;
  }

  @Override
  public void onApplicationEvent(AuditableEvent event) {
    getAuditableEventContext(event)
        .ifPresent(
            eventContext -> {
              Optional<CerberusAuthToken> cerberusPrincipal =
                  eventContext.getPrincipalAsCerberusPrincipal();

              ImmutableMap<String, String> flattenedAuditEvent =
                  ImmutableMap.<String, String>builder()
                      .put(
                          "event_timestamp",
                          eventContext.getTimestamp().format(ATHENA_DATE_FORMATTER))
                      .put("principal_name", eventContext.getPrincipalName())
                      .put(
                          "principal_type",
                          cerberusPrincipal
                              .map(p -> cerberusPrincipal.get().getPrincipalType().getName())
                              .orElse(AuditableEventContext.UNKNOWN))
                      .put(
                          "principal_token_created",
                          cerberusPrincipal
                              .map(
                                  p ->
                                      cerberusPrincipal
                                          .get()
                                          .getCreated()
                                          .format(ATHENA_DATE_FORMATTER))
                              .orElseGet(
                                  () ->
                                      OffsetDateTime.parse(PARTY_LIKE_ITS_99, ISO_OFFSET_DATE_TIME)
                                          .format(ATHENA_DATE_FORMATTER)))
                      .put(
                          "principal_token_expires",
                          cerberusPrincipal
                              .map(
                                  p ->
                                      cerberusPrincipal
                                          .get()
                                          .getExpires()
                                          .format(ATHENA_DATE_FORMATTER))
                              .orElseGet(
                                  () ->
                                      OffsetDateTime.parse(PARTY_LIKE_ITS_99, ISO_OFFSET_DATE_TIME)
                                          .format(ATHENA_DATE_FORMATTER)))
                      .put(
                          "principal_is_admin",
                          cerberusPrincipal
                              .map(p -> String.valueOf(p.isAdmin()))
                              .orElseGet(() -> String.valueOf(false)))
                      .put("ip_address", eventContext.getIpAddress())
                      .put("x_forwarded_for", eventContext.getXForwardedFor())
                      .put("cerberus_version", eventContext.getVersion())
                      .put("client_version", eventContext.getClientVersion())
                      .put("http_method", eventContext.getMethod())
                      .put("status_code", String.valueOf(eventContext.getStatusCode()))
                      .put("path", eventContext.getPath())
                      .put("action", eventContext.getAction())
                      .put("was_success", String.valueOf(eventContext.isSuccess()))
                      .put("name", eventContext.getEventName())
                      .put(
                          "sdb_name_slug",
                          Optional.ofNullable(eventContext.getSdbNameSlug())
                              .orElse(AuditableEventContext.UNKNOWN))
                      .put("originating_class", eventContext.getOriginatingClass())
                      .put("trace_id", eventContext.getTraceId())
                      .build();

              try {
                auditLogger.info(om.writeValueAsString(flattenedAuditEvent));
                log.info(event.toString());
              } catch (JsonProcessingException e) {
                log.error("failed to log audit event", e);
              }
            });
  }

  private Optional<AuditableEventContext> getAuditableEventContext(AuditableEvent event) {
    return event.getAuditableEventContext() != null
        ? Optional.of(event.getAuditableEventContext())
        : Optional.empty();
  }
}
