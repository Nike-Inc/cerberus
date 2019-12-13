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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class SluggerTest {

    private Slugger subject;

    @Before
    public void setUp() throws Exception {
        subject = new Slugger();
    }

    @Test
    public void test_to_slug() {
        final String testCase = "èternäl testíng";
        final String expected = "eternal-testing";

        assertThat(subject.toSlug(testCase)).isEqualTo(expected);
    }

    @Test
    public void test_sluggify_kms_alias() {
        assertEquals("x-y-z", subject.slugifyKmsAliases("x.y.z"));
        assertEquals("accountId/role-name", subject.slugifyKmsAliases("accountId/role.name"));
    }
}