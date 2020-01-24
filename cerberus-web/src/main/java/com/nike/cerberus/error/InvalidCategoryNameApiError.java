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

package com.nike.cerberus.error;

import com.nike.backstopper.apierror.ApiError;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

public class InvalidCategoryNameApiError implements ApiError {

  private final String categoryName;

  public InvalidCategoryNameApiError(String categoryName) {
    this.categoryName = categoryName;
  }

  @Override
  public String getName() {
    return "InvalidCategoryNameApiError";
  }

  @Override
  public String getErrorCode() {
    return DefaultApiError.ENTITY_NOT_FOUND.getErrorCode();
  }

  @Override
  public String getMessage() {
    return String.format(
        "The category %s could not be mapped to a valid category id", categoryName);
  }

  @Override
  public Map<String, Object> getMetadata() {
    return null;
  }

  @Override
  public int getHttpStatusCode() {
    return HttpServletResponse.SC_BAD_REQUEST;
  }
}
