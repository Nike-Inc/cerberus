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

package com.nike.cerberus.service;

import com.google.common.collect.Lists;
import com.nike.cerberus.dao.RoleDao;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.record.RoleRecord;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Business logic for interacting with roles */
@Component
public class RoleService {

  private final RoleDao roleDao;

  @Autowired
  public RoleService(final RoleDao roleDao) {
    this.roleDao = roleDao;
  }

  /**
   * Retrieves all roles from the data store and returns them.
   *
   * @return List of role domain objects.
   */
  public List<Role> getAllRoles() {
    final List<RoleRecord> roleRecords = roleDao.getAllRoles();
    final List<Role> roles = Lists.newArrayListWithCapacity(roleRecords.size());

    roleRecords.forEach(
        r ->
            roles.add(
                Role.builder()
                    .id(r.getId())
                    .name(r.getName())
                    .createdBy(r.getCreatedBy())
                    .lastUpdatedBy(r.getLastUpdatedBy())
                    .createdTs(r.getCreatedTs())
                    .lastUpdatedTs(r.getLastUpdatedTs())
                    .build()));

    return roles;
  }

  /**
   * Retrieves the specific role by ID.
   *
   * @param id The identifier for the role to retrieve.
   * @return The role, if it exists.
   */
  public Optional<Role> getRoleById(final String id) {
    final Optional<RoleRecord> record = roleDao.getRoleById(id);

    if (record.isPresent()) {
      return Optional.of(
          Role.builder()
              .id(record.get().getId())
              .name(record.get().getName())
              .createdBy(record.get().getCreatedBy())
              .lastUpdatedBy(record.get().getLastUpdatedBy())
              .createdTs(record.get().getCreatedTs())
              .lastUpdatedTs(record.get().getLastUpdatedTs())
              .build());
    }

    return Optional.empty();
  }

  /**
   * Retrieves the specific role by name.
   *
   * @param name The name of the role to retrieve.
   * @return The role, if it exists.
   */
  public Optional<Role> getRoleByName(final String name) {
    final Optional<RoleRecord> record = roleDao.getRoleByName(name);

    if (record.isPresent()) {
      return Optional.of(
          Role.builder()
              .id(record.get().getId())
              .name(record.get().getName())
              .createdBy(record.get().getCreatedBy())
              .lastUpdatedBy(record.get().getLastUpdatedBy())
              .createdTs(record.get().getCreatedTs())
              .lastUpdatedTs(record.get().getLastUpdatedTs())
              .build());
    }

    return Optional.empty();
  }

  public Map<String, String> getRoleIdToStringMap() {
    List<RoleRecord> roleRecords = roleDao.getAllRoles();
    Map<String, String> roleIdToStringMap = new HashMap<>(roleRecords.size());
    roleRecords.forEach(
        roleRecord -> roleIdToStringMap.put(roleRecord.getId(), roleRecord.getName()));
    return roleIdToStringMap;
  }
}
