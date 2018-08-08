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

package com.nike.cerberus.aws.sts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.server.config.guice.AwsStsGuiceModule;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A HttpClient for interacting with AWS STS
 */
@Singleton
public class AwsStsHttpClient {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private static final MediaType DEFAULT_CONTENT_MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded");

    private static final MediaType DEFAULT_ACCEPTED_MEDIA_TYPE = MediaType.parse("application/json");

    private static final String DEFAULT_AWS_STS_ENDPOINT = "https://sts.amazonaws.com";

    private static final String DEFAULT_GET_CALLER_IDENTITY_ACTION = "Action=GetCallerIdentity&Version=2011-06-15";

    private static final String DEFAULT_METHOD = "POST";

    protected static final int DEFAULT_AUTH_RETRIES = 3;

    protected static final int DEFAULT_RETRY_INTERVAL_IN_MILLIS = 200;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public AwsStsHttpClient(@Named(AwsStsGuiceModule.AWS_STS_HTTP_CLIENT_NAME) final OkHttpClient httpClient,
                            @Named(AwsStsGuiceModule.AWS_STS_OBJECT_MAPPER_NAME) final ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the HTTP request based on the input parameters.
     *
     * @param headers     HTTP Headers to include in the request
     * @param responseClass The class of the response object
     * @return Response from the server
     */
    public <M> M execute(final Map<String, String> headers,
                            final Class<M> responseClass) {
        try {
            Request request = buildRequest(headers);
            Response response = executeRequestWithRetry(request, DEFAULT_AUTH_RETRIES, DEFAULT_RETRY_INTERVAL_IN_MILLIS);
            if (response.code() >= 400 && response.code() < 500) {
                final String msg = String.format("Failed to authenticate with AWS, error message: %s",
                        response.body().string());

                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS)
                        .withExceptionMessage(msg)
                        .build();
            } else if (response.code() >= 500){
                final String msg = String.format("Something is wrong with AWS, error message: %s",
                        response.body().string());

                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                        .withExceptionMessage(msg)
                        .build();
            }
            return parseResponseBody(response, responseClass);
        } catch (IOException e) {
            throw toApiException(e);
        }
    }

    /**
     * Build the request
     *
     * @throws JsonProcessingException
     */
    protected Request buildRequest(Map<String, String> headers) {
        Request.Builder requestBuilder = new Request.Builder().url(DEFAULT_AWS_STS_ENDPOINT)
                .addHeader("Accept", DEFAULT_ACCEPTED_MEDIA_TYPE.toString());

        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }

        requestBuilder.addHeader("Content-Type", DEFAULT_CONTENT_MEDIA_TYPE.toString())
                .method(DEFAULT_METHOD, RequestBody.create(DEFAULT_CONTENT_MEDIA_TYPE, DEFAULT_GET_CALLER_IDENTITY_ACTION
                        ));

        return requestBuilder.build();
    }

    protected ApiException toApiException(IOException e) {
        return ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionCause(e)
                    .withExceptionMessage("I/O error while communicating with AWS STS.")
                    .build();
    }

    /**
     * Convenience method for parsing the HTTP response and mapping it to a class.
     *
     * @param response      The HTTP response object
     * @param responseClass The class to map the response body to
     * @param <M>           Represents the type to map to
     * @return Deserialized object from the response body
     */
    protected <M> M parseResponseBody(final Response response, final Class<M> responseClass) {
        try {
            return objectMapper.readValue(response.body().string(), responseClass);
        } catch (IOException e) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionCause(e)
                    .withExceptionMessage("Error parsing the response body from AWS STS.")
                    .build();
        }
    }

    /**
     * Executes an HTTP request and retries if a 500 level error is returned
     * @param request                The request to execute
     * @param numRetries             The maximum number of times to retry
     * @param sleepIntervalInMillis  Time in milliseconds to sleep between retries. Zero for no sleep.
     * @return Any HTTP response with status code below 500, or the last error response if only 500's are returned
     * @throws IOException  If an IOException occurs during the last retry, then rethrow the error
     */
    protected Response executeRequestWithRetry(Request request, int numRetries, int sleepIntervalInMillis) throws IOException {
        IOException exception = null;
        Response response = null;
        for(int retryNumber = 0; retryNumber < numRetries; retryNumber++) {
            try {
                response = httpClient.newCall(request).execute();
                if (response.code() < 500) {
                    return response;
                }
            } catch (IOException ioe) {
                LOGGER.debug(String.format("Failed to call %s %s. Retrying...", request.method(), request.url()), ioe);
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
            LOGGER.warn("Sleep interval interrupted.", ie);
        }
    }
}
