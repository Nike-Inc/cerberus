/*
 * Copyright (c) 2018 Nike, Inc.
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
 *
 */

package com.nike.cerberus.mapper;

import com.nike.cerberus.record.SecureDataVersionRecord;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public interface SecureDataVersionMapper {

    int writeSecureDataVersion(@Param("record") SecureDataVersionRecord record);

    int updateSecureDataVersion(@Param("record") SecureDataVersionRecord record);

    Integer getTotalNumVersionsForPath(String path);

    List<SecureDataVersionRecord> listSecureDataVersionsByPath(@Param("path") String path,
                                                               @Param("limit") int limit,
                                                               @Param("offset") int offset);

    SecureDataVersionRecord readSecureDataVersionById(@Param("id") String id);

    String[] getVersionPathsByPartialPath(@Param("partialPath") String partialPath);

    Set<String> getVersionPathsBySdbId(@Param("sdbId") String sdbId);

    int deleteAllVersionsThatStartWithPartialPath(@Param("partialPath") String partialPath);

    List<SecureDataVersionRecord> getOldestSecureDataVersion(@Param("datetime") OffsetDateTime dateTime,
                                               @Param("limit") int limit);
}
