package com.nike.cerberus.config;

import com.amazonaws.util.EC2MetadataUtils;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.nike.internal.util.Pair;
import com.nike.riposte.metrics.codahale.CodahaleMetricsCollector;
import com.nike.riposte.metrics.codahale.CodahaleMetricsListener;
import com.nike.riposte.metrics.codahale.EndpointMetricsHandler;
import com.nike.riposte.metrics.codahale.contrib.SignalFxReporterFactory;
import com.nike.riposte.metrics.codahale.impl.SignalFxEndpointMetricsHandler;
import com.nike.riposte.metrics.codahale.impl.SignalFxEndpointMetricsHandler.MetricDimensionConfigurator;
import com.nike.riposte.server.http.HttpProcessingState;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.signalfx.codahale.reporter.SignalFxReporter;
import com.signalfx.codahale.reporter.SignalFxReporter.Builder;
import com.signalfx.codahale.reporter.SignalFxReporter.MetricDetails;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.nike.riposte.metrics.codahale.impl.SignalFxEndpointMetricsHandler.DEFAULT_REQUEST_LATENCY_TIMER_DIMENSION_CONFIGURATOR;

/**
 * A set of methods used to help instantiate the necessary metrics objects in the Guice module
 */
public class MetricsConfigurationHelper {

    private static final String EC2_HOSTNAME_PREFIX = "ip-";

    private final String signalFxApiKey;

    private final String signalFxAppEnvDim;

    /**
     * This 'holder' class allows optional injection of SignalFx-specific properties that are only necessary when
     * SignalFx metrics reporting is enabled.
     *
     * The 'optional=true' parameter to Guice @Inject cannot be used in combination with the @Provides annotation
     * or with constructor injection.
     *
     * https://github.com/google/guice/wiki/FrequentlyAskedQuestions
     */
    static class SignalFxOptionalPropertyHolder {
        @Inject(optional=true)
        @com.google.inject.name.Named("metrics.signalfx.api_key")
        String signalFxApiKey = "signalfx api key injection failed";  // set default value so key is never null

        @Inject(optional=true)
        @com.google.inject.name.Named("metrics.signalfx.dimension.app_env")
        String signalFxAppEnvDim = "empty";  // set default value so 'env' dimension is never null
    }

    @Inject
    public MetricsConfigurationHelper(SignalFxOptionalPropertyHolder signalFxPropertyHolder) {
        signalFxApiKey = signalFxPropertyHolder.signalFxApiKey;
        signalFxAppEnvDim = signalFxPropertyHolder.signalFxAppEnvDim;
    }

    /**
     * Generates the SignalFx metrics reporter
     * @param serviceVersionDim           The version number for this service/app
     * @param customReporterConfigurator  Allows custom SignalFx dimensions or configuration to be sent to SignalFx
     * @param metricDetailsToReport       The set of metric details that should be reported to SignalFx
     * @return  The SignalFxReporterFactory
     */
    public SignalFxReporterFactory generateSignalFxReporterFactory(
            String serviceVersionDim,
            String appNameDim,
            Function<Builder, Builder> customReporterConfigurator,
            Set<MetricDetails> metricDetailsToReport
    ) {
            Set<SignalFxReporter.MetricDetails> finalMetricDetailsToReport = (metricDetailsToReport == null)
                    ? DEFAULT_METRIC_DETAILS_TO_SEND_TO_SIGNALFX
                    : metricDetailsToReport;

            String host = EC2MetadataUtils.getLocalHostName();
            String ec2Hostname = EC2_HOSTNAME_PREFIX + EC2MetadataUtils.getPrivateIpAddress();

            String serverGroup = System.getProperty("serverGroup", "unknown");
            String serverRegion = System.getProperty("serverRegion", "unknown");

            Function<SignalFxReporter.Builder, SignalFxReporter.Builder> defaultReporterConfigurator =
                    (builder) -> builder
                            .addUniqueDimension("host", host)
                            .addUniqueDimension("ec2_hostname", ec2Hostname)
                            .addUniqueDimension("app", appNameDim)
                            .addUniqueDimension("env", signalFxAppEnvDim)
                            .addUniqueDimension("framework", "riposte")
                            .addUniqueDimension("app_version", serviceVersionDim)
                            .addUniqueDimension("server_group", serverGroup)
                            .addUniqueDimension("server_region", serverRegion)
                            .setDetailsToAdd(finalMetricDetailsToReport);

            if (customReporterConfigurator == null)
                customReporterConfigurator = Function.identity();

            return new SignalFxReporterFactory(
                    signalFxApiKey,
                    defaultReporterConfigurator.andThen(customReporterConfigurator),
                    // Report metrics at a 10 second interval
                    Pair.of(10L, TimeUnit.SECONDS)
            );
    }

