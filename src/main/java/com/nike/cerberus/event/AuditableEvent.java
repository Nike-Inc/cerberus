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

package com.nike.cerberus.event;

import io.netty.handler.codec.http.HttpMethod;

/**
 * An event that can be used to describe what a principal is doing with the API
 */
public class AuditableEvent implements Event {

    private Object principal;
    private String ipAddress;
    private String xForwardedFor;
    private String clientVersion;
    private HttpMethod method;
    private String path;
    private String action;
    private String name;
    private String originatingClass;

    @Override
    public String getEventAsString() {
        return new StringBuilder("Event: ").append(name).append(", ")
                .append("Principal: ").append(principal == null ? "Unknown" : principal).append(", ")
                .append("IP Address: ").append(ipAddress).append(", ")
                .append("X-Forwarded-For: ").append(xForwardedFor).append(", ")
                .append("Client Version: ").append(clientVersion).append(", ")
                .append("Method: ").append(method).append(", ")
                .append("Path: ").append(path).append(", ")
                .append("Action: ").append('\'').append(action).append("\', ")
                .append("Originating Class: ").append(originatingClass)
                .toString();
    }


    public static final class Builder {
        private Object principal;
        private String ipAddress;
        private String xForwardedFor;
        private String clientVersion;
        private HttpMethod method;
        private String path;
        private String action;
        private String name;
        private String originatingClass;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder withPrincipal(Object principal) {
            this.principal = principal;
            return this;
        }

        public Builder withIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder withXForwardedFor(String xForwardedFor) {
            this.xForwardedFor = xForwardedFor;
            return this;
        }

        public Builder withClientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        public Builder withMethod(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder withPath(String path) {
            this.path = path;
            return this;
        }

        public Builder withAction(String action) {
            this.action = action;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withOriginatingClass(String originatingClass) {
            this.originatingClass = originatingClass;
            return this;
        }

        public AuditableEvent build() {
            AuditableEvent auditableEvent = new AuditableEvent();
            auditableEvent.action = this.action;
            auditableEvent.name = this.name;
            auditableEvent.clientVersion = this.clientVersion;
            auditableEvent.ipAddress = this.ipAddress;
            auditableEvent.method = this.method;
            auditableEvent.principal = this.principal;
            auditableEvent.originatingClass = this.originatingClass;
            auditableEvent.path = this.path;
            auditableEvent.xForwardedFor = this.xForwardedFor;
            return auditableEvent;
        }
    }
}
