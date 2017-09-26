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
import com.nike.cerberus.record.SecureDataRecord;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SecureDataService {

    private final SecureDataDao secureDataDao;
    private final EncryptionService encryptionService;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public SecureDataService(SecureDataDao secureDataDao,
                             EncryptionService encryptionService) {
        this.secureDataDao = secureDataDao;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public void writeSecret(String sdbId, String path, String plainTextPayload) {
        log.debug("Writing secure data: SDB ID: {}, Path: {}, Payload: {}", sdbId, path, plainTextPayload);

        String encryptedPayload = encryptionService.encrypt(plainTextPayload);

        secureDataDao.writeSecureData(sdbId, path, encryptedPayload);
    }

    public String readSecret(String path) {
        log.debug("Reading secure data: Path: {}", path);
        Optional<SecureDataRecord> secureDataRecord = secureDataDao.readSecureDataByPath(path);
        if (! secureDataRecord.isPresent()) {
            // TODO
            log.error("NOT FOUND");
            return null;
        }

        String encryptedBlob = secureDataRecord.get().getEncryptedBlob();
        String plainText = encryptionService.decrypt(encryptedBlob);

        return plainText;
    }

    public Set<String> listKeys(String partialPath) {
        Set<String> keys = new HashSet<>();
        String[] pArray = secureDataDao.getPathsByPartialPath(partialPath);
        if (pArray == null || pArray.length < 1) {
            return keys;
        }

        for (int i = 0; i < pArray.length; i++) {
            String fullPath = pArray[i];
            String res = StringUtils.removeStart(fullPath, partialPath);

        }

        return keys;
    }

}
