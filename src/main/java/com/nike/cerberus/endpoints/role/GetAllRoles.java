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
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Returns all known roles.
 */
@RiposteEndpoint
public class GetAllRoles extends StandardEndpoint<Void, List<Role>> {

    private final RoleService roleService;

    @Inject
    public GetAllRoles(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public CompletableFuture<ResponseInfo<List<Role>>> execute(final RequestInfo<Void> request,
                                                               final Executor longRunningTaskExecutor,
                                                               final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> ResponseInfo.newBuilder(roleService.getAllRoles()).build(), ctx),
                longRunningTaskExecutor
        );
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/role", HttpMethod.GET);
    }
}
