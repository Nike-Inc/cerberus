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

import com.nike.cerberus.domain.UserGroupPermission;
import java.util.HashSet;
import javax.validation.ConstraintValidatorContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

/** Tests the UserGroupPermissionsValidator class */
public class UserGroupPermissionsValidatorTest {

  private ConstraintValidatorContext mockConstraintValidatorContext;

  private UserGroupPermissionsValidator subject;

  @Before
  public void setup() {
    mockConstraintValidatorContext = mock(ConstraintValidatorContext.class);
    subject = new UserGroupPermissionsValidator();
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
    UserGroupPermission a = new UserGroupPermission();
    a.setName("abc");
    UserGroupPermission b = new UserGroupPermission();
    b.setName("def");

    Assert.assertTrue(subject.isValid(Sets.newSet(a, b), mockConstraintValidatorContext));
  }
}
