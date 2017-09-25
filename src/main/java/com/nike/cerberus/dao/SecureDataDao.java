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

import com.nike.cerberus.record.SecureDataRecord;
import com.nike.cerberus.mapper.SecureDataMapper;

import javax.inject.Inject;

public class SecureDataDao {

    private final SecureDataMapper secureDataMapper;

    @Inject
    public SecureDataDao(SecureDataMapper secureDataMapper) {
        this.secureDataMapper = secureDataMapper;
    }

    public void writeSecureData(String sdbId, String path, String encryptedPayload) {
        secureDataMapper.writeSecureData(
                new SecureDataRecord.SecureDataRecordBuilder()
                .withId(path.hashCode())
                .withPath(path)
                .withSdbId(sdbId)
                .withEncryptedBlob(encryptedPayload)
                .build()
        );
    }
}
