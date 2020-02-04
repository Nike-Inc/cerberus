/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.dao;

import com.nike.cerberus.mapper.RoleMapper;
import com.nike.cerberus.record.RoleRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Data access layer for the role data. */
@Component
public class RoleDao {

  private final RoleMapper roleMapper;

  @Autowired
  public RoleDao(final RoleMapper roleMapper) {
    this.roleMapper = roleMapper;
  }

  public List<RoleRecord> getAllRoles() {
    return roleMapper.getAllRoles();
  }

  public Optional<RoleRecord> getRoleById(final String id) {
    return Optional.ofNullable(roleMapper.getRoleById(id));
  }

  public Optional<RoleRecord> getRoleByName(final String name) {
    return Optional.ofNullable(roleMapper.getRoleByName(name));
  }
}
