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

/** POJO representing a create session login token request. */
class CreateSessionLoginTokenRequest {

  private String usernameOrEmail;

  private String password;

  private String subdomain;

  public String getUsernameOrEmail() {
    return usernameOrEmail;
  }

  public CreateSessionLoginTokenRequest setUsernameOrEmail(String usernameOrEmail) {
    this.usernameOrEmail = usernameOrEmail;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public CreateSessionLoginTokenRequest setPassword(String password) {
    this.password = password;
    return this;
  }

  public String getSubdomain() {
    return subdomain;
  }

  public CreateSessionLoginTokenRequest setSubdomain(String subdomain) {
    this.subdomain = subdomain;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CreateSessionLoginTokenRequest that = (CreateSessionLoginTokenRequest) o;

    if (usernameOrEmail != null
        ? !usernameOrEmail.equals(that.usernameOrEmail)
        : that.usernameOrEmail != null) return false;
    if (password != null ? !password.equals(that.password) : that.password != null) return false;
    return subdomain != null ? subdomain.equals(that.subdomain) : that.subdomain == null;
  }

  @Override
  public int hashCode() {
    int result = usernameOrEmail != null ? usernameOrEmail.hashCode() : 0;
    result = 31 * result + (password != null ? password.hashCode() : 0);
    result = 31 * result + (subdomain != null ? subdomain.hashCode() : 0);
    return result;
  }
}
