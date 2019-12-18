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

package com.nike.cerberus.event.processor;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.nike.cerberus.event.AuditableEvent;
import com.nike.cerberus.event.AuditableEventContext;
import com.nike.cerberus.security.CerberusPrincipal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;

/**
 * Event listener that only cares about auditable events and outputs to a special audit log appender
 * in a flat json format that is optimized for use with AWS Athena
 */
@ConditionalOnProperty("cerberus.events.auditLogProcessor.enabled")
public class AthenaLoggingEventListener implements ApplicationListener<AuditableEvent> {

  protected final Logger auditLogger = LoggerFactory.getLogger(this.getClass());

  private static final DateTimeFormatter ATHENA_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final String PARTY_LIKE_ITS_99 = "1999-01-01T01:00:00+00:00";

  private final ObjectMapper om = new ObjectMapper();

  public String getName() {
    return "athena-logging-event-listener";
  }

  @Override
  public void onApplicationEvent(AuditableEvent event) {
    getAuditableEventContext(event)
        .ifPresent(
            eventContext -> {
              Optional<CerberusPrincipal> cerberusPrincipal =
                  eventContext.getPrincipalAsCerberusPrincipal();

              ImmutableMap<String, String> flattenedAuditEvent =
                  ImmutableMap.<String, String>builder()
                      .put(
                          "event_timestamp",
                          eventContext.getTimestamp().format(ATHENA_DATE_FORMATTER))
                      .put("principal_name", eventContext.getPrincipal().toString())
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
                                          .getTokenCreated()
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
                                          .getTokenExpires()
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
                      .put("client_version", eventContext.getClientVersion())
                      .put("http_method", eventContext.getMethod())
                      .put("path", eventContext.getPath())
                      .put("action", eventContext.getAction())
                      .put("was_success", String.valueOf(eventContext.isSuccess()))
                      .put("name", eventContext.getEventName())
                      .put("sdb_name_slug", eventContext.getSdbNameSlug())
                      .put("originating_class", eventContext.getOriginatingClass())
                      .put("trace_id", eventContext.getTraceId())
                      .build();

              try {
                auditLogger.info(om.writeValueAsString(flattenedAuditEvent));
              } catch (JsonProcessingException e) {
                LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
                    .error("failed to log audit event", e);
              }
            });
  }

  private Optional<AuditableEventContext> getAuditableEventContext(AuditableEvent event) {
    return event.getAuditableEventContext() != null
        ? Optional.of(event.getAuditableEventContext())
        : Optional.empty();
  }
}
