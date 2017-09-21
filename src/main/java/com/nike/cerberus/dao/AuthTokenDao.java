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

package com.nike.cerberus.dao;

import com.nike.cerberus.mapper.AuthTokenMapper;
import com.nike.cerberus.record.AuthTokenRecord;

import javax.inject.Inject;

public class AuthTokenDao {

    private final AuthTokenMapper authTokenMapper;

    @Inject
    public AuthTokenDao(AuthTokenMapper authTokenMapper) {
        this.authTokenMapper = authTokenMapper;
    }

    public int createAuthToken(AuthTokenRecord record) {
        return authTokenMapper.createAuthToken(record);
    }

    public AuthTokenRecord getAuthTokenFromHash(String hash) {
        return authTokenMapper.getAuthTokenFromHash(hash);
    }
}
