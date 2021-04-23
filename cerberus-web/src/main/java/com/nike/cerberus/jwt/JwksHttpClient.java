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

package com.nike.cerberus.jwt;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.util.CustomApiError;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

/** A HttpClient for interacting with JSON Web Key Set (JWKS) endpoint */
@Slf4j
@Component
public class JwksHttpClient {

  protected static final int DEFAULT_AUTH_RETRIES = 5;
  private static final int DEFAULT_RETRY_INTERVAL_IN_MILLIS = 250;
  private static final int DEFAULT_TIMEOUT = 15;
  private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  public JwksHttpClient() {
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
            .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
            .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
            .build();
  }

  public JwksHttpClient(OkHttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public JsonWebKeySet getJsonWebKeySet(final String jwksEndpoint) {

    return execute(jwksEndpoint, JsonWebKeySet.class);
  }
  /**
   * Executes the HTTP request based on the input parameters.
   *
   * @param region The region to call sts get caller identity in.
   * @param headers HTTP Headers to include in the request
   * @param responseClass The class of the response object
   * @return Response from the server
   */
  public <M> M execute(final String jwksEndpoint, final Class<M> responseClass) {
    try {
      Request request = buildRequest(jwksEndpoint);
      Response response =
          executeRequestWithRetry(request, DEFAULT_AUTH_RETRIES, DEFAULT_RETRY_INTERVAL_IN_MILLIS);
      if (!response.isSuccessful()) {
        throw new RuntimeException("placeholder"); // todo
      }
      return parseResponseBody(response, responseClass);
    } catch (IOException e) {
      throw toApiException(e);
    }
  }

  /** Build the request */
  protected Request buildRequest(final String jwksEndpoint) {
    return new Request.Builder().url(jwksEndpoint).build();
  }

  protected ApiException toApiException(IOException e) {
    String msg = "I/O error while communicating with AWS STS.";
    return ApiException.newBuilder()
        .withApiErrors(
            CustomApiError.createCustomApiError(DefaultApiError.SERVICE_UNAVAILABLE, msg))
        .withExceptionCause(e)
        .withExceptionMessage(msg)
        .build();
  }

  /**
   * Convenience method for parsing the HTTP response and mapping it to a class.
   *
   * @param response The HTTP response object
   * @param responseClass The class to map the response body to
   * @param <M> Represents the type to map to
   * @return Deserialized object from the response body
   */
  protected <M> M parseResponseBody(final Response response, final Class<M> responseClass) {
    try {
      return objectMapper.readValue(response.body().string(), responseClass);
    } catch (IOException e) {
      String msg = "Error parsing the response body from AWS STS.";
      throw ApiException.newBuilder()
          .withApiErrors(
              CustomApiError.createCustomApiError(DefaultApiError.SERVICE_UNAVAILABLE, msg))
          .withExceptionCause(e)
          .withExceptionMessage(msg)
          .build();
    }
  }

  /**
   * Executes an HTTP request and retries if a 500 level error is returned
   *
   * @param request The request to execute
   * @param numRetries The maximum number of times to retry
   * @param sleepIntervalInMillis Time in milliseconds to sleep between retries. Zero for no sleep.
   * @return Any HTTP response with status code below 500, or the last error response if only 500's
   *     are returned
   * @throws IOException If an IOException occurs during the last retry, then rethrow the error
   */
  protected Response executeRequestWithRetry(
      Request request, int numRetries, int sleepIntervalInMillis) throws IOException {
    IOException exception = null;
    Response response = null;
    for (int retryNumber = 0; retryNumber < numRetries; retryNumber++) {
      try {
        response = httpClient.newCall(request).execute();
        if (response.code() < 500) {
          return response;
        }
      } catch (IOException ioe) {
        log.debug(
            String.format("Failed to call %s %s. Retrying...", request.method(), request.url()),
            ioe);
        exception = ioe;
      }
      sleep(sleepIntervalInMillis * (long) Math.pow(2, retryNumber));
    }

    if (exception != null) {
      throw exception;
    } else {
      return response;
    }
  }

  private void sleep(long milliseconds) {
    try {
      TimeUnit.MILLISECONDS.sleep(milliseconds);
    } catch (InterruptedException ie) {
      log.warn("Sleep interval interrupted.", ie);
    }
  }
}
