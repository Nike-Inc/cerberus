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

package com.nike.cerberus.domain;

import com.nike.cerberus.validation.UniqueIamRolePermissions;
import com.nike.cerberus.validation.UniqueOwner;
import com.nike.cerberus.validation.UniqueUserGroupPermissions;
import com.nike.cerberus.validation.group.Updatable;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.groups.Default;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/** Represents a logical grouping of secrets. */
@UniqueOwner(groups = {Default.class, Updatable.class})
@Data
@Builder
public class SafeDepositBoxV1 implements SafeDepositBox {

  private String id;

  @NotBlank(message = "SDB_CATEGORY_ID_INVALID")
  private String categoryId;

  @NotBlank(message = "SDB_NAME_BLANK")
  @Length(max = 100, message = "SDB_NAME_TOO_LONG")
  private String name;

  @Length(
      max = 1000,
      message = "SDB_DESCRIPTION_TOO_LONG",
      groups = {Default.class, Updatable.class})
  private String description;

  private String path;

  private OffsetDateTime createdTs;

  private OffsetDateTime lastUpdatedTs;

  private String createdBy;

  private String lastUpdatedBy;

  @NotBlank(
      message = "SDB_OWNER_BLANK",
      groups = {Default.class, Updatable.class})
  @Length(
      max = 255,
      message = "SDB_OWNER_TOO_LONG",
      groups = {Default.class, Updatable.class})
  private String owner;

  @Valid
  @UniqueUserGroupPermissions(groups = {Default.class, Updatable.class})
  @Builder.Default
  private Set<UserGroupPermission> userGroupPermissions = new HashSet<>();

  @Valid
  @UniqueIamRolePermissions(groups = {Default.class, Updatable.class})
  @Builder.Default
  private Set<IamRolePermission> iamRolePermissions = new HashSet<>();
}
