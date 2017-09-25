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

package com.nike.cerberus.record;

public class SecureDataRecord {

    private Integer id;
    private String sdbId;
    private String path;
    private String encryptedBlob;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSdbId() {
        return sdbId;
    }

    public void setSdbId(String sdbId) {
        this.sdbId = sdbId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getEncryptedBlob() {
        return encryptedBlob;
    }

    public void setEncryptedBlob(String encryptedBlob) {
        this.encryptedBlob = encryptedBlob;
    }

    public static final class SecureDataRecordBuilder {
        private Integer id;
        private String sdbId;
        private String path;
        private String encryptedBlob;

        public SecureDataRecordBuilder() {
        }

        public SecureDataRecordBuilder withId(Integer id) {
            this.id = id;
            return this;
        }

        public SecureDataRecordBuilder withSdbId(String sdbId) {
            this.sdbId = sdbId;
            return this;
        }

        public SecureDataRecordBuilder withPath(String path) {
            this.path = path;
            return this;
        }

        public SecureDataRecordBuilder withEncryptedBlob(String encryptedBlob) {
            this.encryptedBlob = encryptedBlob;
            return this;
        }

        public SecureDataRecord build() {
            SecureDataRecord secureDataRecord = new SecureDataRecord();
            secureDataRecord.encryptedBlob = this.encryptedBlob;
            secureDataRecord.id = this.id;
            secureDataRecord.sdbId = this.sdbId;
            secureDataRecord.path = this.path;
            return secureDataRecord;
        }
    }
}
