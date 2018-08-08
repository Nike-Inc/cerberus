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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.Map;

/**
 * A HttpClient for interacting with AWS STS
 */
@Singleton
public class AwsStsHttpClient {

    private static final MediaType DEFAULT_CONTENT_MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded");

    private static final MediaType DEFAULT_ACCEPTED_MEDIA_TYPE = MediaType.parse("application/json");

    private static final String DEFAULT_AWS_STS_ENDPOINT = "https://sts.amazonaws.com";

    private static final String DEFAULT_GET_CALLER_IDENTITY_ACTION = "Action=GetCallerIdentity&Version=2011-06-15";

    private static final String DEFAULT_METHOD = "POST";

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
            Response response =  httpClient.newCall(request).execute();
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
        if (e instanceof SSLException
                && e.getMessage() != null
                && e.getMessage().contains("Unrecognized SSL message, plaintext connection?")) {
            return ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionCause(e)
                    .withExceptionMessage("I/O error while communicating with AWS STS. Unrecognized SSL message may be due to a web proxy e.g. AnyConnect")
                    .build();
        } else {
            return ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionCause(e)
                    .withExceptionMessage("I/O error while communicating with AWS STS.")
                    .build();
        }
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
}
