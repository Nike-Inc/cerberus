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

import static com.nike.backstopper.apierror.ApiErrorConstants.*;
import static com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES;
import static javax.servlet.http.HttpServletResponse.SC_NOT_IMPLEMENTED;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.sample.SampleProjectApiErrorsBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** TODO remove dependency on SampleProjectApiErrors, have everything be in this project */
public class DefaultApiErrorsImpl extends SampleProjectApiErrorsBase {

  public static final List<Integer> CERBERUS_STATUS_CODE_PRIORITY_ORDER = Arrays.asList(
          HTTP_STATUS_CODE_FORBIDDEN, HTTP_STATUS_CODE_UNAUTHORIZED, HTTP_STATUS_CODE_SERVICE_UNAVAILABLE,
          HTTP_STATUS_CODE_TOO_MANY_REQUESTS, HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR, HTTP_STATUS_CODE_METHOD_NOT_ALLOWED,
          HTTP_STATUS_CODE_NOT_ACCEPTABLE, HTTP_STATUS_CODE_UNSUPPORTED_MEDIA_TYPE, HTTP_STATUS_CODE_NOT_FOUND,
          HTTP_STATUS_CODE_CONFLICT,
          HTTP_STATUS_CODE_BAD_REQUEST, SC_NOT_IMPLEMENTED);

  @Override
  protected List<ApiError> getProjectSpecificApiErrors() {
    return new ArrayList<>(Arrays.asList(DefaultApiError.values()));
  }

  @Override
  protected ProjectSpecificErrorCodeRange getProjectSpecificErrorCodeRange() {
    return ALLOW_ALL_ERROR_CODES;
  }

  @Override
  public ApiError getForbiddenApiError() {
    return DefaultApiError.ACCESS_DENIED;
  }

  @Override
  public List<Integer> getStatusCodePriorityOrder() { return CERBERUS_STATUS_CODE_PRIORITY_ORDER; }
}
