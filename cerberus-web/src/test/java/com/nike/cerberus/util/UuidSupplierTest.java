/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.util;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Fail.fail;

public class UuidSupplierTest {

    private UuidSupplier subject;

    @Before
    public void setUp() throws Exception {
        subject = new UuidSupplier();
    }

    @Test
    public void get_returns_valid_uuid() {
        final String uuid = subject.get();

        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException iae) {
            fail("UUID generated unable to be parsed by UUID.fromString()");
        }
    }
}