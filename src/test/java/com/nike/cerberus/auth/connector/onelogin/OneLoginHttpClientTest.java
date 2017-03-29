package com.nike.cerberus.auth.connector.onelogin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.server.config.guice.OneLoginGuiceModule;
import okhttp3.*;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class OneLoginHttpClientTest {

    private static final String oneloginApiRegion = "us";

    private ObjectMapper objectMapper;
    private OkHttpClient httpClient;
    private OneLoginHttpClient oneLoginHttpClient;

    @Before
    public void setup() {
        OneLoginGuiceModule module = new OneLoginGuiceModule();
        objectMapper = module.getObjectMapper();
        httpClient = mock(OkHttpClient.class);
        oneLoginHttpClient = new OneLoginHttpClient(oneloginApiRegion, httpClient, objectMapper);
    }

    @Test
    public void test_execute() throws Exception {

        long statusCode = 200L;
        long userId = 100L;

        Response response = createFakeGetUserResponse(statusCode, userId);
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(httpClient.newCall(any())).thenReturn(call);

        // invoke method under test
        GetUserResponse actualResponse = oneLoginHttpClient.execute("fake/path", "GET", null, null, GetUserResponse.class);

        assertEquals(statusCode, actualResponse.getStatus().getCode());
        assertEquals(userId, actualResponse.getData().get(0).getId());
    }

    @Test(expected = ApiException.class)
    public void test_execute_handles_error() throws Exception {

        Call call = mock(Call.class);
        when(call.execute()).thenReturn(createFakeUnparsableResponse());
        when(httpClient.newCall(any())).thenReturn(call);

        // invoke method under test
        oneLoginHttpClient.execute("fake/path", "GET", null, null, GetUserResponse.class);
    }

    @Test
    public void test_buildRequest() throws Exception {
        String method = "POST";

        VerifyFactorRequest body = new VerifyFactorRequest()
                .setDeviceId("deviceId")
                .setStateToken("stateToken")
                .setOtpToken("otpToken");

        // invoke method under test
        Request request = oneLoginHttpClient.buildRequest(oneLoginHttpClient.buildUrl("foo/bar"), method, Maps.newHashMap(), body);

        assertEquals(2, request.headers().size());
        assertEquals("https://api.us.onelogin.com/foo/bar", request.url().uri().toString());
        assertEquals(method, request.method());
        assertEquals(90, request.body().contentLength());
    }

    @Test
    public void test_buildRequest_with_no_body() throws JsonProcessingException {

        String method = "GET";

        // invoke method under test
        Request request = oneLoginHttpClient.buildRequest(oneLoginHttpClient.buildUrl("foo/bar"), method, Maps.newHashMap(), null);

        assertEquals(1, request.headers().size());
        assertEquals("https://api.us.onelogin.com/foo/bar", request.url().uri().toString());
        assertEquals(method, request.method());
    }

    @Test
    public void test_toApiException_ssl_plain_text_error() {
        SSLException sslException = new SSLException("Unrecognized SSL message, plaintext connection?");
        assertTrue(oneLoginHttpClient.toApiException(sslException).getMessage().contains("Unrecognized SSL message may be due to a web proxy"));
    }

    @Test
    public void test_toApiException_with_generic_error() {
        assertEquals("I/O error while communicating with OneLogin.", oneLoginHttpClient.toApiException(new IOException()).getMessage());
    }

    @Test
    public void test_buildUrl() {
        assertEquals("https://api.us.onelogin.com/foo/bar", oneLoginHttpClient.buildUrl("foo/bar").uri().toString());
    }

    @Test
    public void test_parseResponseBody() throws Exception {

        long statusCode = 200L;
        long userId = 100L;

        Response response = createFakeGetUserResponse(statusCode, userId);

        // invoke method under test
        GetUserResponse actualResponse = oneLoginHttpClient.parseResponseBody(response, GetUserResponse.class);

        assertEquals(statusCode, actualResponse.getStatus().getCode());
        assertEquals(userId, actualResponse.getData().get(0).getId());
    }


    @Test(expected = ApiException.class)
    public void test_parseResponseBody_malformed_response() throws Exception {
        // invoke method under test
        oneLoginHttpClient.parseResponseBody(createFakeUnparsableResponse(), GetUserResponse.class);
    }


    private Response createFakeGetUserResponse(long statusCode, long userId) throws JsonProcessingException {
        ResponseStatus status = new ResponseStatus();
        status.setCode(statusCode);
        status.setError(false);

        UserData data = new UserData();
        data.setId(userId);
        data.setEmail("fake@example.com");

        GetUserResponse value = new GetUserResponse();
        value.setStatus(status);
        value.setData(Lists.newArrayList(data));

        String body = objectMapper.writeValueAsString(value);

        return new Response.Builder()
                .request(new Request.Builder().url("https://example.com/fake").build())
                .body(ResponseBody.create(null, body))
                .protocol(Protocol.HTTP_2)
                .code(200)
                .build();
    }

    private Response createFakeUnparsableResponse() {
        String body = "{ \"foo\": \"bar\" ";

        return new Response.Builder()
                .request(new Request.Builder().url("https://example.com/fake").build())
                .body(ResponseBody.create(null, body))
                .protocol(Protocol.HTTP_2)
                .code(200)
                .build();
    }

}
