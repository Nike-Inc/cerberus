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

/** POJO representing the payload of a get user response. */
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

  private List<String> roleId = new LinkedList<>();

  private Map<String, String> customAttributes = new HashMap<>();

  public OffsetDateTime getActivatedAt() {
    return activatedAt;
  }

  public UserData setActivatedAt(OffsetDateTime activatedAt) {
    this.activatedAt = activatedAt;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public UserData setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public UserData setEmail(String email) {
    this.email = email;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public UserData setUsername(String username) {
    this.username = username;
    return this;
  }

  public String getFirstname() {
    return firstname;
  }

  public UserData setFirstname(String firstname) {
    this.firstname = firstname;
    return this;
  }

  public long getGroupId() {
    return groupId;
  }

  public UserData setGroupId(long groupId) {
    this.groupId = groupId;
    return this;
  }

  public long getId() {
    return id;
  }

  public UserData setId(long id) {
    this.id = id;
    return this;
  }

  public long getInvalidLoginAttempts() {
    return invalidLoginAttempts;
  }

  public UserData setInvalidLoginAttempts(long invalidLoginAttempts) {
    this.invalidLoginAttempts = invalidLoginAttempts;
    return this;
  }

  public OffsetDateTime getInvitationSentAt() {
    return invitationSentAt;
  }

  public UserData setInvitationSentAt(OffsetDateTime invitationSentAt) {
    this.invitationSentAt = invitationSentAt;
    return this;
  }

  public OffsetDateTime getLastLogin() {
    return lastLogin;
  }

  public UserData setLastLogin(OffsetDateTime lastLogin) {
    this.lastLogin = lastLogin;
    return this;
  }

  public String getLastname() {
    return lastname;
  }

  public UserData setLastname(String lastname) {
    this.lastname = lastname;
    return this;
  }

  public OffsetDateTime getLockedUntil() {
    return lockedUntil;
  }

  public UserData setLockedUntil(OffsetDateTime lockedUntil) {
    this.lockedUntil = lockedUntil;
    return this;
  }

  public String getNotes() {
    return notes;
  }

  public UserData setNotes(String notes) {
    this.notes = notes;
    return this;
  }

  public String getOpenidName() {
    return openidName;
  }

  public UserData setOpenidName(String openidName) {
    this.openidName = openidName;
    return this;
  }

  public OffsetDateTime getPasswordChangedAt() {
    return passwordChangedAt;
  }

  public UserData setPasswordChangedAt(OffsetDateTime passwordChangedAt) {
    this.passwordChangedAt = passwordChangedAt;
    return this;
  }

  public String getPhone() {
    return phone;
  }

  public UserData setPhone(String phone) {
    this.phone = phone;
    return this;
  }

  public long getStatus() {
    return status;
  }

  public UserData setStatus(long status) {
    this.status = status;
    return this;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public UserData setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getDistinguishedName() {
    return distinguishedName;
  }

  public UserData setDistinguishedName(String distinguishedName) {
    this.distinguishedName = distinguishedName;
    return this;
  }

  public String getExternalId() {
    return externalId;
  }

  public UserData setExternalId(String externalId) {
    this.externalId = externalId;
    return this;
  }

  public String getDirectoryId() {
    return directoryId;
  }

  public UserData setDirectoryId(String directoryId) {
    this.directoryId = directoryId;
    return this;
  }

  public String getMemberOf() {
    return memberOf;
  }

  public UserData setMemberOf(String memberOf) {
    this.memberOf = memberOf;
    return this;
  }

  public String getSamaccountname() {
    return samaccountname;
  }

  public UserData setSamaccountname(String samaccountname) {
    this.samaccountname = samaccountname;
    return this;
  }

  public String getUserprincipalname() {
    return userprincipalname;
  }

  public UserData setUserprincipalname(String userprincipalname) {
    this.userprincipalname = userprincipalname;
    return this;
  }

  public String getManagerAdId() {
    return managerAdId;
  }

  public UserData setManagerAdId(String managerAdId) {
    this.managerAdId = managerAdId;
    return this;
  }

  public List<String> getRoleId() {
    return roleId;
  }

  public UserData setRoleId(List<String> roleId) {
    this.roleId = roleId;
    return this;
  }

  public Map<String, String> getCustomAttributes() {
    return customAttributes;
  }

  public UserData setCustomAttributes(Map<String, String> customAttributes) {
    this.customAttributes = customAttributes;
    return this;
  }
}
