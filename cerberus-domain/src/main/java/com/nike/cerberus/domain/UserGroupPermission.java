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

import com.nike.cerberus.validation.group.Updatable;
import java.time.OffsetDateTime;
import javax.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;

/** Represents a permission granted to a user group with regards to a safe deposit box. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserGroupPermission {

  private String id;

  @NotBlank(
      message = "USER_GROUP_NAME_BLANK",
      groups = {Default.class, Updatable.class})
  private String name;

  @NotBlank(
      message = "USER_GROUP_ROLE_ID_INVALID",
      groups = {Default.class, Updatable.class})
  private String roleId;

  private OffsetDateTime createdTs;

  private OffsetDateTime lastUpdatedTs;

  private String createdBy;

  private String lastUpdatedBy;
}
