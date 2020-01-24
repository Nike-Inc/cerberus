/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.auth.connector.onelogin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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

  @Test
  public void test_hashCode() {
    assertEquals(new GenerateTokenRequest().hashCode(), new GenerateTokenRequest().hashCode());
    assertTrue(
        new GenerateTokenRequest().hashCode()
            != new GenerateTokenRequest().setGrantType("foo").hashCode());
  }
}
