/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.validation;

import static org.mockito.Mockito.mock;

import com.nike.cerberus.domain.SafeDepositBoxV1;
import com.nike.cerberus.domain.SafeDepositBoxV2;
import com.nike.cerberus.domain.UserGroupPermission;
import javax.validation.ConstraintValidatorContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** Tests the UniqueOwnerValidator class */
public class UniqueOwnerValidatorTest {

  private ConstraintValidatorContext mockConstraintValidatorContext;

  private UniqueOwnerValidator subject;

  @Before
  public void setup() {
    mockConstraintValidatorContext = mock(ConstraintValidatorContext.class);
    subject = new UniqueOwnerValidator();
  }

  @Test
  public void blank_owner_returns_valid() {
    SafeDepositBoxV1 safeDepositBox1 = new SafeDepositBoxV1();
    safeDepositBox1.setOwner("   ");
    SafeDepositBoxV2 safeDepositBox2 = new SafeDepositBoxV2();
    safeDepositBox2.setOwner("   ");
    Assert.assertTrue(subject.isValid(safeDepositBox1, mockConstraintValidatorContext));
    Assert.assertTrue(subject.isValid(safeDepositBox2, mockConstraintValidatorContext));
  }

  @Test
  public void empty_user_group_set_is_valid() {
    SafeDepositBoxV1 safeDepositBox1 = new SafeDepositBoxV1();
    safeDepositBox1.setOwner("owner");
    SafeDepositBoxV2 safeDepositBox2 = new SafeDepositBoxV2();
    safeDepositBox2.setOwner("owner");
    Assert.assertTrue(subject.isValid(safeDepositBox1, mockConstraintValidatorContext));
    Assert.assertTrue(subject.isValid(safeDepositBox2, mockConstraintValidatorContext));
  }

  @Test
  public void unique_owner_is_valid() {
    UserGroupPermission userGroupPermission = new UserGroupPermission();
    userGroupPermission.setName("group");

    SafeDepositBoxV1 safeDepositBox1 = new SafeDepositBoxV1();
    safeDepositBox1.setOwner("owner");
    safeDepositBox1.getUserGroupPermissions().add(userGroupPermission);

    SafeDepositBoxV2 safeDepositBox2 = new SafeDepositBoxV2();
    safeDepositBox2.setOwner("owner");
    safeDepositBox2.getUserGroupPermissions().add(userGroupPermission);

    Assert.assertTrue(subject.isValid(safeDepositBox1, mockConstraintValidatorContext));
    Assert.assertTrue(subject.isValid(safeDepositBox2, mockConstraintValidatorContext));
  }

  @Test
  public void owner_in_group_permissions_is_invalid() {
    UserGroupPermission userGroupPermission = new UserGroupPermission();
    userGroupPermission.setName("owner");

    SafeDepositBoxV1 safeDepositBox1 = new SafeDepositBoxV1();
    safeDepositBox1.setOwner("owner");
    safeDepositBox1.getUserGroupPermissions().add(userGroupPermission);

    SafeDepositBoxV2 safeDepositBox2 = new SafeDepositBoxV2();
    safeDepositBox2.setOwner("owner");
    safeDepositBox2.getUserGroupPermissions().add(userGroupPermission);

    Assert.assertFalse(subject.isValid(safeDepositBox1, mockConstraintValidatorContext));
    Assert.assertFalse(subject.isValid(safeDepositBox2, mockConstraintValidatorContext));
  }
}
