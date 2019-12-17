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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

public class DateTimeSupplierTest {

    private DateTimeSupplier subject;

    @Before
    public void setUp() throws Exception {
        subject = new DateTimeSupplier();
    }

    @Test
    public void test_now_returns_utc_time() {
        final OffsetDateTime dateTime = subject.get();
        final ZoneOffset offset = dateTime.getOffset();

        assertThat(offset.getTotalSeconds()).isEqualTo(0);
    }
}