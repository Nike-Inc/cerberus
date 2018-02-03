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

package com.nike.cerberus.endpoints;

import com.nike.cerberus.event.AuditableEvent;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.service.EventProcessorService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.wingtips.Span;
import com.nike.wingtips.TraceAndSpanIdGenerator;
import com.nike.wingtips.Tracer;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.CerberusHttpHeaders.*;

/**
 * Endpoint class that can be extended to automatically enable audit event processing
 */
public abstract class AuditableEventEndpoint<I, O> extends StandardEndpoint<I, O> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected EventProcessorService eventProcessorService;

    @Inject
    public void setEventProcessorService(EventProcessorService eventProcessorService) {
        this.eventProcessorService = eventProcessorService;
    }

    @Override
    public CompletableFuture<ResponseInfo<O>> execute(RequestInfo<I> request, Executor longRunningTaskExecutor, ChannelHandlerContext ctx) {
        CompletableFuture<ResponseInfo<O>> cf = doExecute(request, longRunningTaskExecutor, ctx);
        if (eventProcessorService != null) {
            eventProcessorService.ingestEvent(generateAuditableEvent(getPrincipal(request), request));
        }
        return cf;
    }

    private Object getPrincipal(RequestInfo<I> request) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);
        if (securityContext.isPresent()) {
            return securityContext.get().getUserPrincipal();
        }
        return  AuditableEvent.UNKNOWN;
    }

    abstract public CompletableFuture<ResponseInfo<O>> doExecute(RequestInfo<I> request, Executor longRunningTaskExecutor, ChannelHandlerContext ctx);

    /**
     * Creates an Auditable Event that can be sent to the event processing service that describes
     * what a given principle is doing with the API.
     *
     * @param principal The principal that is performing an API action
     * @param request The request info object for the given request
     * @return an Auditable Event that can be sent to the event processing service that describes
     * what a given principle is doing with the API.
     */
    public AuditableEvent generateAuditableEvent(Object principal, RequestInfo<I> request) {
        CustomizableAuditData customizableAuditData = getCustomizableAuditData(request);
        return auditableEvent(principal, request)
                .withAction(customizableAuditData.getDescription())
                .withSdbNameSlug(customizableAuditData.getSdbNameSlug())
                .build();
    }

    /**
     * Default action to be logged can be overridden in the endpoints to provide more detail.
     *
     * @param request The request info object
     * @return A description of the auditable event
     */
    protected String describeActionForAuditEvent(RequestInfo<I> request) {
        return String.format("Invoked %s request to uri %s triggering the %s endpoint class.",
                request.getMethod(),
                request.getPath(),
                getClass().getSimpleName());
    }

    /**
     * Method that can be implemented to add the SDB Slug Name to audit data
     */
    protected String getSlugifiedSdbName(RequestInfo<I> request) {
        return AuditableEvent.UNKNOWN;
    }

    /**
     * Method that can be overridden to add custom audit data to events
     */
    protected CustomizableAuditData getCustomizableAuditData(RequestInfo<I> request) {
        return new CustomizableAuditData()
                .setDescription(describeActionForAuditEvent(request))
                .setSdbNameSlug(getSlugifiedSdbName(request));
    }

    /**
     * Builds the base Auditable event that should be common amongst all AuditableEventEndpoints
     *
     * @param principal The principal that is performing an API action
     * @param request The request info object for the given request
     * @return an AuditableEvent Builder with base attributes populated
     */
    protected AuditableEvent.Builder auditableEvent(Object principal, RequestInfo<I> request) {
        return auditableEvent(principal, request, getClass().getSimpleName());
    }

    /**
     * Static method that can build the base Auditable event, used internally here for endpoints that extend
     * AuditableEventEndpoint, but also useful for classes that don't go through the security validator and cannot
     * extend this class such as the auth endpoints.
     *
     * @param principal The principal that is performing an API action
     * @param request The request info object for the given request
     * @param className The classname for the endpoint creating the event
     * @return an AuditableEvent Builder with base attributes populated
     */
    public static AuditableEvent.Builder auditableEvent(Object principal,
                                                        RequestInfo request,
                                                        String className) {

        return AuditableEvent.Builder.create()
                .withName(className + " Endpoint Called")
                .withPrincipal(principal)
                .withMethod(request.getMethod())
                .withPath(request.getPath())
                .withIpAddress(getXForwardedClientIp(request))
                .withXForwardedFor(getXForwardedCompleteHeader(request))
                .withClientVersion(getClientVersion(request))
                .withOriginatingClass(className);
    }
}
