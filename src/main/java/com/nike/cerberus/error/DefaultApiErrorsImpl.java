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

package com.nike.cerberus.error;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.sample.SampleProjectApiErrorsBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import static com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES;

/**
 * Returns the project specific errors for this application for the common jersey error
 * handling library.
 * <p>
 * Individual projects should feel free to rename this class to something specific
 * <p>
 * Created by dsand7 on 10/2/14.
 */
@Named
@Singleton
public class DefaultApiErrorsImpl extends SampleProjectApiErrorsBase {

    @Override
    protected List<ApiError> getProjectSpecificApiErrors() {
        final List<ApiError> errorList = new ArrayList<>();
        errorList.addAll(Arrays.asList(DefaultApiError.values()));
        return errorList;
    }

    @Override
    protected ProjectSpecificErrorCodeRange getProjectSpecificErrorCodeRange() {
        return ALLOW_ALL_ERROR_CODES;
    }

}
