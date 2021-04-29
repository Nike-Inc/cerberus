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

import com.nike.cerberus.domain.SafeDepositBoxV1;
import com.nike.cerberus.domain.SafeDepositBoxV2;
import com.nike.cerberus.domain.UserGroupPermission;
import java.util.HashSet;
import java.util.Set;
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
    SafeDepositBoxV1 safeDepositBox1 = SafeDepositBoxV1.builder().owner("   ").build();
    SafeDepositBoxV2 safeDepositBox2 = SafeDepositBoxV2.builder().owner("   ").build();
    Assert.assertTrue(subject.isValid(safeDepositBox1, mockConstraintValidatorContext));
    Assert.assertTrue(subject.isValid(safeDepositBox2, mockConstraintValidatorContext));
  }

  @Test
  public void empty_user_group_set_is_valid() {
    SafeDepositBoxV1 safeDepositBox1 = SafeDepositBoxV1.builder().owner("owner").build();
    SafeDepositBoxV2 safeDepositBox2 = SafeDepositBoxV2.builder().owner("owner").build();
    Assert.assertTrue(subject.isValid(safeDepositBox1, mockConstraintValidatorContext));
    Assert.assertTrue(subject.isValid(safeDepositBox2, mockConstraintValidatorContext));
  }

  @Test
  public void unique_owner_is_valid() {
    UserGroupPermission userGroupPermission = UserGroupPermission.builder().name("group").build();

    Set<UserGroupPermission> userGroupPermissions = new HashSet<>();
    userGroupPermissions.add(userGroupPermission);
    SafeDepositBoxV1 safeDepositBox1 =
        SafeDepositBoxV1.builder()
            .owner("owner")
            .userGroupPermissions(userGroupPermissions)
            .build();

    SafeDepositBoxV2 safeDepositBox2 =
        SafeDepositBoxV2.builder()
            .owner("owner")
            .userGroupPermissions(userGroupPermissions)
            .build();

    Assert.assertTrue(subject.isValid(safeDepositBox1, mockConstraintValidatorContext));
    Assert.assertTrue(subject.isValid(safeDepositBox2, mockConstraintValidatorContext));
  }

  @Test
  public void owner_in_group_permissions_is_invalid() {
    UserGroupPermission userGroupPermission = UserGroupPermission.builder().name("owner").build();
    userGroupPermission.setName("owner");

    Set<UserGroupPermission> userGroupPermissions = new HashSet<>();
    userGroupPermissions.add(userGroupPermission);
    SafeDepositBoxV1 safeDepositBox1 =
        SafeDepositBoxV1.builder()
            .owner("owner")
            .userGroupPermissions(userGroupPermissions)
            .build();

    SafeDepositBoxV2 safeDepositBox2 =
        SafeDepositBoxV2.builder()
            .owner("owner")
            .userGroupPermissions(userGroupPermissions)
            .build();

    Assert.assertFalse(subject.isValid(safeDepositBox1, mockConstraintValidatorContext));
    Assert.assertFalse(subject.isValid(safeDepositBox2, mockConstraintValidatorContext));
  }
}
