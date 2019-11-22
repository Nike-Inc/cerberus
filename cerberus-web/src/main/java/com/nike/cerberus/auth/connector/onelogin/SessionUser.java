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

package com.nike.cerberus.auth.connector.onelogin;

/**
 * POJO representing the session user.
 */
class SessionUser {

    private long id;

    private String email;

    private String username;

    private String firstname;

    private String lastname;

    public long getId() {
        return id;
    }

    public SessionUser setId(long id) {
        this.id = id;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public SessionUser setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public SessionUser setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getFirstname() {
        return firstname;
    }

    public SessionUser setFirstname(String firstname) {
        this.firstname = firstname;
        return this;
    }

    public String getLastname() {
        return lastname;
    }

    public SessionUser setLastname(String lastname) {
        this.lastname = lastname;
        return this;
    }
}
