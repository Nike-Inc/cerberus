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
import com.google.common.collect.ImmutableSet;
import io.netty.handler.codec.http.HttpMethod;

import java.util.Arrays;

import static com.nike.cerberus.record.RoleRecord.ROLE_OWNER;
import static com.nike.cerberus.record.RoleRecord.ROLE_READ;
import static com.nike.cerberus.record.RoleRecord.ROLE_WRITE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;

public enum SecureDataAction {

    READ(GET, ImmutableSet.of(ROLE_OWNER, ROLE_WRITE, ROLE_READ)),
    WRITE(POST, ImmutableSet.of(ROLE_OWNER, ROLE_WRITE)),
    DELETE(HttpMethod.DELETE, ImmutableSet.of(ROLE_OWNER, ROLE_WRITE));

    private final HttpMethod method;

    private final ImmutableSet<String> allowedRoles;

    public HttpMethod getMethod() {
        return method;
    }

    public ImmutableSet<String> getAllowedRoles() {
        return allowedRoles;
    }

    SecureDataAction(HttpMethod method, ImmutableSet<String> allowedRoles) {
        this.method = method;
        this.allowedRoles = allowedRoles;
    }

    public static SecureDataAction fromMethod(HttpMethod method) {
        SecureDataAction action = Arrays.stream(values())
                .filter(e -> e.method.equals(method))
                .findFirst()
                .orElse(null);

        Preconditions.checkNotNull(action, String.format("Method: %s is not associated with an secure data action", method.name()));
        return action;
    }
}
