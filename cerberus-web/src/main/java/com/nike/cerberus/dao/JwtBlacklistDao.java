/*
 * Copyright (c) 2019 Nike, Inc.
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

import com.nike.cerberus.mapper.JwtBlacklistMapper;
import com.nike.cerberus.record.JwtBlacklistRecord;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtBlacklistDao {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final JwtBlacklistMapper jwtBlacklistMapper;

  @Autowired
  public JwtBlacklistDao(JwtBlacklistMapper jwtBlacklistMapper) {
    this.jwtBlacklistMapper = jwtBlacklistMapper;
  }

  public HashSet<String> getBlacklist() {
    return jwtBlacklistMapper.getBlacklist();
  }

  public int addToBlacklist(JwtBlacklistRecord jwtBlacklistRecord) {
    return jwtBlacklistMapper.addToBlacklist(jwtBlacklistRecord);
  }

  public int deleteExpiredTokens() {
    return jwtBlacklistMapper.deleteExpiredTokens();
  }
}
