/*
 * Copyright (c) 2017 Nike, Inc.
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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Generate an Auth Token
 * <p>
 * References:
 * https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string/41156#41156
 */
public class AuthTokenGenerator {

    // TODO: additional research, security review

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase(Locale.ROOT);
    private static final String DIGITS = "0123456789";
    private static final String ALPHANUM = UPPER + LOWER + DIGITS;

    private static final char[] SYMBOLS = ALPHANUM.toCharArray();

    private static final int LENGTH = 64;

    private final SecureRandom random = SecureRandom.getInstanceStrong();

    public AuthTokenGenerator() throws NoSuchAlgorithmException {
    }

    /**
     * Generate an Auth Token
     */
    public String generateSecureToken() {
        final char[] buf = new char[LENGTH];
        for (int i = 0; i < buf.length; ++i) {
            buf[i] = SYMBOLS[random.nextInt(SYMBOLS.length)];
        }
        return new String(buf);
    }
}
