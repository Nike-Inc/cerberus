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

package com.nike.cerberus.auth.connector.okta;

import com.okta.sdk.models.factors.Factor;
import com.okta.sdk.models.users.User;
import java.util.List;

/** POJO representing embedded data within the user authentication response. */
public class EmbeddedAuthResponseDataV1 {

  private User user;

  private List<Factor> factors;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public List<Factor> getFactors() {
    return factors;
  }

  public void setFactors(List<Factor> factors) {
    this.factors = factors;
  }
}
