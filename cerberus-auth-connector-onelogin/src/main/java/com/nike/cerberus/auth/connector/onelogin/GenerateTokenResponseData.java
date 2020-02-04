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

import java.time.OffsetDateTime;

/** POJO representing the payload of a generate token response. */
class GenerateTokenResponseData {

  private String accessToken;

  private OffsetDateTime createdAt;

  private int expiresIn;

  private String refreshToken;

  private String tokenType;

  private long accountId;

  public String getAccessToken() {
    return accessToken;
  }

  public GenerateTokenResponseData setAccessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public GenerateTokenResponseData setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public int getExpiresIn() {
    return expiresIn;
  }

  public GenerateTokenResponseData setExpiresIn(int expiresIn) {
    this.expiresIn = expiresIn;
    return this;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public GenerateTokenResponseData setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
    return this;
  }

  public String getTokenType() {
    return tokenType;
  }

  public GenerateTokenResponseData setTokenType(String tokenType) {
    this.tokenType = tokenType;
    return this;
  }

  public long getAccountId() {
    return accountId;
  }

  public GenerateTokenResponseData setAccountId(long accountId) {
    this.accountId = accountId;
    return this;
  }
}
