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

package com.nike.cerberus.auth.connector.onelogin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** A HttpClient for interacting with OneLogin */
@Component
public class OneLoginHttpClient {

  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json");
  private static final String DEFAULT_ONELOGIN_API_URI_TEMPLATE = "https://api.%s.onelogin.com/";
  private static final int DEFAULT_TIMEOUT = 15;
  private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

  private final URI oneloginApiUri;
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  static ObjectMapper getObjectMapper() {
    var objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    return objectMapper;
  }

  public OneLoginHttpClient(OneLoginConfigurationProperties oneLoginConfigurationProperties) {

    this.oneloginApiUri =
        URI.create(
            String.format(
                DEFAULT_ONELOGIN_API_URI_TEMPLATE, oneLoginConfigurationProperties.getApiRegion()));

    this.httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
            .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
            .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
            .build();

    this.objectMapper = getObjectMapper();
  }

  /**
   * Executes the HTTP request based on the input parameters.
   *
   * @param path The Path to execute the request against
   * @param method The HTTP method for the request
   * @param headers HTTP Headers to include in the request
   * @param requestBody The request body of the HTTP request
   * @param responseClass The class of the response object
   * @return Response from the server
   */
  public <M> M execute(
      final String path,
      final String method,
      final Map<String, String> headers,
      final Object requestBody,
      final Class<M> responseClass) {
    try {
      Request request = buildRequest(buildUrl(path), method, headers, requestBody);
      Response response = httpClient.newCall(request).execute();
      return parseResponseBody(response, responseClass);
    } catch (IOException e) {
      throw toApiException(e);
    }
  }

  /**
   * Builds the full URL.
   *
   * @param path Path for the requested operation
   * @return Full URL to execute a request against
   */
  protected HttpUrl buildUrl(final String path) {
    String baseUrl = oneloginApiUri.toString();

    if (!StringUtils.endsWith(baseUrl, "/")) {
      baseUrl += "/";
    }

    return HttpUrl.parse(baseUrl + path);
  }

  /**
   * Build the request
   *
   * @param url The URL to execute the request against
   * @param method The HTTP method for the request
   * @param requestBody The request body of the HTTP request
   * @throws JsonProcessingException
   */
  protected Request buildRequest(
      HttpUrl url, String method, Map<String, String> headers, Object requestBody)
      throws JsonProcessingException {
    Request.Builder requestBuilder =
        new Request.Builder().url(url).addHeader("Accept", DEFAULT_MEDIA_TYPE.toString());

    if (headers != null) {
      headers.forEach(requestBuilder::addHeader);
    }

    if (requestBody != null) {
      requestBuilder
          .addHeader("Content-Type", DEFAULT_MEDIA_TYPE.toString())
          .method(
              method,
              RequestBody.create(
                  DEFAULT_MEDIA_TYPE,
                  objectMapper.writeValueAsString(requestBody).getBytes(Charset.forName("UTF-8"))));
    } else {
      requestBuilder.method(method, null);
    }

    return requestBuilder.build();
  }

  protected ApiException toApiException(IOException e) {
    if (e instanceof SSLException
        && e.getMessage() != null
        && e.getMessage().contains("Unrecognized SSL message, plaintext connection?")) {
      return ApiException.newBuilder()
          .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
          .withExceptionCause(e)
          .withExceptionMessage(
              "I/O error while communicating with OneLogin. Unrecognized SSL message may be due to a web proxy e.g. AnyConnect")
          .build();
    } else {
      return ApiException.newBuilder()
          .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
          .withExceptionCause(e)
          .withExceptionMessage("I/O error while communicating with OneLogin.")
          .build();
    }
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
    final ResponseBody body = response.body();
    try {
      final String responseBodyString = body == null ? "" : body.string();
      return objectMapper.readValue(responseBodyString, responseClass);
    } catch (IOException e) {
      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
          .withExceptionCause(e)
          .withExceptionMessage("Error parsing the response body from OneLogin.")
          .build();
    }
  }
}
