/*
 * Copyright (c) 2021 Nike, Inc.
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

import com.nike.cerberus.mapper.JwtBlocklistMapper;
import com.nike.cerberus.record.JwtBlocklistRecord;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtBlocklistDao {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final JwtBlocklistMapper jwtBlocklistMapper;

  @Autowired
  public JwtBlocklistDao(JwtBlocklistMapper jwtBlocklistMapper) {
    this.jwtBlocklistMapper = jwtBlocklistMapper;
  }

  public HashSet<String> getBlocklist() {
    return jwtBlocklistMapper.getBlocklist();
  }

  public int addToBlocklist(JwtBlocklistRecord jwtBlocklistRecord) {
    return jwtBlocklistMapper.addToBlocklist(jwtBlocklistRecord);
  }

  public int deleteExpiredTokens() {
    return jwtBlocklistMapper.deleteExpiredTokens();
  }
}
