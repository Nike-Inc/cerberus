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

package com.nike.cerberus.auth.connector;

import com.nike.cerberus.domain.AuthTokenResponse;
import java.util.LinkedList;
import java.util.List;

/** Represents the authentication data returned by the auth connector. */
public class AuthData {

  private String userId;

  private String username;

  private String stateToken;

  private List<AuthMfaDevice> devices = new LinkedList<>();

  private AuthTokenResponse clientToken;

  public String getUserId() {
    return userId;
  }

  public AuthData setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public AuthData setUsername(String username) {
    this.username = username;
    return this;
  }

  public String getStateToken() {
    return stateToken;
  }

  public AuthData setStateToken(String stateToken) {
    this.stateToken = stateToken;
    return this;
  }

  public List<AuthMfaDevice> getDevices() {
    return devices;
  }

  public AuthData setDevices(List<AuthMfaDevice> devices) {
    this.devices = devices;
    return this;
  }

  public AuthTokenResponse getClientToken() {
    return clientToken;
  }

  public AuthData setClientToken(AuthTokenResponse clientToken) {
    this.clientToken = clientToken;
    return this;
  }
}
