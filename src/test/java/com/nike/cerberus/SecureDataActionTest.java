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
 */

package com.nike.cerberus;

import com.google.common.collect.ImmutableSet;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;

import static com.nike.cerberus.SecureDataAction.DELETE;
import static com.nike.cerberus.SecureDataAction.READ;
import static com.nike.cerberus.SecureDataAction.WRITE;
import static junit.framework.TestCase.assertEquals;

public class SecureDataActionTest {

    @Test
    public void test_that_SecureDataAction_fromMethod_returns_proper_actions() {
        assertEquals(READ, SecureDataAction.fromMethod(HttpMethod.GET));
        assertEquals(WRITE, SecureDataAction.fromMethod(HttpMethod.POST));
        assertEquals(WRITE, SecureDataAction.fromMethod(HttpMethod.PUT));
        assertEquals(DELETE, SecureDataAction.fromMethod(HttpMethod.DELETE));
    }

    @Test
    public void test_that_SecureDataAction_returns_expected_method() {
        assertEquals(ImmutableSet.of(HttpMethod.GET), SecureDataAction.READ.getMethods());
        assertEquals(ImmutableSet.of(HttpMethod.POST, HttpMethod.PUT), SecureDataAction.WRITE.getMethods());
        assertEquals(ImmutableSet.of(HttpMethod.DELETE), SecureDataAction.DELETE.getMethods());
    }
}
