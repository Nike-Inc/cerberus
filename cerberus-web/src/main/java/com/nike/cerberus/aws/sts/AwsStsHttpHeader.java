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

package com.nike.cerberus.aws.sts;

import com.amazonaws.regions.Regions;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.util.CustomApiError;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** POJO representing AWS Signature Version 4 headers */
public final class AwsStsHttpHeader {

  public static final String HEADER_X_AMZ_DATE = "x-amz-date";
  public static final String HEADER_X_AMZ_SECURITY_TOKEN = "x-amz-security-token";
  public static final String HEADER_AUTHORIZATION = "Authorization";

  private String amzDate;
  private String amzSecurityToken;
  private String authorization;

  public AwsStsHttpHeader(String amzDate, String amzSecurityToken, String authorization) {
    Preconditions.checkNotNull(amzDate);
    Preconditions.checkNotNull(authorization);

    this.amzDate = amzDate;
    this.amzSecurityToken = amzSecurityToken;
    this.authorization = authorization;
  }

  public Map<String, String> generateHeaders() {
    Map<String, String> headers = Maps.newHashMap();
    headers.put(HEADER_AUTHORIZATION, authorization);
    headers.put(HEADER_X_AMZ_DATE, amzDate);
    if (amzSecurityToken != null) {
      headers.put(HEADER_X_AMZ_SECURITY_TOKEN, amzSecurityToken);
    }
    return headers;
  }

  public String getRegion() {
    Pattern pattern = Pattern.compile(".*Credential=.*?/\\d+/(?<region>.*?)/.*");
    Matcher matcher = pattern.matcher(authorization);
    boolean didMatch = matcher.matches();

    if (!didMatch) {
      String msg = "Failed to determine region from header.";
      throw ApiException.newBuilder()
          .withApiErrors(
              CustomApiError.createCustomApiError(DefaultApiError.GENERIC_BAD_REQUEST, msg))
          .withExceptionMessage(
              String.format("Failed to determine region from header %s.", authorization))
          .build();
    }

    String region = matcher.group("region");

    try {
      //noinspection ResultOfMethodCallIgnored
      Regions.fromName(region);
    } catch (IllegalArgumentException e) {
      String msg = String.format("Invalid region supplied %s.", region);
      throw ApiException.newBuilder()
          .withApiErrors(
              CustomApiError.createCustomApiError(DefaultApiError.GENERIC_BAD_REQUEST, msg))
          .withExceptionMessage(msg)
          .build();
    }

    return region;
  }

  public String getAmzDate() {
    return amzDate;
  }

  public AwsStsHttpHeader setAmzDate(String amzDate) {
    this.amzDate = amzDate;
    return this;
  }

  public String getAmzSecurityToken() {
    return amzSecurityToken;
  }

  public AwsStsHttpHeader setAmzSecurityToken(String amzSecurityToken) {
    this.amzSecurityToken = amzSecurityToken;
    return this;
  }

  public String getAuthorization() {
    return authorization;
  }

  public AwsStsHttpHeader setAuthorization(String authorization) {
    this.authorization = authorization;
    return this;
  }
}
