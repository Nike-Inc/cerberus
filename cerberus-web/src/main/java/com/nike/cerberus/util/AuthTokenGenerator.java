/*
 * Copyright (c) 2019 Nike, Inc.
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
 *
 */

package com.nike.cerberus.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Generate an Auth Token.
 * <p>
 * Auth tokens have a configurable TTL, e.g. 1 hour.
 * <p>
 * References:
 * https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string/41156#41156
 */
@Component
public class AuthTokenGenerator {

    public static final String AUTH_TOKEN_LENGTH_CONFIG_PARAM = "${cerberus.auth.token.generate.length}";

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase(Locale.ROOT);
    private static final String DIGITS = "0123456789";
    private static final String ALPHANUM = UPPER + LOWER + DIGITS;

    private static final char[] SYMBOLS = ALPHANUM.toCharArray();

    private final int length;

    @Autowired
    public AuthTokenGenerator(@Value(AUTH_TOKEN_LENGTH_CONFIG_PARAM) final int length) throws NoSuchAlgorithmException {
        if (length < 64) {
            throw new IllegalArgumentException(AUTH_TOKEN_LENGTH_CONFIG_PARAM + " must be at least 64 but was " + length);
        }
        this.length = length;
    }

    /**
     * Generate an Auth Token
     */
    public String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        final char[] buf = new char[length];
        for (int i = 0; i < buf.length; ++i) {
            buf[i] = SYMBOLS[random.nextInt(SYMBOLS.length)];
        }
        return new String(buf);
    }
}
