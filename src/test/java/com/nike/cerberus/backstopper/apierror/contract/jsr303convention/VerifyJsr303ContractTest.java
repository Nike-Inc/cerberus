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

package com.nike.cerberus.backstopper.apierror.contract.jsr303convention;

import com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase;
import com.nike.backstopper.apierror.contract.jsr303convention.VerifyJsr303ValidationMessagesPointToApiErrorsTest;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.cerberus.error.DefaultApiErrorsImpl;

/**
 * Verifies that *ALL* non-excluded JSR 303 validation annotations in this project have a message defined that maps to a
 * {@link com.nike.backstopper.apierror.ApiError} enum name from this project's {@link ProjectApiErrors}. This is how
 * the JSR 303 Bean Validation system is connected to the Backstopper error handling system and you should NOT disable
 * these tests.
 *
 * <p>You can exclude annotation declarations (e.g. for unit test classes that are intended to violate the naming
 * convention) by making sure that the {@link ApplicationJsr303AnnotationTroller#ignoreAllAnnotationsAssociatedWithTheseProjectClasses()}
 * and {@link ApplicationJsr303AnnotationTroller#specificAnnotationDeclarationExclusionsForProject()} methods return
 * what you need, but you should not exclude any annotations in production code under normal circumstances.
 *
 * @author Nic Munroe
 */
public class VerifyJsr303ContractTest extends VerifyJsr303ValidationMessagesPointToApiErrorsTest {

    private static final ProjectApiErrors PROJECT_API_ERRORS = new DefaultApiErrorsImpl();

    @Override
    protected ReflectionBasedJsr303AnnotationTrollerBase getAnnotationTroller() {
        return ApplicationJsr303AnnotationTroller.getInstance();
    }

    @Override
    protected ProjectApiErrors getProjectApiErrors() {
        return PROJECT_API_ERRORS;
    }
}
