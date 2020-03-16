/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.error;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableSet;
import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.cerberus.metric.MetricsService;
import com.nike.internal.util.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * A SignalFx-aware ApiExceptionHandlerUtils that increments an api_errors {@link Counter} metric
 * with the following dimensions (based on the error info that gets logged): response_code,
 * contributing_errors, and exception_class.
 */
@Primary
@Component
public class SfxAwareApiExceptionHandlerUtils extends ApiExceptionHandlerUtils {

  /** The name of the API errors metric sent to SignalFx. */
  public static final String API_ERRORS_METRIC_NAME = "api_errors";
  /** The name/key of the HTTP response code dimension applied to the API errors metric. */
  public static final String RESPONSE_CODE_DIM_KEY = "response_code";
  /** The name/key of the contributing errors dimension applied to the API errors metric. */
  public static final String CONTRIBUTING_ERRORS_DIM_KEY = "contributing_errors";
  /** The name/key of the exception class dimension applied to the API errors metric. */
  public static final String EXCEPTION_CLASS_DIM_KEY = "exception_class";
  /** The names/keys of sensitive HTTP headers in lower case. */
  public static final Set<String> sensitiveHeaderNamesInLowerCase =
      ImmutableSet.of("authorization", "x-amz-security-token", "x-cerberus-token", "x-vault-token");

  private final MetricsService metricsService;

  @Autowired
  public SfxAwareApiExceptionHandlerUtils(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @Override
  public String buildErrorMessageForLogs(
      StringBuilder sb,
      RequestInfoForLogging request,
      Collection<ApiError> contributingErrors,
      Integer httpStatusCode,
      Throwable cause,
      List<Pair<String, String>> extraDetailsForLogging) {
    reactSensitiveHeaders(request);
    try {
      // Do the normal logging thing.
      return super.buildErrorMessageForLogs(
          sb, request, contributingErrors, httpStatusCode, cause, extraDetailsForLogging);
    } finally {
      // Update SignalFx metrics around API Errors.
      String contributingErrorsString =
          contributingErrors == null ? "[NONE]" : concatenateErrorCollection(contributingErrors);

      metricsService
          .getOrCreateCounter(
              API_ERRORS_METRIC_NAME,
              Map.of(
                  RESPONSE_CODE_DIM_KEY, String.valueOf(httpStatusCode),
                  CONTRIBUTING_ERRORS_DIM_KEY, contributingErrorsString,
                  EXCEPTION_CLASS_DIM_KEY, cause.getClass().getName()))
          .inc();
    }
  }

  private void reactSensitiveHeaders(RequestInfoForLogging request) {
    List<String> redactedHeaderValue = Arrays.asList("REDACTED");

    Map<String, List<String>> headersMap = request.getHeadersMap();
    Set<String> headerNames =
        headersMap.keySet().stream()
            .filter(name -> sensitiveHeaderNamesInLowerCase.contains(name.toLowerCase()))
            .collect(Collectors.toSet());
    headerNames.stream().forEach(name -> headersMap.put(name, redactedHeaderValue));
  }
}
