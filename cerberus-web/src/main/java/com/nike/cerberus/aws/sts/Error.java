/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.aws.sts;

/** POJO representing a get-caller-identity error. */
class Error {
  private String type;
  private String code;
  private String message;

  public String getType() {
    return type;
  }

  public Error setType(String type) {
    this.type = type;
    return this;
  }

  public String getCode() {
    return code;
  }

  public Error setCode(String code) {
    this.code = code;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public Error setMessage(String message) {
    this.message = message;
    return this;
  }
}
