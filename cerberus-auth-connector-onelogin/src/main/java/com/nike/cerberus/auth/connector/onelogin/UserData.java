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

package com.nike.cerberus.auth.connector.onelogin;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** POJO representing the payload of a get user response. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UserData {

  private OffsetDateTime activatedAt;

  private OffsetDateTime createdAt;

  private String email;

  private String username;

  private String firstname;

  private long groupId;

  private long id;

  private long invalidLoginAttempts;

  private OffsetDateTime invitationSentAt;

  private OffsetDateTime lastLogin;

  private String lastname;

  private OffsetDateTime lockedUntil;

  private String notes;

  private String openidName;

  private OffsetDateTime passwordChangedAt;

  private String phone;

  private long status;

  private OffsetDateTime updatedAt;

  private String distinguishedName;

  private String externalId;

  private String directoryId;

  private String memberOf;

  private String samaccountname;

  private String userprincipalname;

  private String managerAdId;

  @Builder.Default private List<String> roleId = new LinkedList<>();
  @Builder.Default private Map<String, String> customAttributes = new HashMap<>();
}
