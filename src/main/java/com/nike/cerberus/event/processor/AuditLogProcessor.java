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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.nike.cerberus.event.AuditableEvent;
import com.nike.cerberus.event.Event;
import com.nike.cerberus.security.CerberusPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * Event Processor that only cares about auditable events and outputs to a special audit log appender in a flat json
 * format that is optimized for use with AWS Athena
 */
public class AuditLogProcessor implements EventProcessor {

    protected final Logger auditLogger = LoggerFactory.getLogger(this.getClass());

    private static final DateTimeFormatter ATHENA_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PARTY_LIKE_ITS_99 = "1999-01-01T01:00:00+00:00";

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void process(Event originalEvent) {
        getAuditableEvent(originalEvent).ifPresent(event -> {

            Optional<CerberusPrincipal> cerberusPrincipal = event.getPrincipalAsCerberusPrincipal();

            ImmutableMap<String, String> flattenedAuditEvent = ImmutableMap.<String, String>builder()
                    .put("event_timestamp", event.getTimestamp().format(ATHENA_DATE_FORMATTER))
                    .put("principal_name", event.getPrincipalName())
                    .put("principal_type", cerberusPrincipal
                            .map(p -> cerberusPrincipal.get().getPrincipalType().getName()).orElse(AuditableEvent.UNKNOWN))
                    .put("principal_token_created", cerberusPrincipal
                            .map(p -> cerberusPrincipal.get().getTokenCreated().format(ATHENA_DATE_FORMATTER))
                            .orElseGet(() -> OffsetDateTime.parse(PARTY_LIKE_ITS_99, ISO_OFFSET_DATE_TIME).format(ATHENA_DATE_FORMATTER)))
                    .put("principal_token_expires", cerberusPrincipal
                            .map(p -> cerberusPrincipal.get().getTokenExpires().format(ATHENA_DATE_FORMATTER))
                            .orElseGet(() -> OffsetDateTime.parse(PARTY_LIKE_ITS_99, ISO_OFFSET_DATE_TIME).format(ATHENA_DATE_FORMATTER)))
                    .put("principal_is_admin", cerberusPrincipal
                            .map(p -> String.valueOf(p.isAdmin())).orElseGet(() -> String.valueOf(false)))
                    .put("ip_address", event.getIpAddress())
                    .put("x_forwarded_for", event.getxForwardedFor())
                    .put("client_version", event.getClientVersion())
                    .put("http_method", event.getMethodAsString())
                    .put("path", event.getPath())
                    .put("action", event.getAction())
                    .put("was_success", String.valueOf(event.isSuccess()))
                    .put("name", event.getName())
                    .put("sdb_name_slug", event.getSdbNameSlug())
                    .put("originating_class", event.getOriginatingClass())
                    .put("trace_id", event.getTraceId())
                    .build();

            try {
                auditLogger.info(om.writeValueAsString(flattenedAuditEvent));
            } catch (JsonProcessingException e) {
                LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).error("failed to log audit event", e);
            }
        });
    }

    private Optional<AuditableEvent> getAuditableEvent(Event event) {
        return event instanceof AuditableEvent ? Optional.of((AuditableEvent) event) : Optional.empty();
    }

    @Override
    public String getName() {
        return "audit-log-processor";
    }

}
