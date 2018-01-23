package com.nike.cerberus.auth.connector.onelogin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.server.config.guice.OneLoginGuiceModule;
import okhttp3.*;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.lang.Object;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

/**
 * A HttpClient for interacting with OneLogin
 */
@Singleton
public class OneLoginHttpClient {

    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json");

    private static final Set<String> VALID_API_REGIONS = Sets.newHashSet("us", "eu");

    private static final String DEFAULT_ONELOGIN_API_URI_TEMPLATE = "https://api.%s.onelogin.com/";

    private final URI oneloginApiUri;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public OneLoginHttpClient(@Named("auth.connector.onelogin.api_region") final String oneloginApiRegion,
                              final OkHttpClient httpClient,
                              @Named(OneLoginGuiceModule.ONE_LOGIN_OBJECT_MAPPER_NAME) final ObjectMapper objectMapper) {
        Preconditions.checkArgument(VALID_API_REGIONS.contains(oneloginApiRegion),
                "OneLogin API region is invalid! Valid values: %s, %s",
                VALID_API_REGIONS.toArray());

        this.oneloginApiUri = URI.create(String.format(DEFAULT_ONELOGIN_API_URI_TEMPLATE, oneloginApiRegion));

        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the HTTP request based on the input parameters.
     *
     * @param path        The Path to execute the request against
     * @param method      The HTTP method for the request
     * @param headers     HTTP Headers to include in the request
     * @param requestBody The request body of the HTTP request
     * @param responseClass The class of the response object
     * @return Response from the server
     */
    public <M> M execute(final String path,
                            final String method,
                            final Map<String, String> headers,
                            final Object requestBody,
                            final Class<M> responseClass) {
        try {
            Request request = buildRequest(buildUrl(path), method, headers, requestBody);
            Response response =  httpClient.newCall(request).execute();
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
     * @param url         The URL to execute the request against
     * @param method      The HTTP method for the request
     * @param requestBody The request body of the HTTP request
     * @throws JsonProcessingException
     */
    protected Request buildRequest(HttpUrl url, String method, Map<String, String> headers, Object requestBody) throws JsonProcessingException {
        Request.Builder requestBuilder = new Request.Builder().url(url)
                .addHeader("Accept", DEFAULT_MEDIA_TYPE.toString());

        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }

        if (requestBody != null) {
            requestBuilder.addHeader("Content-Type", DEFAULT_MEDIA_TYPE.toString())
                    .method(method, RequestBody.create(DEFAULT_MEDIA_TYPE,
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
                    .withExceptionMessage("I/O error while communicating with OneLogin. Unrecognized SSL message may be due to a web proxy e.g. AnyConnect")
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
                    .withExceptionMessage("Error parsing the response body from OneLogin.")
                    .build();
        }
    }
}
