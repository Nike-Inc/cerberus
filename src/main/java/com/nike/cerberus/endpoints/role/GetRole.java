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

package com.nike.cerberus.endpoints.role;

import com.nike.cerberus.domain.Role;
import com.nike.cerberus.endpoints.RiposteEndpoint;
import com.nike.cerberus.service.RoleService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.server.http.impl.FullResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Returns the role for the specified ID.
 */
@RiposteEndpoint
public class GetRole extends StandardEndpoint<Void, Role> {

    public static final String PATH_PARAM_ID = "id";

    private final RoleService roleService;

    @Inject
    public GetRole(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Role>> execute(RequestInfo<Void> request,
                                                         Executor longRunningTaskExecutor,
                                                         ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> getRole(request.getPathParam(PATH_PARAM_ID)), ctx),
                longRunningTaskExecutor
        );
    }

    public FullResponseInfo<Role> getRole(final String id) {
        final Optional<Role> role = roleService.getRoleById(id);

        if (role.isPresent()) {
            return ResponseInfo.newBuilder(role.get()).build();
        }

        return ResponseInfo.<Role>newBuilder().withHttpStatusCode(HttpResponseStatus.NOT_FOUND.code()).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/role/{id}", HttpMethod.GET);
    }
}
