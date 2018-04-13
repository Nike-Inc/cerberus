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

package com.nike.cerberus.mapper;

import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.record.SecureDataRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface SecureDataMapper {

    int writeSecureData(@Param("record") SecureDataRecord record);

    int updateSecureData(@Param("record") SecureDataRecord record);

    SecureDataRecord readSecureDataByPath(@Param("path") String path);

    SecureDataRecord readSecureDataByPathAndType(@Param("path") String path, @Param("type") SecureDataType type);

    SecureDataRecord readMetadataByPathAndType(@Param("path") String path, @Param("type") SecureDataType type);

    String[] getPathsByPartialPath(@Param("partialPath") String partialPath);

    String[] getPathsByPartialPathAndType(@Param("partialPath") String partialPath, @Param("type") SecureDataType type);

    List<SecureDataRecord> listSecureDataByPartialPathAndType(
            @Param("partialPath") String partialPath,
            @Param("type") SecureDataType type,
            @Param("limit") int limit,
            @Param("offset") int offset);

    int countByType(@Param("type") SecureDataType type);

    Set<String> getPathsBySdbId(@Param("sdbId") String sdbId);

    int getTotalNumberOfDataNodes();

    int deleteAllSecretsThatStartWithGivenPartialPath(@Param("partialPath") String partialPath);

    int deleteSecret(@Param("path") String path);

    Integer getSumTopLevelKeyValuePairs();
}
