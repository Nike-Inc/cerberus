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

package com.nike.cerberus.validation;

import static org.mockito.Mockito.mock;

import com.nike.cerberus.domain.IamRolePermission;
import java.util.HashSet;
import javax.validation.ConstraintValidatorContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

/** Tests the IamRolePermissionsValidator class */
public class IamRolePermissionsValidatorTest {

  private ConstraintValidatorContext mockConstraintValidatorContext;

  private IamRolePermissionsValidator subject;

  @Before
  public void setup() {
    mockConstraintValidatorContext = mock(ConstraintValidatorContext.class);
    subject = new IamRolePermissionsValidator();
  }

  @Test
  public void null_set_is_valid() {
    Assert.assertTrue(subject.isValid(null, mockConstraintValidatorContext));
  }

  @Test
  public void empty_set_is_valid() {
    Assert.assertTrue(subject.isValid(new HashSet<>(), mockConstraintValidatorContext));
  }

  @Test
  public void unique_set_is_valid() {
    IamRolePermission a = IamRolePermission.builder().accountId("123").iamRoleName("abc").build();
    IamRolePermission b = IamRolePermission.builder().accountId("123").iamRoleName("def").build();

    Assert.assertTrue(subject.isValid(Sets.newSet(a, b), mockConstraintValidatorContext));
  }

  @Test
  public void duplicate_set_is_invalid() {
    IamRolePermission a = IamRolePermission.builder().accountId("123").iamRoleName("abc").build();
    IamRolePermission b = IamRolePermission.builder().accountId("123").iamRoleName("ABC").build();

    Assert.assertFalse(subject.isValid(Sets.newSet(a, b), mockConstraintValidatorContext));
  }
}
