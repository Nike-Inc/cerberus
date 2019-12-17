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

package com.nike.cerberus.domain;

import org.junit.Test;

import java.util.Set;

import static groovy.util.GroovyTestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class VaultStyleErrorResponseTest {

    @Test
    public void test_that_the_builder_adds_all_the_errors() {
        VaultStyleErrorResponse res = VaultStyleErrorResponse.Builder.create()
                .withError("error1")
                .withError("error2")
                .build();

        assertEquals("The response should have 2 errosr", 2, res.getErrors().size());
        assertTrue("The response should have the exact 2 errors", res.getErrors().containsAll(Set.of("error1", "error2")));
    }

}
