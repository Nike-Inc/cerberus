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

package com.nike.cerberus.service;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.*;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.error.InvalidCategoryNameApiError;
import com.nike.cerberus.error.InvalidRoleNameApiError;
import com.nike.cerberus.util.UuidSupplier;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** A service that can perform admin tasks around SDB metadata */
@Component
public class MetadataService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final SafeDepositBoxService safeDepositBoxService;
  private final CategoryService categoryService;
  private final RoleService roleService;
  private final UuidSupplier uuidSupplier;

  @Autowired
  public MetadataService(
      SafeDepositBoxService safeDepositBoxService,
      CategoryService categoryService,
      RoleService roleService,
      UuidSupplier uuidSupplier) {

    this.safeDepositBoxService = safeDepositBoxService;
    this.categoryService = categoryService;
    this.roleService = roleService;
    this.uuidSupplier = uuidSupplier;
  }

  /**
   * Creates or Updates an SDB using saved off metadata. This method differs from
   * SafeDepositBoxService::createSafeDepositBoxV1 and SafeDepositBoxService::updateSafeDepositBoxV1
   * only in that this method sets the created by and last updated fields which are normally sourced
   * automatically.
   *
   * <p>This is an admin function so that backed up SDB metadata can easily be restored. An example
   * would be a cross region recovery event where you are restoring backed up data from a different
   * region / cerberus environment
   *
   * @param sdbMetadata SDB Payload to restore
   */
  public void restoreMetadata(SDBMetadata sdbMetadata, String adminUser) {
    logger.info("Restoring metadata for SDB: '{}'", sdbMetadata.getName());

    String id = getSdbId(sdbMetadata);
    String categoryId = getCategoryId(sdbMetadata);
    Set<UserGroupPermission> userGroupPermissionSet = getUserGroupPermissionSet(sdbMetadata);
    Set<IamPrincipalPermission> iamPrincipalPermissionSet =
        getIamPrincipalPermissionSet(sdbMetadata);

    SafeDepositBoxV2 sdb =
        SafeDepositBoxV2.builder()
            .id(id)
            .path(sdbMetadata.getPath())
            .categoryId(categoryId)
            .name(sdbMetadata.getName())
            .owner(sdbMetadata.getOwner())
            .description(sdbMetadata.getDescription())
            .createdTs(sdbMetadata.getCreatedTs())
            .lastUpdatedTs(sdbMetadata.getLastUpdatedTs())
            .createdBy(sdbMetadata.getCreatedBy())
            .lastUpdatedBy(sdbMetadata.getLastUpdatedBy())
            .userGroupPermissions(userGroupPermissionSet)
            .iamPrincipalPermissions(iamPrincipalPermissionSet)
            .build();

    safeDepositBoxService.restoreSafeDepositBox(sdb, adminUser);
  }

  /**
   * Retrieves the IAM Role Permission Set for SDB Metadata Object.
   *
   * @param sdbMetadata the sdb metadata
   * @return IAM Principal Permission Set
   */
  private Set<IamPrincipalPermission> getIamPrincipalPermissionSet(SDBMetadata sdbMetadata) {
    Set<IamPrincipalPermission> iamPrincipalPermissionSet = new HashSet<>();
    sdbMetadata
        .getIamRolePermissions()
        .forEach(
            (iamPrincipalArn, roleName) -> {
              iamPrincipalPermissionSet.add(
                  IamPrincipalPermission.builder()
                      .iamPrincipalArn(iamPrincipalArn.trim())
                      .roleId(getRoleIdFromName(roleName))
                      .build());
            });
    return iamPrincipalPermissionSet;
  }

  /**
   * Retrieves the User Group Permission Set for SDB Metadata Object.
   *
   * @param sdbMetadata the sdb metadata
   * @return User Group Permission Set
   */
  private Set<UserGroupPermission> getUserGroupPermissionSet(SDBMetadata sdbMetadata) {
    Set<UserGroupPermission> userGroupPermissionSet = new HashSet<>();
    sdbMetadata
        .getUserGroupPermissions()
        .forEach(
            (groupName, roleName) -> {
              userGroupPermissionSet.add(
                  UserGroupPermission.builder()
                      .name(groupName)
                      .roleId(getRoleIdFromName(roleName))
                      .build());
            });
    return userGroupPermissionSet;
  }

  /**
   * Retrieves or generates an ID for the safe deposit box.
   *
   * @param sdbMetadata the sdb metadata
   * @return id for the sdb
   */
  public String getSdbId(SDBMetadata sdbMetadata) {
    Optional<String> sdbId = safeDepositBoxService.getSafeDepositBoxIdByName(sdbMetadata.getName());
    String id;
    if (sdbId.isPresent()) {
      id = sdbId.get();

      logger.info(
          "Found existing SDB for {} with id {}, forcing restore", sdbMetadata.getName(), id);
    } else {
      // create
      id = uuidSupplier.get();
      logger.info("No SDB found for {}, creating new SDB", sdbMetadata.getName());
    }
    return id;
  }

  /**
   * Gets the role id for a role by its name
   *
   * @param roleName the name that you need an id for
   * @return the role id
   */
  private String getRoleIdFromName(String roleName) {
    // map the string role name to a role id
    Optional<Role> role = roleService.getRoleByName(roleName);
    if (!role.isPresent()) {
      throw ApiException.newBuilder().withApiErrors(new InvalidRoleNameApiError(roleName)).build();
    }
    return role.get().getId();
  }

  /**
   * Method for retrieving metadata about SDBs sorted by created date.
   *
   * @param limit The int limit for paginating.
   * @param offset The int offset for paginating.
   * @return SDBMetadataResult of meta data.
   */
  public SDBMetadataResult getSDBMetadata(int limit, int offset, String sdbNameFilter) {
    SDBMetadataResult result =
        SDBMetadataResult.builder()
            .limit(Optional.ofNullable(sdbNameFilter).map(it -> 1).orElse(limit))
            .offset(Optional.ofNullable(sdbNameFilter).map(it -> 1).orElse(offset))
            .totalSDBCount(
                Optional.ofNullable(sdbNameFilter)
                    .map(it -> 1)
                    .orElseGet(safeDepositBoxService::getTotalNumberOfSafeDepositBoxes))
            .build();
    result.setHasNext(result.getTotalSDBCount() > (offset + limit));
    if (result.isHasNext()) {
      result.setNextOffset(offset + limit);
    }
    List<SDBMetadata> sdbMetadataList = getSDBMetadataList(limit, offset, sdbNameFilter);
    result.setSafeDepositBoxMetadata(sdbMetadataList);
    result.setSdbCountInResult(sdbMetadataList.size());

    return result;
  }

  /**
   * Gets a list of SBD Metadata's
   *
   * @param limit The limit for the results
   * @param offset The offset for pagination
   * @return A list of SDB Metadata
   */
  protected List<SDBMetadata> getSDBMetadataList(int limit, int offset, String sdbNameFilter) {
    List<SDBMetadata> sdbs = new LinkedList<>();

    // Collect the categories.
    Map<String, String> catIdToStringMap = categoryService.getCategoryIdToCategoryNameMap();
    // Collect the roles
    Map<String, String> roleIdToStringMap = roleService.getRoleIdToStringMap();

    List<SafeDepositBoxV2> safeDepositBoxes =
        Optional.ofNullable(sdbNameFilter)
            .map(
                i ->
                    Collections.singletonList(
                        safeDepositBoxService
                            .getSafeDepositBoxDangerouslyWithoutPermissionValidation(
                                safeDepositBoxService
                                    .getSafeDepositBoxIdByName(sdbNameFilter)
                                    .orElseThrow(
                                        () ->
                                            ApiException.newBuilder()
                                                .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                                                .build()))))
            .orElseGet(() -> safeDepositBoxService.getSafeDepositBoxes(limit, offset));

    // for each SDB collect the user and iam permissions and add to result
    safeDepositBoxes.forEach(
        sdb -> {
          SDBMetadata data =
              SDBMetadata.builder()
                  .name(sdb.getName())
                  .id(sdb.getId())
                  .path(sdb.getPath())
                  .description(sdb.getDescription())
                  .category(catIdToStringMap.get(sdb.getCategoryId()))
                  .createdBy(sdb.getCreatedBy())
                  .createdTs(sdb.getCreatedTs())
                  .lastUpdatedBy(sdb.getLastUpdatedBy())
                  .lastUpdatedTs(sdb.getLastUpdatedTs())
                  .owner(sdb.getOwner())
                  .userGroupPermissions(
                      getUserGroupPermissionsMap(roleIdToStringMap, sdb.getUserGroupPermissions()))
                  .build();
          data.setIamRolePermissions(
              getIamPrincipalPermissionMap(roleIdToStringMap, sdb.getIamPrincipalPermissions()));
          sdbs.add(data);
        });

    return sdbs;
  }

  /**
   * Retrieves a simplified user group permission map that is only strings so it can be transported
   * across Cerberus environments
   */
  protected Map<String, String> getUserGroupPermissionsMap(
      Map<String, String> roleIdToStringMap, Set<UserGroupPermission> permissions) {

    Map<String, String> permissionsMap = new HashMap<>();
    permissions.forEach(
        permission ->
            permissionsMap.put(
                permission.getName(), roleIdToStringMap.get(permission.getRoleId())));

    return permissionsMap;
  }

  /**
   * Retrieves a simplified iam permission map that is only strings so it can be transported across
   * Cerberus environments
   */
  protected Map<String, String> getIamPrincipalPermissionMap(
      Map<String, String> roleIdToStringMap, Set<IamPrincipalPermission> iamPerms) {

    Map<String, String> iamPrincipalMap = new HashMap<>(iamPerms.size());
    iamPerms.forEach(
        perm -> {
          String role = roleIdToStringMap.get(perm.getRoleId());

          iamPrincipalMap.put(perm.getIamPrincipalArn(), role);
        });
    return iamPrincipalMap;
  }

  /** Gets the category id for a sdb */
  public String getCategoryId(SDBMetadata sdbMetadata) {
    // Map the string category name to a category id
    Optional<String> categoryOpt = categoryService.getCategoryIdByName(sdbMetadata.getCategory());
    if (!categoryOpt.isPresent()) {
      throw ApiException.newBuilder()
          .withApiErrors(new InvalidCategoryNameApiError(sdbMetadata.getCategory()))
          .build();
    }
    return categoryOpt.get();
  }
}