    public CodahaleMetricsListener generateCodahaleMetricsListenerWithSignalFxSupport(
            SignalFxReporterFactory signalFxReporterFactory,
            CodahaleMetricsCollector metricsCollector,
            MetricDimensionConfigurator<Timer> customRequestTimerDimensionConfigurator,
            ExtraRequestLogicHandler extraRequestLogicHandler
    ) {
        MetricRegistry metricRegistry = metricsCollector.getMetricRegistry();

        // Use the identity function if customRequestTimerDimensionConfigurator is null.
        if (customRequestTimerDimensionConfigurator == null)
            customRequestTimerDimensionConfigurator = METRIC_DIMENSION_CONFIGURATOR_IDENTITY;

        // Create the SignalFxEndpointMetricsHandler with the customRequestTimerDimensionConfigurator and
        //     extraRequestLogicHandler specifics.
        EndpointMetricsHandler endpointMetricsHandler = new SignalFxEndpointMetricsHandler(
                signalFxReporterFactory.getReporter(metricRegistry).getMetricMetadata(),
                metricRegistry,
                // Use a rolling window reservoir with the same window as the reporting frequency,
                //      to prevent the dashboards from producing false or misleading data.
                new SignalFxEndpointMetricsHandler.RollingWindowTimerBuilder(signalFxReporterFactory.getInterval(),
                        signalFxReporterFactory.getTimeUnit()),
                // Do the default request latency timer dimensions, chained with customRequestTimerDimensionConfigurator
                //      for any custom logic desired.
                DEFAULT_REQUEST_LATENCY_TIMER_DIMENSION_CONFIGURATOR
                        .chainedWith(customRequestTimerDimensionConfigurator)
        ) {
            @Override
            public void handleRequest(RequestInfo<?> requestInfo, ResponseInfo<?> responseInfo,
                                      HttpProcessingState httpState,
                                      int responseHttpStatusCode, int responseHttpStatusCodeXXValue,
                                      long requestElapsedTimeMillis) {

                // Do the normal endpoint stuff.
                super.handleRequest(requestInfo, responseInfo, httpState, responseHttpStatusCode,
                        responseHttpStatusCodeXXValue,
                        requestElapsedTimeMillis);

                // Do any extra logic (if desired).
                if (extraRequestLogicHandler != null) {
                    extraRequestLogicHandler.handleExtraRequestLogic(
                            requestInfo, responseInfo, httpState, responseHttpStatusCode, responseHttpStatusCodeXXValue,
                            requestElapsedTimeMillis
                    );
                }
            }
        };

        return CodahaleMetricsListener
                .newBuilder(metricsCollector)
                .withEndpointMetricsHandler(endpointMetricsHandler)
                // The metric names should be basic with no prefix - SignalFx dimensions do the job normally covered
                //      by metric name prefixes.
                .withServerStatsMetricNamingStrategy(CodahaleMetricsListener.MetricNamingStrategy.defaultNoPrefixImpl())
                .withServerConfigMetricNamingStrategy(CodahaleMetricsListener.MetricNamingStrategy.defaultNoPrefixImpl())
                // Histograms should use a rolling window reservoir with the same window as the reporting frequency,
                //      otherwise the dashboards will produce false or misleading data.
                .withRequestAndResponseSizeHistogramSupplier(
                        () -> new Histogram(
                                new SlidingTimeWindowReservoir(signalFxReporterFactory.getInterval(),
                                        signalFxReporterFactory.getTimeUnit())
                        )
                )
                .build();
    }

    /**
     * The default set of metric details to send to SignalFx when reporting metrics. By reducing these to only the
     * common ones necessary and letting SignalFx calculate aggregates for us where possible (e.g. calculating rates
     * just from the count metric rather than us sending the pre-aggregated codahale 1min/5min/15min metric details)
     * of data sent to SignalFx is significantly decreased and therefore saves a lot of money.
     */
    public static final Set<SignalFxReporter.MetricDetails> DEFAULT_METRIC_DETAILS_TO_SEND_TO_SIGNALFX = Collections.unmodifiableSet(
            EnumSet.of(
                    SignalFxReporter.MetricDetails.COUNT,
                    SignalFxReporter.MetricDetails.MIN,
                    SignalFxReporter.MetricDetails.MEAN,
                    SignalFxReporter.MetricDetails.MAX,
                    SignalFxReporter.MetricDetails.PERCENT_95,
                    SignalFxReporter.MetricDetails.PERCENT_99
            )
    );

    /**
     * A {@link SignalFxEndpointMetricsHandler.MetricDimensionConfigurator} that is the identity function - nothing will
     * be done when it's called and it will simply return the provided rawBuilder.
     */
    public static final SignalFxEndpointMetricsHandler.MetricDimensionConfigurator<Timer>
            METRIC_DIMENSION_CONFIGURATOR_IDENTITY =
            (rawBuilder, a, b, c, d, e, f, g, h, i, j) -> rawBuilder;

    /**
     * A functional interface for doing any extra logic you want on a per-request basis (e.g. extra metrics not covered
     * by the default request timer).
     */
    @FunctionalInterface
    public interface ExtraRequestLogicHandler {

        void handleExtraRequestLogic(RequestInfo<?> requestInfo, ResponseInfo<?> responseInfo,
                                     HttpProcessingState httpState,
                                     int responseHttpStatusCode, int responseHttpStatusCodeXXValue,
                                     long requestElapsedTimeMillis);
    }
}
