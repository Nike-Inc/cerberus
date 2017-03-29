package com.nike.cerberus.auth.connector.onelogin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GenerateTokenRequestTest {

    @Test
    public void test_getGrantType() {
        assertEquals("client_credentials", new GenerateTokenRequest().getGrantType());
    }

    @Test
    public void test_setGrantType() {
        assertEquals("foo", new GenerateTokenRequest().setGrantType("foo").getGrantType());
    }

    @Test
    public void test_equals() {
        assertEquals(new GenerateTokenRequest(), new GenerateTokenRequest());
    }

}