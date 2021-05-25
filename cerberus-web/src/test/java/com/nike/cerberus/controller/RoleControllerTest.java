package com.nike.cerberus.controller;

import com.nike.cerberus.domain.Role;
import com.nike.cerberus.service.RoleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RoleControllerTest {

  @Mock private RoleService roleService;

  @InjectMocks private RoleController roleController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetCategoryWhenRoleIsNotPresentById() {
    Mockito.when(roleService.getRoleById("id")).thenReturn(Optional.empty());
    ResponseEntity<Role> categoryResponseEntity = roleController.getCategory("id");
    Assert.assertEquals(HttpStatus.NOT_FOUND, categoryResponseEntity.getStatusCode());
  }

  @Test
  public void testGetCategory() {
    Role role = Mockito.mock(Role.class);
    Mockito.when(roleService.getRoleById("id")).thenReturn(Optional.of(role));
    ResponseEntity<Role> categoryResponseEntity = roleController.getCategory("id");
    Assert.assertEquals(HttpStatus.OK, categoryResponseEntity.getStatusCode());
    Assert.assertSame(role, categoryResponseEntity.getBody());
  }

  @Test
  public void testListRoles() {
    List<Role> roles = new ArrayList<>();
    Role role = Mockito.mock(Role.class);
    roles.add(role);
    Mockito.when(roleService.getAllRoles()).thenReturn(roles);
    List<Role> actualRoles = roleController.listRoles();
    Assert.assertSame(roles, actualRoles);
    Assert.assertEquals(roles, actualRoles);
  }
}
