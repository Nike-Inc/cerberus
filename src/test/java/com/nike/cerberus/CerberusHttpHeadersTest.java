package com.nike.cerberus;

import com.nike.riposte.server.http.RequestInfo;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CerberusHttpHeadersTest {

    @Test
    public void test_getClientVersion() {
        String fakeVersion = "fake/1.2.3";
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(CerberusHttpHeaders.HEADER_X_CERBERUS_CLIENT, fakeVersion);
        RequestInfo request = mock(RequestInfo.class);
        when(request.getHeaders()).thenReturn(headers);

        Assert.assertEquals(fakeVersion, CerberusHttpHeaders.getClientVersion(request));
    }

    @Test
    public void test_getClientVersion_when_null() {
        RequestInfo request = mock(RequestInfo.class);

        Assert.assertEquals("Unknown", CerberusHttpHeaders.getClientVersion(request));
    }

    @Test
    public void test_getXForwardedClientIp_with_three() {
        String value = "ip1, ip2, ip3";
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(CerberusHttpHeaders.HEADER_X_FORWARDED_FOR, value);
        RequestInfo request = mock(RequestInfo.class);
        when(request.getHeaders()).thenReturn(headers);

        Assert.assertEquals("ip1", CerberusHttpHeaders.getXForwardedClientIp(request));
    }

    @Test
    public void test_getXForwardedClientIp_with_one() {
        String value = "ip1";
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(CerberusHttpHeaders.HEADER_X_FORWARDED_FOR, value);
        RequestInfo request = mock(RequestInfo.class);
        when(request.getHeaders()).thenReturn(headers);

        Assert.assertEquals("ip1", CerberusHttpHeaders.getXForwardedClientIp(request));
    }

    @Test
    public void test_getXForwardedClientIp_with_null() {
        RequestInfo request = mock(RequestInfo.class);

        Assert.assertNull(CerberusHttpHeaders.getXForwardedClientIp(request));
    }
}