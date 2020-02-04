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

package com.nike.cerberus.controller;

import static com.nike.cerberus.security.CerberusPrincipal.ROLE_USER;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.nike.cerberus.domain.Role;
import com.nike.cerberus.service.RoleService;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/role")
public class RoleController {

  private final RoleService roleService;

  @Autowired
  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  @RolesAllowed(ROLE_USER)
  @RequestMapping(value = "/{roleId:.+}", method = GET)
  public ResponseEntity<Role> getCategory(@PathVariable String roleId) {
    return ResponseEntity.of(roleService.getRoleById(roleId));
  }

  @RolesAllowed(ROLE_USER)
  @RequestMapping(method = GET)
  public List<Role> listRoles() {
    return roleService.getAllRoles();
  }
}
