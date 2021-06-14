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

package com.nike.cerberus.auth.connector.okta;

import com.okta.sdk.resource.user.factor.UserFactorList;
import com.okta.sdk.resource.user.User;
import java.util.List;

/** POJO representing embedded data within the user authentication response. */
public class EmbeddedAuthResponseDataV1 {

  private User user;

  private UserFactorList factors;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public UserFactorList getFactors() {
    return factors;
  }

  public void setFactors(UserFactorList factors) {
    this.factors = factors;
  }
}
