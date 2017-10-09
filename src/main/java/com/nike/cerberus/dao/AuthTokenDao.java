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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Optional;

public class AuthTokenDao {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AuthTokenMapper authTokenMapper;

    @Inject
    public AuthTokenDao(AuthTokenMapper authTokenMapper) {
        this.authTokenMapper = authTokenMapper;
    }

    public int createAuthToken(AuthTokenRecord record) {
        return authTokenMapper.createAuthToken(record);
    }

    public Optional<AuthTokenRecord> getAuthTokenFromHash(String hash) {
        return Optional.ofNullable(authTokenMapper.getAuthTokenFromHash(hash));
    }

    public void deleteAuthTokenFromHash(String hash) {
        authTokenMapper.deleteAuthTokenFromHash(hash);
    }

    public int deleteExpiredTokens(int maxDelete, int batchSize, int batchPauseTimeInMillis) {
        int numberOfDeletedTokens = 0;
        int cur;
        do {
            cur = authTokenMapper.deleteExpiredTokens(batchSize);
            logger.info("Deleted {} tokens in this batch {} so far", cur, numberOfDeletedTokens);
            numberOfDeletedTokens += cur;
            if (cur > 0 && numberOfDeletedTokens < maxDelete && batchPauseTimeInMillis > 0) {
                try {
                    Thread.sleep(batchPauseTimeInMillis);
                } catch (InterruptedException e) {
                    logger.error("Failed to sleep between delete batches", e);
                }
            }
        } while (cur > 0 && numberOfDeletedTokens < maxDelete);
        return numberOfDeletedTokens;
    }
}
