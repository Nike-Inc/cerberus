/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.server.config.guice;

import com.google.common.collect.Lists;
import com.nike.cerberus.hystrix.HystrixRequestAndResponseFilter;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.server.config.CmsConfig;
import com.nike.riposte.metrics.codahale.CodahaleMetricsListener;
import com.nike.riposte.server.config.AppInfo;
import com.nike.riposte.server.config.impl.DependencyInjectionProvidedServerConfigValuesBase;
import com.nike.riposte.server.error.handler.RiposteErrorHandler;
import com.nike.riposte.server.error.handler.RiposteUnhandledErrorHandler;
import com.nike.riposte.server.error.validation.RequestValidator;
import com.nike.riposte.server.http.Endpoint;
import com.nike.riposte.server.http.filter.RequestAndResponseFilter;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * An extension of {@link DependencyInjectionProvidedServerConfigValuesBase} that includes all the extra dependency-injected properties needed
 * by {@link CmsConfig}.
 *
 * @author Nic Munroe
 */
public class GuiceProvidedServerConfigValues extends DependencyInjectionProvidedServerConfigValuesBase {

    public final RiposteErrorHandler riposteErrorHandler;
    public final RiposteUnhandledErrorHandler riposteUnhandledErrorHandler;
    public final RequestValidator validationService;
    public final CodahaleMetricsListener metricsListener;
    public final CompletableFuture<AppInfo> appInfoFuture;
    public final CmsRequestSecurityValidator cmsRequestSecurityValidator;
    public final List<RequestAndResponseFilter> requestAndResponseFilters;

    @Inject
    public GuiceProvidedServerConfigValues(@Named("endpoints.port") Integer endpointsPort,
                                           @Named("endpoints.sslPort") Integer endpointsSslPort,
                                           @Named("endpoints.useSsl") Boolean endpointsUseSsl,
                                           @Named("netty.bossThreadCount") Integer numBossThreads,
                                           @Named("netty.workerThreadCount") Integer numWorkerThreads,
                                           @Named("netty.maxRequestSizeInBytes") Integer maxRequestSizeInBytes,
                                           @Named("appEndpoints") Set<Endpoint<?>> appEndpoints,
                                           @Named("debugActionsEnabled") Boolean debugActionsEnabled,
                                           @Named("debugChannelLifecycleLoggingEnabled") Boolean debugChannelLifecycleLoggingEnabled,
                                           RiposteErrorHandler riposteErrorHandler,
                                           RiposteUnhandledErrorHandler riposteUnhandledErrorHandler,
                                           RequestValidator validationService,
                                           @Nullable CodahaleMetricsListener metricsListener,
                                           @Named("appInfoFuture") CompletableFuture<AppInfo> appInfoFuture,
                                           CmsRequestSecurityValidator cmsRequestSecurityValidator,
                                           HystrixRequestAndResponseFilter hystrixRequestAndResponseFilter
    ) {
        super(endpointsPort, endpointsSslPort, endpointsUseSsl, numBossThreads, numWorkerThreads, maxRequestSizeInBytes, appEndpoints,
              debugActionsEnabled, debugChannelLifecycleLoggingEnabled);

        this.riposteErrorHandler = riposteErrorHandler;
        this.riposteUnhandledErrorHandler = riposteUnhandledErrorHandler;
        this.validationService = validationService;
        this.metricsListener = metricsListener;
        this.appInfoFuture = appInfoFuture;
        this.cmsRequestSecurityValidator = cmsRequestSecurityValidator;
        this.requestAndResponseFilters = Lists.newArrayList(hystrixRequestAndResponseFilter);
    }
}
