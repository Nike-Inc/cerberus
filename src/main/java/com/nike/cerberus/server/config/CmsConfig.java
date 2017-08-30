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

package com.nike.cerberus.server.config;

import com.google.inject.util.Modules;
import com.nike.backstopper.handler.riposte.config.guice.BackstopperRiposteConfigGuiceModule;
import com.nike.cerberus.server.config.guice.*;
import com.nike.guice.PropertiesRegistrationGuiceModule;
import com.nike.guice.typesafeconfig.TypesafeConfigPropertiesRegistrationGuiceModule;
import com.nike.riposte.metrics.MetricsListener;
import com.nike.riposte.server.config.AppInfo;
import com.nike.riposte.server.config.ServerConfig;
import com.nike.riposte.server.error.handler.RiposteErrorHandler;
import com.nike.riposte.server.error.handler.RiposteUnhandledErrorHandler;
import com.nike.riposte.server.error.validation.RequestSecurityValidator;
import com.nike.riposte.server.error.validation.RequestValidator;
import com.nike.riposte.server.http.Endpoint;
import com.nike.riposte.server.logging.AccessLogger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.typesafe.config.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The {@link ServerConfig} for this application. Many of the server config option values (e.g. {@link ServerConfig#endpointsPort()} come from
 * your config properties files, and this also tells the server to use {@link #getAppGuiceModules(Config)} for the Guice modules for this app.
 * <p/>
 * If you have more modules besides {@link CmsGuiceModule} you want to use in your application just add them to the
 * {@link #getAppGuiceModules(Config)} list. You should never remove the {@link BackstopperRiposteConfigGuiceModule} module from that list
 * unless you replace it with an extension of that class that performs the same function - it is what configures the application's error handling system.
 *
 * @author Nic Munroe
 */
public class CmsConfig implements ServerConfig {

    /*
         We use a GuiceProvidedServerConfigValues to generate most of the values we need to return for ServerConfig's methods.
         Some values will be provided by config files (using a PropertiesRegistrationGuiceModule), others from CmsGuiceModule,
         and others from ExceptionHandlerNettyGuiceConfigModule. Having Guice instantiate them this way means they will be created,
         finalized, and ready for use by the time the ServerConfig methods are called. No need for synchronized methods or lazy-loading.
     */
    protected final GuiceProvidedServerConfigValues guiceValues;

    protected final Config appConfig;

    protected final AccessLogger accessLogger = new AccessLogger();

    protected final ObjectMapper objectMapper;

    protected CmsConfig(Config appConfig, PropertiesRegistrationGuiceModule propertiesRegistrationGuiceModule) {
        super();

        // Store the appConfig.
        if (appConfig == null)
            throw new IllegalArgumentException("appConfig cannot be null");

        this.appConfig = appConfig;

        this.objectMapper = configureObjectMapper();

        // Create a Guice Injector for this app.
        List<Module> appGuiceModules = new ArrayList<>();
        appGuiceModules.add(propertiesRegistrationGuiceModule);
        appGuiceModules.addAll(Arrays.asList(
                new CmsMyBatisModule(),
                new BackstopperRiposteConfigGuiceModule(),
                new CmsFlywayModule(),
                new OneLoginGuiceModule(),
                new MetricsGuiceModule()
        ));

        // bind the CMS Guice module last allowing the S3 props file to override any given application property
        Injector appInjector = Guice.createInjector(Modules.override(appGuiceModules)
                .with(new CmsGuiceModule(appConfig, objectMapper)));

        // Use the new Guice Injector to create a GuiceProvidedServerConfigValues, which will contain all the guice-provided config stuff for this app.
        this.guiceValues = appInjector.getProvider(GuiceProvidedServerConfigValues.class).get();

        // Now that everything else is setup, we can initialize the metrics listener.
        if (guiceValues.metricsListener != null)
            guiceValues.metricsListener.initEndpointAndServerConfigMetrics(this);
    }

    public CmsConfig(Config appConfig) {
        this(appConfig, new TypesafeConfigPropertiesRegistrationGuiceModule(appConfig));
    }

    public static ObjectMapper configureObjectMapper() {
        final ObjectMapper om = new ObjectMapper();
        om.findAndRegisterModules();
        om.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        om.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        om.enable(SerializationFeature.INDENT_OUTPUT);
        return om;
    }

    @Override
    public ObjectMapper defaultRequestContentDeserializer() { return objectMapper; }

    @Override
    public ObjectMapper defaultResponseContentSerializer() { return objectMapper; }

    @Override
    public AccessLogger accessLogger() {
        return accessLogger;
    }

    @Override
    public CompletableFuture<AppInfo> appInfo() {
        return guiceValues.appInfoFuture;
    }

    @Override
    public MetricsListener metricsListener() {
        return guiceValues.metricsListener;
    }

    @Override
    public RequestSecurityValidator requestSecurityValidator() {
        return guiceValues.cmsRequestSecurityValidator;
    }

    @Override
    public Collection<Endpoint<?>> appEndpoints() {
        return guiceValues.appEndpoints;
    }

    @Override
    public RiposteErrorHandler riposteErrorHandler() {
        return guiceValues.riposteErrorHandler;
    }

    @Override
    public RiposteUnhandledErrorHandler riposteUnhandledErrorHandler() {
        return guiceValues.riposteUnhandledErrorHandler;
    }

    @Override
    public RequestValidator requestContentValidationService() {
        return guiceValues.validationService;
    }

    @Override
    public boolean isDebugActionsEnabled() {
        return guiceValues.debugActionsEnabled;
    }

    @Override
    public boolean isDebugChannelLifecycleLoggingEnabled() {
        return guiceValues.debugChannelLifecycleLoggingEnabled;
    }

    @Override
    public int endpointsPort() {
        return guiceValues.endpointsPort;
    }

    @Override
    public int endpointsSslPort() {
        return guiceValues.endpointsSslPort;
    }

    @Override
    public boolean isEndpointsUseSsl() {
        return guiceValues.endpointsUseSsl;
    }

    @Override
    public int numBossThreads() {
        return guiceValues.numBossThreads;
    }

    @Override
    public int numWorkerThreads() {
        return guiceValues.numWorkerThreads;
    }

    @Override
    public int maxRequestSizeInBytes() {
        return guiceValues.maxRequestSizeInBytes;
    }
}
