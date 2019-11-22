/*
 * Copyright (c) 2019 Nike, Inc.
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

import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.nike.cerberus.record.RoleRecord.ROLE_OWNER;
import static com.nike.cerberus.record.RoleRecord.ROLE_WRITE;
import static com.nike.cerberus.record.RoleRecord.ROLE_READ;

/**
 * Maps an action against a path, to the role required for that action.
 */
public enum SecureDataAction {

    READ(ImmutableSet.of(ROLE_OWNER, ROLE_WRITE, ROLE_READ)),
    WRITE(ImmutableSet.of(ROLE_OWNER, ROLE_WRITE)),
    DELETE(ImmutableSet.of(ROLE_OWNER, ROLE_WRITE));

    private final ImmutableSet<String> allowedRoles;

    public ImmutableSet<String> getAllowedRoles() {
        return allowedRoles;
    }

    SecureDataAction(ImmutableSet<String> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public static SecureDataAction fromString(String actionAsString) {
        return Arrays.stream(values())
                .filter(e -> e.name().equals(actionAsString))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("The action: %s is not valid. Valid actions: %s",
                  actionAsString, Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", ")))));
    }
}
