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
 *
 */

package com.nike.cerberus.server.config.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.nike.cerberus.config.MetricsConfigurationHelper;
import com.nike.riposte.metrics.codahale.CodahaleMetricsCollector;
import com.nike.riposte.metrics.codahale.CodahaleMetricsEngine;
import com.nike.riposte.metrics.codahale.CodahaleMetricsListener;
import com.nike.riposte.metrics.codahale.ReporterFactory;
import com.nike.riposte.metrics.codahale.SignalFxAwareCodahaleMetricsCollector;
import com.nike.riposte.metrics.codahale.contrib.DefaultGraphiteReporterFactory;
import com.nike.riposte.metrics.codahale.contrib.DefaultJMXReporterFactory;
import com.nike.riposte.metrics.codahale.contrib.DefaultSLF4jReporterFactory;
import com.nike.riposte.metrics.codahale.contrib.SignalFxReporterFactory;
import com.nike.riposte.server.config.AppInfo;
import com.signalfx.codahale.metrics.MetricBuilder;
import com.signalfx.codahale.reporter.MetricMetadataImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.nike.cerberus.config.MetricsConfigurationHelper.DEFAULT_METRIC_DETAILS_TO_SEND_TO_SIGNALFX;

/**
 * Guice Module to instantiate metrics objects
 */
public class MetricsGuiceModule extends AbstractModule {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void configure() {
    }

    /**
     * Note: The CodahaleMetricsEngine is not required, but is passed here to ensure it is created and started
     */
    @Provides
    @Singleton
    public CodahaleMetricsListener metricsListener(@Nullable CodahaleMetricsCollector metricsCollector,
                                                   @Nullable CodahaleMetricsEngine engine,
                                                   @Nullable SignalFxReporterFactory signalFxReporterFactory) {
        if (metricsCollector == null)
            return null;

        if (signalFxReporterFactory == null) {
            // No SignalFx - just use the default.
            return new CodahaleMetricsListener(metricsCollector);
        }
        else {
            // SignalFx is being used - make sure we use a CodahaleMetricsListener that sets everything up
            //      properly for SignalFx.
            return MetricsConfigurationHelper.generateCodahaleMetricsListenerWithSignalFxSupport(
                    signalFxReporterFactory,  metricsCollector, null, null
            );
        }
    }

    @Provides
    @Singleton
    public CodahaleMetricsEngine codahaleMetricsEngine(@Nullable CodahaleMetricsCollector cmc,
                                                       @Nullable List<ReporterFactory> reporters) {
        if (cmc == null)
            return null;

        if (reporters == null)
            reporters = Collections.emptyList();

        CodahaleMetricsEngine engine = new CodahaleMetricsEngine(cmc, reporters);
        engine.start();
        return engine;
    }

    @Provides
    @Singleton
    public CodahaleMetricsCollector codahaleMetricsCollector(
            @Nullable List<ReporterFactory> reporters, @Nullable SignalFxReporterFactory signalFxReporterFactory
    ) {
        if (reporters == null) {
            return null;
        }

        // Try to return a SignalFx-based collector if possible.
        if (signalFxReporterFactory != null) {
            return new SignalFxAwareCodahaleMetricsCollector(signalFxReporterFactory);
        }

        // No SignalFx support available - just use a default collector.
        return new CodahaleMetricsCollector();
    }

    @Provides
    @Singleton
    public SignalFxAwareCodahaleMetricsCollector sfxAwareCodahaleMetricsCollector(
            @Nullable CodahaleMetricsCollector configuredAppCollector
    ) {
        // If metrics are completely disabled then return null.
        if (configuredAppCollector == null)
            return null;

        return configuredAppCollector instanceof SignalFxAwareCodahaleMetricsCollector
                ? (SignalFxAwareCodahaleMetricsCollector)configuredAppCollector
                // Metrics are enabled, but SignalFx is not. Create a dummy SignalFxAwareCodahaleMetricsCollector
                //       using the real MetricRegistry for the app, a dummy MetricMetadata, and default metric builders.
                //       This will let users use the API even if SignalFx is disabled (i.e. local environment), and
                //       different metric objects will be used based on the dimensions.
                : new SignalFxAwareCodahaleMetricsCollector(
                configuredAppCollector.getMetricRegistry(), new MetricMetadataImpl(),
                MetricBuilder.TIMERS, MetricBuilder.HISTOGRAMS
        );
    }

    @Provides
    @Singleton
    public List<ReporterFactory> metricsReporters(
            @Named("metrics.slf4j.reporting.enabled") boolean slf4jReportingEnabled,
            @Named("metrics.jmx.reporting.enabled") boolean jmxReportingEnabled,
            @Named("metrics.signalfx.reporting.enabled") boolean signalFxEnabled,
            @Named("graphite.url") String graphiteUrl,
            @Named("graphite.port") int graphitePort,
            @Named("graphite.reporting.enabled") boolean graphiteEnabled,
            @Named("appInfoFuture") CompletableFuture<AppInfo> appInfoFuture,
            @Nullable SignalFxReporterFactory signalFxReporterFactory
    ) {
        List<ReporterFactory> reporters = new ArrayList<>();

        if (slf4jReportingEnabled)
            reporters.add(new DefaultSLF4jReporterFactory());

        if (jmxReportingEnabled)
            reporters.add(new DefaultJMXReporterFactory());

        if (signalFxEnabled && signalFxReporterFactory != null)
            reporters.add(signalFxReporterFactory);

        if (graphiteEnabled) {
            AppInfo appInfo = appInfoFuture.join();
            String graphitePrefix = appInfo.appId() + "." + appInfo.dataCenter() + "." + appInfo.environment()
                    + "." + appInfo.instanceId();
            reporters.add(new DefaultGraphiteReporterFactory(graphitePrefix, graphiteUrl, graphitePort));
        }

        if (reporters.isEmpty()) {
            logger.info("No metrics reporters enabled - disabling metrics entirely.");
            return null;
        }

        String metricReporterTypes = reporters.stream()
                .map(rf -> rf.getClass().getSimpleName())
                .collect(Collectors.joining(",", "[", "]"));
        logger.info("Metrics reporters enabled. metric_reporter_types={}", metricReporterTypes);

        return reporters;
    }

    @Provides
    @Singleton
    public SignalFxReporterFactory signalFxReporterFactory(
            @Named("metrics.signalfx.reporting.enabled") boolean signalFxEnabled,
            @Named("service.version") String serviceVersion,
            @Named("metrics.signalfx.api_key") String apiKey,
            @Named("cms.app.name") String appName,
            @Named("cms.app.env") String appEnv
    ) {
        if (signalFxEnabled) {
            MetricsConfigurationHelper.generateSignalFxReporterFactory(
                    serviceVersion,
                    apiKey,
                    appName,
                    appEnv,
                    null,
                    DEFAULT_METRIC_DETAILS_TO_SEND_TO_SIGNALFX);
        }

        return null;
    }
}