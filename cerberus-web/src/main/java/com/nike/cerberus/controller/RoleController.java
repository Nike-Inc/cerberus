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
