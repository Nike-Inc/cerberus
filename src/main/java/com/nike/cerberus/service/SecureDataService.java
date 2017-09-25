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

package com.nike.cerberus.service;


import com.nike.cerberus.dao.SecureDataDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

public class SecureDataService {

    private final SecureDataDao secureDataDao;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public SecureDataService(SecureDataDao secureDataDao) {
        this.secureDataDao = secureDataDao;
    }

    public void writeSecret(String sdbId, String path, String plainTextPayload) {
        log.debug("Writing secure data: SDB ID: {}, Path: {}, Payload: {}", sdbId, path, plainTextPayload);

        String encryptedPayload = encrypt(plainTextPayload);

        secureDataDao.writeSecureData(sdbId, path, encryptedPayload);
    }

    public Object readSecret(String path) {
        log.debug("Reading secure data: Path: {}", path);
        return null;
    }

    public List<String> listKeys(String path) {
        return new LinkedList<>();
    }

    private String encrypt(String plainTextPayload) {
        log.error("ENCRYPT NOT IMPLEMENTED RETURNING PLAIN TEXT");
        return plainTextPayload;
    }

    private String decrypt(String encryptedPayload) {
        log.error("DECRYPT NOT IMPLEMENTED RETURNING PLAIN TEXT");
        return encryptedPayload;
    }

}
