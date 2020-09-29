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

package com.nike.cerberus.util;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.cerberus.error.DefaultApiError;
import java.util.UUID;

public final class CustomApiError {

  public static ApiError createCustomApiError(DefaultApiError error, final String message) {
    return new ApiErrorBase(
        "custom-error-wrapper-" + UUID.randomUUID().toString(),
        error.getErrorCode(),
        error.getMessage() + " " + message,
        error.getHttpStatusCode());
  }
}
