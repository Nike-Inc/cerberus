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
import com.nike.backstopper.apierror.contract.jsr303convention.VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest;
import com.nike.backstopper.validation.constraints.StringConvertsToClassType;

/**
 * Makes sure that any Enums referenced by {@link StringConvertsToClassType} JSR 303 annotations are case insensitive if
 * they are marked with {@link StringConvertsToClassType#allowCaseInsensitiveEnumMatch()} set to true.
 *
 * <p>You can exclude annotation declarations (e.g. for unit test classes that are intended to violate the naming
 * convention) by making sure that the {@link ApplicationJsr303AnnotationTroller#ignoreAllAnnotationsAssociatedWithTheseProjectClasses()}
 * and {@link ApplicationJsr303AnnotationTroller#specificAnnotationDeclarationExclusionsForProject()} methods return
 * what you need, but you should not exclude any annotations in production code under normal circumstances.
 *
 * @author Nic Munroe
 */
public class VerifyStringConvertsToClassTypeAnnotationsAreValidTest
    extends VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest {

    @Override
    protected ReflectionBasedJsr303AnnotationTrollerBase getAnnotationTroller() {
        return ApplicationJsr303AnnotationTroller.getInstance();
    }
}
