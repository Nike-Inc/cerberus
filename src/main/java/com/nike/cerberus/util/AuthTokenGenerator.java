package com.nike.cerberus.util;

/**
 * Generate an Auth Token
 */
public class AuthTokenGenerator {

    /**
     * Generate an Auth Token
     */
    public String generateSecureToken() {
        // TODO: additional research, security review
        return new RandomString().nextString();
    }
}
