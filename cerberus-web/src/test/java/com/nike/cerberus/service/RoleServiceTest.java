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

package com.nike.cerberus.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.nike.cerberus.dao.RoleDao;
import com.nike.cerberus.record.RoleRecord;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class RoleServiceTest {

  @Mock private RoleDao roleDao;

  @InjectMocks private RoleService roleService;

  @Before
  public void before() {
    initMocks(this);
  }

  @Test
  public void test_that_getRoleIdToStringMap_returns_valid_map() {
    Map<String, String> expected = new HashMap<>();
    expected.put("abc", "foo");

    when(roleDao.getAllRoles())
        .thenReturn(Arrays.asList(new RoleRecord().setId("abc").setName("foo")));

    Map<String, String> actual = roleService.getRoleIdToStringMap();
    assertEquals(expected, actual);
  }
}
