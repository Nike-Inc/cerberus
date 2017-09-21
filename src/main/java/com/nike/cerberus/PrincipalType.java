/*
 * Copyright (c) 2017 Nike, Inc.
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

package com.nike.cerberus;

import com.google.common.base.Preconditions;

import java.util.Arrays;

public enum PrincipalType {
    USER("USER"),
    IAM("IAM");

    private final String name;

    public String getName() {
        return name;
    }

    PrincipalType(String name) {
        this.name = name;
    }

    public static PrincipalType fromName(String name) {
        PrincipalType type = Arrays.stream(values())
                .filter(bl -> bl.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        Preconditions.checkNotNull(type, "Invalid principal type");
        return type;
    }
}
