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

/** POJO representing the payload of a verify factor request. */
class VerifyFactorRequest {

  private String deviceId;

  private String stateToken;

  private String otpToken;

  public String getDeviceId() {
    return deviceId;
  }

  public VerifyFactorRequest setDeviceId(String deviceId) {
    this.deviceId = deviceId;
    return this;
  }

  public String getStateToken() {
    return stateToken;
  }

  public VerifyFactorRequest setStateToken(String stateToken) {
    this.stateToken = stateToken;
    return this;
  }

  public String getOtpToken() {
    return otpToken;
  }

  public VerifyFactorRequest setOtpToken(String otpToken) {
    this.otpToken = otpToken;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VerifyFactorRequest that = (VerifyFactorRequest) o;

    if (deviceId != null ? !deviceId.equals(that.deviceId) : that.deviceId != null) return false;
    if (stateToken != null ? !stateToken.equals(that.stateToken) : that.stateToken != null)
      return false;
    return otpToken != null ? otpToken.equals(that.otpToken) : that.otpToken == null;
  }

  @Override
  public int hashCode() {
    int result = deviceId != null ? deviceId.hashCode() : 0;
    result = 31 * result + (stateToken != null ? stateToken.hashCode() : 0);
    result = 31 * result + (otpToken != null ? otpToken.hashCode() : 0);
    return result;
  }
}
