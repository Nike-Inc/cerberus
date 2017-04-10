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

package com.nike.cerberus.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.dao.SafeDepositBoxDao;
import com.nike.cerberus.dao.UserGroupDao;
import com.nike.cerberus.domain.Category;
import com.nike.cerberus.domain.IamRolePermissionV1;
import com.nike.cerberus.domain.IamRolePermissionV2;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.domain.SafeDepositBoxV1;
import com.nike.cerberus.domain.SafeDepositBoxSummary;
import com.nike.cerberus.domain.SafeDepositBoxV2;
import com.nike.cerberus.domain.UserGroupPermission;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.RoleRecord;
import com.nike.cerberus.record.SafeDepositBoxRecord;
import com.nike.cerberus.record.UserGroupRecord;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.UuidSupplier;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.model.VaultListResponse;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for interacting with safe deposit boxes.
 */
@Singleton
public class SafeDepositBoxService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SafeDepositBoxDao safeDepositBoxDao;

    private final UserGroupDao userGroupDao;

    private final UuidSupplier uuidSupplier;

    private final CategoryService categoryService;

    private final RoleService roleService;

    private final VaultAdminClient vaultAdminClient;

    private final VaultPolicyService vaultPolicyService;

    private final UserGroupPermissionService userGroupPermissionService;

    private final IamRolePermissionService iamRolePermissionService;

    private final Slugger slugger;

    private final DateTimeSupplier dateTimeSupplier;

    private final AwsIamRoleArnParser awsIamRoleArnParser;

    @Inject
    public SafeDepositBoxService(final SafeDepositBoxDao safeDepositBoxDao,
                                 final UserGroupDao userGroupDao,
                                 final UuidSupplier uuidSupplier,
                                 final CategoryService categoryService,
                                 final RoleService roleService,
                                 final VaultAdminClient vaultAdminClient,
                                 final VaultPolicyService vaultPolicyService,
                                 final UserGroupPermissionService userGroupPermissionService,
                                 final IamRolePermissionService iamRolePermissionService,
                                 final Slugger slugger,
                                 final DateTimeSupplier dateTimeSupplier,
                                 final AwsIamRoleArnParser awsIamRoleArnParser) {
        this.safeDepositBoxDao = safeDepositBoxDao;
        this.userGroupDao = userGroupDao;
        this.uuidSupplier = uuidSupplier;
        this.categoryService = categoryService;
        this.roleService = roleService;
        this.vaultAdminClient = vaultAdminClient;
        this.vaultPolicyService = vaultPolicyService;
        this.userGroupPermissionService = userGroupPermissionService;
        this.iamRolePermissionService = iamRolePermissionService;
        this.slugger = slugger;
        this.dateTimeSupplier = dateTimeSupplier;
        this.awsIamRoleArnParser = awsIamRoleArnParser;
    }

    /**
     * Queries the data store for all safe deposit box associated with the user groups supplied.
     *
     * @param userGroups Set of user groups to find associated safe deposit boxes with
     * @return Collection of summaries for each associated safe deposit box
     */
    public List<SafeDepositBoxSummary> getAssociatedSafeDepositBoxes(final Set<String> userGroups) {
        final List<SafeDepositBoxRecord> records = safeDepositBoxDao.getUserAssociatedSafeDepositBoxes(userGroups);
        final List<SafeDepositBoxSummary> summaries = Lists.newArrayListWithCapacity(records.size());

        records.forEach(r -> {
            summaries.add(new SafeDepositBoxSummary()
                    .setId(r.getId())
                    .setName(r.getName())
                    .setCategoryId(r.getCategoryId())
                    .setPath(r.getPath()));
        });

        return summaries;
    }

    /**
     * Queries the data store for the specific safe deposit box by ID.  The query also enforces that the specified
     * safe deposit box has a linked permission via the user groups supplied in the call.
     *
     * @param groups Set of user groups that must have at least one matching permission for the specific safe
     *               deposit box
     * @param id The unique identifier for the safe deposit box to lookup
     * @return The safe deposit box, if found
     */
    public Optional<SafeDepositBoxV1> getAssociatedSafeDepositBoxV1(final Set<String> groups, final String id) {

        Optional<SafeDepositBoxV2> safeDepositBoxV2 = getAssociatedSafeDepositBoxV2(groups, id);

        return safeDepositBoxV2.map(this::convertSafeDepositBoxV2ToV1);
    }

    /**
     * Queries the data store for the specific safe deposit box by ID.  The query also enforces that the specified
     * safe deposit box has a linked permission via the user groups supplied in the call.
     *
     * @param groups Set of user groups that must have at least one matching permission for the specific safe
     *               deposit box
     * @param id The unique identifier for the safe deposit box to lookup
     * @return The safe deposit box, if found
     */
    public Optional<SafeDepositBoxV2> getAssociatedSafeDepositBoxV2(final Set<String> groups, final String id) {

        final Optional<SafeDepositBoxRecord> safeDepositBoxRecord = safeDepositBoxDao.getSafeDepositBox(id);

        if (safeDepositBoxRecord.isPresent()) {
            final Set<UserGroupPermission> userGroupPermissions = userGroupPermissionService.getUserGroupPermissions(id);

            final long count = userGroupPermissions.stream().filter(perm -> groups.contains(perm.getName())).count();
            if (count == 0) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.ACCESS_DENIED)
                        .build();
            }

            return Optional.of(getSDBFromRecordV2(safeDepositBoxRecord.get()));
        }

        return Optional.empty();
    }

    protected SafeDepositBoxV1 getSDBFromRecordV1(SafeDepositBoxRecord safeDepositBoxRecord) {

        SafeDepositBoxV2 safeDepositBoxV2 = getSDBFromRecordV2(safeDepositBoxRecord);

        return convertSafeDepositBoxV2ToV1(safeDepositBoxV2);
    }

    protected SafeDepositBoxV2 getSDBFromRecordV2(SafeDepositBoxRecord safeDepositBoxRecord) {
        if (safeDepositBoxRecord == null) {
            throw new IllegalArgumentException("Safe Deposit Box Record must not be null");
        }

        String id = safeDepositBoxRecord.getId();

        final Set<UserGroupPermission> userGroupPermissions =
                userGroupPermissionService.getUserGroupPermissions(id);

        String owner = null;
        final Optional<String> possibleOwner = extractOwner(userGroupPermissions);

        if (!possibleOwner.isPresent()) {
            logger.error("Detected Safe Deposit Box without owner! ID={}", id);
        } else {
            owner = possibleOwner.get();
        }

        final Set<IamRolePermissionV2> iamRolePermissions = iamRolePermissionService.getIamRolePermissions(id);


        SafeDepositBoxV2 safeDepositBox = new SafeDepositBoxV2();
        safeDepositBox.setId(safeDepositBoxRecord.getId());
        safeDepositBox.setName(safeDepositBoxRecord.getName());
        safeDepositBox.setDescription(safeDepositBoxRecord.getDescription());
        safeDepositBox.setPath(safeDepositBoxRecord.getPath());
        safeDepositBox.setCategoryId(safeDepositBoxRecord.getCategoryId());
        safeDepositBox.setCreatedBy(safeDepositBoxRecord.getCreatedBy());
        safeDepositBox.setLastUpdatedBy(safeDepositBoxRecord.getLastUpdatedBy());
        safeDepositBox.setCreatedTs(safeDepositBoxRecord.getCreatedTs());
        safeDepositBox.setLastUpdatedTs(safeDepositBoxRecord.getLastUpdatedTs());
        safeDepositBox.setOwner(owner);
        safeDepositBox.setUserGroupPermissions(userGroupPermissions);
        safeDepositBox.setIamRolePermissions(iamRolePermissions);

        return safeDepositBox;
    }
    
    /**
     * Creates a safe deposit box and all the appropriate permissions.  Policies for each role are also
     * created within Vault.
     *
     * @param safeDepositBox Safe deposit box to create
     * @param user User requesting the creation
     * @return ID of the created safe deposit box
     */
    @Transactional
    public String createSafeDepositBoxV1(final SafeDepositBoxV1 safeDepositBox, final String user) {

        SafeDepositBoxV2 safeDepositBoxV2 = convertSafeDepositBoxV1ToV2(safeDepositBox);

        return createSafeDepositBoxV2(safeDepositBoxV2, user);
    }

    /**
     * Creates a safe deposit box and all the appropriate permissions.  Policies for each role are also
     * created within Vault.
     *
     * @param safeDepositBox Safe deposit box to create
     * @param user User requesting the creation
     * @return ID of the created safe deposit box
     */
    @Transactional
    public String createSafeDepositBoxV2(final SafeDepositBoxV2 safeDepositBox, final String user) {
        final OffsetDateTime now = dateTimeSupplier.get();
        final SafeDepositBoxRecord boxRecordToStore = buildBoxToStore(safeDepositBox, user, now);
        final Set<UserGroupPermission> userGroupPermissionSet = safeDepositBox.getUserGroupPermissions();
        addOwnerPermission(userGroupPermissionSet, safeDepositBox.getOwner());

        final Set<IamRolePermissionV2> iamRolePermissionSet = safeDepositBox.getIamRolePermissions();

        final boolean isPathInUse = safeDepositBoxDao.isPathInUse(boxRecordToStore.getPath());

        if (isPathInUse) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SDB_UNIQUE_NAME)
                    .build();
        }

        safeDepositBoxDao.createSafeDepositBox(boxRecordToStore);

        userGroupPermissionService.grantUserGroupPermissions(
                boxRecordToStore.getId(),
                userGroupPermissionSet,
                user,
                now);

        iamRolePermissionService.grantIamRolePermissions(
                boxRecordToStore.getId(),
                iamRolePermissionSet,
                user,
                now);

        vaultPolicyService.createStandardPolicies(boxRecordToStore.getName(), boxRecordToStore.getPath());

        return boxRecordToStore.getId();
    }

    /**
     * Updates a safe deposit box.  Currently, only the description, owner and permissions are updatable.
     *
     * @param safeDepositBox Updated safe deposit box
     * @param groups Caller's user groups
     * @param user Caller's username
     * @param id Safe deposit box id
     */
    @Transactional
    public void updateSafeDepositBoxV1(final SafeDepositBoxV1 safeDepositBox, final Set<String> groups,
                                       final String user, final String id) {

        SafeDepositBoxV2 safeDepositBoxV2 = convertSafeDepositBoxV1ToV2(safeDepositBox);

        updateSafeDepositBoxV2(safeDepositBoxV2, groups, user, id);
    }

    /**
     * Updates a safe deposit box.  Currently, only the description, owner and permissions are updatable.
     *
     * @param safeDepositBox Updated safe deposit box
     * @param groups Caller's user groups
     * @param user Caller's username
     * @param id Safe deposit box id
     */
    @Transactional
    public void updateSafeDepositBoxV2(final SafeDepositBoxV2 safeDepositBox, final Set<String> groups,
                                       final String user, final String id) {
        final Optional<SafeDepositBoxV2> currentBox = getAssociatedSafeDepositBoxV2(groups, id);

        if (!currentBox.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                    .withExceptionMessage("The specified safe deposit box was not found.")
                    .build();
        }

        assertIsOwner(groups, currentBox.get());

        final OffsetDateTime now = dateTimeSupplier.get();
        final SafeDepositBoxRecord boxToUpdate = buildBoxToUpdate(id, safeDepositBox, user, now);
        final Set<UserGroupPermission> userGroupPermissionSet = safeDepositBox.getUserGroupPermissions();
        final Set<IamRolePermissionV2> iamRolePermissionSet = safeDepositBox.getIamRolePermissions();

        if (!StringUtils.equals(currentBox.get().getDescription(), boxToUpdate.getDescription())) {
            safeDepositBoxDao.updateSafeDepositBox(boxToUpdate);
        }

        updateOwner(currentBox.get().getId(), safeDepositBox.getOwner(), user, now);
        modifyUserGroupPermissions(currentBox.get(), userGroupPermissionSet, user, now);
        modifyIamRolePermissions(currentBox.get(), iamRolePermissionSet, user, now);
    }

    /**
     * Deletes a safe deposit box and associated permissions.  Also removes the policies and secrets from Vault.
     *
     * @param id The unique identifier for the safe deposit box
     */
    @Transactional
    public void deleteSafeDepositBox(final Set<String> groups, final String id) {
        final Optional<SafeDepositBoxV2> box = getAssociatedSafeDepositBoxV2(groups, id);

        if (!box.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                    .build();
        }

        assertIsOwner(groups, box.get());

        // 1. Remove permissions and metadata from database.
        iamRolePermissionService.deleteIamRolePermissions(id);
        userGroupPermissionService.deleteUserGroupPermissions(id);
        safeDepositBoxDao.deleteSafeDepositBox(id);

        // 2. Recursively delete all secrets from the safe deposit box Vault path.
        deleteAllSecrets(box.get().getPath());

        // 3. Delete the standard policies from Vault for this safe deposit box.
        vaultPolicyService.deleteStandardPolicies(box.get().getName());
    }

    private Optional<String> extractOwner(Set<UserGroupPermission> userGroupPermissions) {
        final Optional<Role> ownerRole = roleService.getRoleByName(RoleRecord.ROLE_OWNER);

        if (!ownerRole.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.MISCONFIGURED_APP)
                    .withExceptionMessage("Owner role doesn't exist!")
                    .build();
        }

        final Optional<UserGroupPermission> ownerPermission = userGroupPermissions.stream().filter(perm -> StringUtils.equals(perm.getRoleId(), ownerRole.get().getId())).findFirst();

        if (!ownerPermission.isPresent()) {
            return Optional.empty();
        }

        userGroupPermissions.remove(ownerPermission.get());
        return Optional.of(ownerPermission.get().getName());
    }

    private void assertIsOwner(final Set<String> groups, final SafeDepositBoxV2 box) {
        if (!groups.contains(box.getOwner())) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SDB_CALLER_OWNERSHIP_REQUIRED)
                    .build();
        }
    }

    /**
     * Adds the owner identified in the request to the set of user group permissions.
     *
     * @param userGroupPermissionSet Set to add the owner to
     * @param owner Owner to be added
     */
    protected void addOwnerPermission(final Set<UserGroupPermission> userGroupPermissionSet, final String owner) {
        UserGroupPermission ownerPermission = new UserGroupPermission();
        ownerPermission.setId(uuidSupplier.get());
        ownerPermission.setName(owner);

        Optional<Role> ownerRole = roleService.getRoleByName(RoleRecord.ROLE_OWNER);

        if (ownerRole.isPresent()) {
            ownerPermission.setRoleId(ownerRole.get().getId());
        } else {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.USER_GROUP_ROLE_ID_INVALID)
                    .build();
        }

        userGroupPermissionSet.add(ownerPermission);
    }

    /**
     * Creates the safe deposit box object to be written to the data store.
     *
     * @param requestedBox Box to copy from
     * @param user User requesting the creation
     * @param dateTime The timestamp for the creation
     * @return The safe deposit box to be stored
     */
    private SafeDepositBoxRecord buildBoxToStore(final SafeDepositBoxV2 requestedBox,
                                           final String user,
                                           final OffsetDateTime dateTime) {
        final SafeDepositBoxRecord boxToStore = new SafeDepositBoxRecord();

        final Optional<Category> category = categoryService.getCategory(requestedBox.getCategoryId());

        if (category.isPresent()) {
            boxToStore.setPath(buildPath(requestedBox.getName(), category.get().getPath()));
        } else {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SDB_CATEGORY_ID_INVALID)
                    .build();
        }

        boxToStore.setId(uuidSupplier.get());
        boxToStore.setCategoryId(requestedBox.getCategoryId());
        boxToStore.setName(requestedBox.getName());
        boxToStore.setDescription(requestedBox.getDescription());
        boxToStore.setCreatedTs(dateTime);
        boxToStore.setLastUpdatedTs(dateTime);
        boxToStore.setCreatedBy(user);
        boxToStore.setLastUpdatedBy(user);
        return boxToStore;
    }

    /**
     * Copies the updatable fields to a new safe deposit box.
     *
     * @param safeDepositBox The safe deposit box to copy from
     * @param user The user requesting the change
     * @param now The date of the change
     * @return Safe deposit box with only updatable data
     */
    private SafeDepositBoxRecord buildBoxToUpdate(final String id,
                                                  final SafeDepositBoxV2 safeDepositBox,
                                                  final String user,
                                                  final OffsetDateTime now) {
        final SafeDepositBoxRecord boxToUpdate = new SafeDepositBoxRecord();
        boxToUpdate.setId(id);
        boxToUpdate.setDescription(safeDepositBox.getDescription());
        boxToUpdate.setLastUpdatedBy(user);
        boxToUpdate.setLastUpdatedTs(now);

        return boxToUpdate;
    }

    /**
     * Builds the path to be stored with the safe deposit box.
     *
     * @param name The box's name
     * @param categoryPath The category path
     * @return The formatted path
     */
    private String buildPath(final String name, final String categoryPath) {
        return categoryPath + "/" + slugger.toSlug(name) + "/";
    }

    /**
     * Updates the owner if its changed.
     */
    protected void updateOwner(final String safeDepositBoxId,
                             final String newOwner,
                             final String user,
                             final OffsetDateTime dateTime) {
        final Optional<Role> ownerRole = roleService.getRoleByName(RoleRecord.ROLE_OWNER);

        if (!ownerRole.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.MISCONFIGURED_APP)
                    .withExceptionMessage("Owner role doesn't exist!")
                    .build();
        }

        final List<UserGroupRecord> userGroupOwnerRecords =
                userGroupDao.getUserGroupsByRole(safeDepositBoxId, ownerRole.get().getId());

        if (userGroupOwnerRecords.size() != 1) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SDB_TOO_MANY_OWNERS)
                    .withExceptionMessage("SDB has more than one owner!")
                    .build();
        }

        if (!StringUtils.equals(userGroupOwnerRecords.get(0).getName(), newOwner)) {
            UserGroupPermission oldOwnerPermission = new UserGroupPermission();
            oldOwnerPermission.setName(userGroupOwnerRecords.get(0).getName());
            oldOwnerPermission.setRoleId(ownerRole.get().getId());

            UserGroupPermission newOwnerPermission = new UserGroupPermission();
            newOwnerPermission.setName(newOwner);
            newOwnerPermission.setRoleId(ownerRole.get().getId());

            userGroupPermissionService.grantUserGroupPermission(safeDepositBoxId, newOwnerPermission, user, dateTime);
            userGroupPermissionService.revokeUserGroupPermission(safeDepositBoxId, oldOwnerPermission, user, dateTime);
        }
    }

    /**
     * Sorts out the set of permissions into, grant, update and revoke sets.  After that it applies those changes.
     */
    protected void modifyUserGroupPermissions(final SafeDepositBoxV2 currentBox,
                                            final Set<UserGroupPermission> userGroupPermissionSet,
                                            final String user,
                                            final OffsetDateTime dateTime) {
        Set<UserGroupPermission> toAddSet = Sets.newHashSet();
        Set<UserGroupPermission> toUpdateSet = Sets.newHashSet();
        Set<UserGroupPermission> toDeleteSet = Sets.newHashSet();

        for (UserGroupPermission userGroupPermission : userGroupPermissionSet) {
            if (currentBox.getUserGroupPermissions().contains(userGroupPermission)) {
                toUpdateSet.add(userGroupPermission);
            } else {
                toAddSet.add(userGroupPermission);
            }
        }

        toDeleteSet.addAll(currentBox.getUserGroupPermissions().stream()
                .filter(userGroupPermission -> !userGroupPermissionSet.contains(userGroupPermission))
                .collect(Collectors.toList()));

        final String safeDepositBoxId = currentBox.getId();
        userGroupPermissionService.grantUserGroupPermissions(safeDepositBoxId, toAddSet, user, dateTime);
        userGroupPermissionService.updateUserGroupPermissions(safeDepositBoxId, toUpdateSet, user, dateTime);
        userGroupPermissionService.revokeUserGroupPermissions(safeDepositBoxId, toDeleteSet, user, dateTime);
    }

    /**
     * Sorts out the set of permissions into, grant, update and revoke sets.  After that it applies those changes.
     */
    protected void modifyIamRolePermissions(final SafeDepositBoxV2 currentBox,
                                          final Set<IamRolePermissionV2> iamRolePermissionSet,
                                          final String user,
                                          final OffsetDateTime dateTime) {
        Set<IamRolePermissionV2> toAddSet = Sets.newHashSet();
        Set<IamRolePermissionV2> toUpdateSet = Sets.newHashSet();
        Set<IamRolePermissionV2> toDeleteSet = Sets.newHashSet();

        for (IamRolePermissionV2 iamRolePermission : iamRolePermissionSet) {
            if (currentBox.getIamRolePermissions().contains(iamRolePermission)) {
                toUpdateSet.add(iamRolePermission);
            } else {
                toAddSet.add(iamRolePermission);
            }
        }

        toDeleteSet.addAll(currentBox.getIamRolePermissions().stream()
                .filter(iamRolePermission -> !iamRolePermissionSet.contains(iamRolePermission))
                .collect(Collectors.toList()));

        final String safeDepositBoxId = currentBox.getId();

        iamRolePermissionService.grantIamRolePermissions(safeDepositBoxId, toAddSet, user, dateTime);
        iamRolePermissionService.updateIamRolePermissions(safeDepositBoxId, toUpdateSet, user, dateTime);
        iamRolePermissionService.revokeIamRolePermissions(safeDepositBoxId, toDeleteSet, user, dateTime);
    }

    /**
     * Deletes all of the secrets from Vault stored at the safe deposit box's path.
     *
     * @param path path to start deleting at.
     */
    private void deleteAllSecrets(final String path) {
        try {
            String fixedPath = path;

            if (StringUtils.endsWith(path, "/")) {
                fixedPath = StringUtils.substring(path, 0, StringUtils.lastIndexOf(path, "/"));
            }

            final VaultListResponse listResponse = vaultAdminClient.list(fixedPath);
            final List<String> keys = listResponse.getKeys();

            if (keys == null || keys.isEmpty()) {
                return;
            }

            for (final String key : keys) {
                if (StringUtils.endsWith(key, "/")) {
                    final String fixedKey = StringUtils.substring(key, 0, key.lastIndexOf("/"));
                    deleteAllSecrets(fixedPath + "/" + fixedKey);
                } else {
                    vaultAdminClient.delete(fixedPath + "/" + key);
                }
            }
        }  catch (VaultClientException vce) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionCause(vce)
                    .withExceptionMessage("Failed to delete secrets from Vault.")
                    .build();
        }
    }

    /**
     * Converts a v2 API version safe deposit box into a v1 version
     * @param safeDepositBoxV2 - V2 API version safe deposit box
     * @return - V1 API version safe deposit box
     */
    protected SafeDepositBoxV1 convertSafeDepositBoxV2ToV1(SafeDepositBoxV2 safeDepositBoxV2) {

        final SafeDepositBoxV1 safeDepositBoxV1 = new SafeDepositBoxV1();
        safeDepositBoxV1.setId(safeDepositBoxV2.getId());
        safeDepositBoxV1.setName(safeDepositBoxV2.getName());
        safeDepositBoxV1.setDescription(safeDepositBoxV2.getDescription());
        safeDepositBoxV1.setPath(safeDepositBoxV2.getPath());
        safeDepositBoxV1.setCategoryId(safeDepositBoxV2.getCategoryId());
        safeDepositBoxV1.setCreatedBy(safeDepositBoxV2.getCreatedBy());
        safeDepositBoxV1.setLastUpdatedBy(safeDepositBoxV2.getLastUpdatedBy());
        safeDepositBoxV1.setCreatedTs(safeDepositBoxV2.getCreatedTs());
        safeDepositBoxV1.setLastUpdatedTs(safeDepositBoxV2.getLastUpdatedTs());
        safeDepositBoxV1.setOwner(safeDepositBoxV2.getOwner());
        safeDepositBoxV1.setUserGroupPermissions(safeDepositBoxV2.getUserGroupPermissions());
        safeDepositBoxV1.setIamRolePermissions(safeDepositBoxV2.getIamRolePermissions().stream()
                .map(iamRolePermission -> new IamRolePermissionV1()
                        .withAccountId(awsIamRoleArnParser.getAccountId(iamRolePermission.getIamPrincipalArn()))
                        .withIamRoleName(awsIamRoleArnParser.getRoleName(iamRolePermission.getIamPrincipalArn()))
                        .withRoleId(iamRolePermission.getRoleId()))
                .collect(Collectors.toSet()));

        return safeDepositBoxV1;
    }

    /**
     * Converts a v1 API version safe deposit box into a v2 version
     * @param safeDepositBoxV1 - V1 API version safe deposit box
     * @return - V2 API version safe deposit box
     */
    protected SafeDepositBoxV2 convertSafeDepositBoxV1ToV2(SafeDepositBoxV1 safeDepositBoxV1) {

        final SafeDepositBoxV2 safeDepositBoxV2 = new SafeDepositBoxV2();
        safeDepositBoxV2.setId(safeDepositBoxV1.getId());
        safeDepositBoxV2.setName(safeDepositBoxV1.getName());
        safeDepositBoxV2.setDescription(safeDepositBoxV1.getDescription());
        safeDepositBoxV2.setPath(safeDepositBoxV1.getPath());
        safeDepositBoxV2.setCategoryId(safeDepositBoxV1.getCategoryId());
        safeDepositBoxV2.setCreatedBy(safeDepositBoxV1.getCreatedBy());
        safeDepositBoxV2.setLastUpdatedBy(safeDepositBoxV1.getLastUpdatedBy());
        safeDepositBoxV2.setCreatedTs(safeDepositBoxV1.getCreatedTs());
        safeDepositBoxV2.setLastUpdatedTs(safeDepositBoxV1.getLastUpdatedTs());
        safeDepositBoxV2.setOwner(safeDepositBoxV1.getOwner());
        safeDepositBoxV2.setUserGroupPermissions(safeDepositBoxV1.getUserGroupPermissions());
        safeDepositBoxV2.setIamRolePermissions(safeDepositBoxV1.getIamRolePermissions().stream()
                .map(iamRolePermission -> new IamRolePermissionV2()
                        .withIamPrincipalArn(String.format(AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_TEMPLATE,
                                iamRolePermission.getAccountId(), iamRolePermission.getIamRoleName()))
                        .withRoleId(iamRolePermission.getRoleId()))
                .collect(Collectors.toSet()));

        return safeDepositBoxV2;
    }

    /**
     * @return The total number of safe deposit boxes.
     */
    public int getTotalNumberOfSafeDepositBoxes() {
        return safeDepositBoxDao.getSafeDepositBoxCount();
    }

    /**
     *
     * A paginatable method for iterating retrieving all SDBs
     *
     * @param limit The maximum number of SDBs to fetch
     * @param offset The offset to paginate with
     */
    public List<SafeDepositBoxV2> getSafeDepositBoxes(int limit, int offset) {
        List<SafeDepositBoxRecord> records = safeDepositBoxDao.getSafeDepositBoxes(limit, offset);
        List<SafeDepositBoxV2> result = new LinkedList<>();
        records.forEach(safeDepositBoxRecord -> {
            result.add(getSDBFromRecordV2(safeDepositBoxRecord));
        });
        return result;
    }

    /**
     * @param name Safe Deposit Box name
     * @return The id for the box
     */
    public Optional<String> getSafeDepositBoxIdByName(String name) {
        return Optional.ofNullable(safeDepositBoxDao.getSafeDepositBoxIdByName(name));
    }

    /**
     * @param id Safe Deposit Box id
     * @return The name for the box
     */
    public Optional<String> getSafeDepositBoxNameById(String id) {
        return Optional.ofNullable(safeDepositBoxDao.getSafeDepositBoxNameById(id));
    }


    /**
     * Admin method for restoring sdb
     * @param safeDepositBox Safe Deposit Box to restore
     */
    @Transactional
    public void restoreSafeDepositBox(SafeDepositBoxV2 safeDepositBox,
                                      String adminUser) {

        SafeDepositBoxRecord boxToStore = new SafeDepositBoxRecord();
        boxToStore.setId(safeDepositBox.getId());
        boxToStore.setPath(safeDepositBox.getPath());
        boxToStore.setCategoryId(safeDepositBox.getCategoryId());
        boxToStore.setName(safeDepositBox.getName());
        boxToStore.setDescription(safeDepositBox.getDescription());
        boxToStore.setCreatedTs(safeDepositBox.getCreatedTs());
        boxToStore.setLastUpdatedTs(safeDepositBox.getLastUpdatedTs());
        boxToStore.setCreatedBy(safeDepositBox.getCreatedBy());
        boxToStore.setLastUpdatedBy(safeDepositBox.getLastUpdatedBy());

        OffsetDateTime now = dateTimeSupplier.get();
        Optional<SafeDepositBoxRecord> existingBoxRecord = safeDepositBoxDao.getSafeDepositBox(safeDepositBox.getId());
        if (existingBoxRecord.isPresent()) {
            safeDepositBoxDao.fullUpdateSafeDepositBox(boxToStore);
            SafeDepositBoxV2 existingBox = getSDBFromRecordV2(existingBoxRecord.get());
            updateOwner(safeDepositBox.getId(), safeDepositBox.getOwner(), adminUser, now);
            modifyUserGroupPermissions(existingBox, safeDepositBox.getUserGroupPermissions(), adminUser, now);
            modifyIamRolePermissions(existingBox, safeDepositBox.getIamRolePermissions(), adminUser, now);
        } else {
            safeDepositBoxDao.createSafeDepositBox(boxToStore);
            addOwnerPermission(safeDepositBox.getUserGroupPermissions(), safeDepositBox.getOwner());
            userGroupPermissionService.grantUserGroupPermissions(
                    safeDepositBox.getId(),
                    safeDepositBox.getUserGroupPermissions(),
                    adminUser,
                    now);

            iamRolePermissionService.grantIamRolePermissions(
                    safeDepositBox.getId(),
                    safeDepositBox.getIamRolePermissions(),
                    adminUser,
                    now);

            vaultPolicyService.createStandardPolicies(safeDepositBox.getName(), safeDepositBox.getPath());
        }
    }
}
