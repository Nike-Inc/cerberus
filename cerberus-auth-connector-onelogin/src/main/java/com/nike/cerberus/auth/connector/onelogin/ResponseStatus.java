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

package com.nike.cerberus.auth.connector.onelogin;

/** POJO representing the response status for all API calls. */
class ResponseStatus {

  private String type;

  private String message;

  private long code;

  private boolean error;

  public String getType() {
    return type;
  }

  public ResponseStatus setType(String type) {
    this.type = type;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public ResponseStatus setMessage(String message) {
    this.message = message;
    return this;
  }

  public long getCode() {
    return code;
  }

  public ResponseStatus setCode(long code) {
    this.code = code;
    return this;
  }

  public boolean isError() {
    return error;
  }

  public ResponseStatus setError(boolean error) {
    this.error = error;
    return this;
  }
}
