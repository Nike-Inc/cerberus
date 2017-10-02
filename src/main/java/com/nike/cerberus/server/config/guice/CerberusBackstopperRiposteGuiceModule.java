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

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.cerberus.error.SfxAwareApiExceptionHandlerUtils;
import com.nike.riposte.metrics.codahale.CodahaleMetricsCollector;
import com.nike.riposte.metrics.codahale.contrib.SignalFxReporterFactory;
import com.signalfx.codahale.reporter.MetricMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

public class CerberusBackstopperRiposteGuiceModule extends AbstractModule {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void configure() {
    }

    /**
     * @param metricsCollector The {@link CodahaleMetricsCollector} being used for the app. Can be null - if null is
     * passed in then {@link ApiExceptionHandlerUtils} will be returned.
     * @param sfxReporterFactory The {@link SignalFxReporterFactory} being used for the app. Can be null - if null is
     * passed in then {@link ApiExceptionHandlerUtils} will be returned.
     * @return A {@link SfxAwareApiExceptionHandlerUtils} if the given args are not null, or {@link
     * ApiExceptionHandlerUtils} if either arg is null.
     */
    @Provides
    @Singleton
    public ApiExceptionHandlerUtils sfxAwareApiExceptionHandlerUtils(CodahaleMetricsCollector metricsCollector,
                                                                     SignalFxReporterFactory sfxReporterFactory) {

        MetricRegistry metricRegistry = (metricsCollector == null)
                ? null
                : metricsCollector.getMetricRegistry();
        MetricMetadata sfxMetricMetadata = (sfxReporterFactory == null || metricRegistry == null)
                ? null
                : sfxReporterFactory.getReporter(metricRegistry).getMetricMetadata();

        if (metricRegistry == null || sfxMetricMetadata == null) {
            logger.warn("Unable to do SignalFx metric gathering around API Errors - the CodahaleMetricsCollector "
                            + "and/or SignalFxReporterFactory were null. Defaulting to ApiExceptionHandlerUtils. "
                            + "metrics_collector_is_null={}, sfx_reporter_factory_is_null={}",
                    (metricsCollector == null), (sfxReporterFactory == null));
            return new ApiExceptionHandlerUtils();
        }

        // We have all the bits we need to do metrics reporting, so return a SfxAwareApiExceptionHandlerUtils
        //      that will do it.
        return new SfxAwareApiExceptionHandlerUtils(metricRegistry, sfxMetricMetadata);
    }
}
