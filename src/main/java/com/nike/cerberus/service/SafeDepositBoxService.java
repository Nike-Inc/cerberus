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
import com.nike.cerberus.domain.IamRolePermission;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.domain.SafeDepositBox;
import com.nike.cerberus.domain.SafeDepositBoxSummary;
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
                                 final DateTimeSupplier dateTimeSupplier) {
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
    public Optional<SafeDepositBox> getAssociatedSafeDepositBox(final Set<String> groups, final String id) {
        final Optional<SafeDepositBoxRecord> safeDepositBoxRecord = safeDepositBoxDao.getSafeDepositBox(id);

        if (safeDepositBoxRecord.isPresent()) {
            final Set<UserGroupPermission> userGroupPermissions = userGroupPermissionService.getUserGroupPermissions(id);

            final long count = userGroupPermissions.stream().filter(perm -> groups.contains(perm.getName())).count();
            if (count == 0) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.ACCESS_DENIED)
                        .build();
            }

            return Optional.of(getSDBFromRecord(safeDepositBoxRecord.get()));
        }

        return Optional.empty();
    }

    protected SafeDepositBox getSDBFromRecord(SafeDepositBoxRecord safeDepositBoxRecord) {
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

        final Set<IamRolePermission> iamRolePermissions = iamRolePermissionService.getIamRolePermissions(id);

        SafeDepositBox safeDepositBox = new SafeDepositBox();
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
    public String createSafeDepositBox(final SafeDepositBox safeDepositBox, final String user) {
        final OffsetDateTime now = dateTimeSupplier.get();
        final SafeDepositBoxRecord boxRecordToStore = buildBoxToStore(safeDepositBox, user, now);
        final Set<UserGroupPermission> userGroupPermissionSet = safeDepositBox.getUserGroupPermissions();
        addOwnerPermission(userGroupPermissionSet, safeDepositBox.getOwner());

        final Set<IamRolePermission> iamRolePermissionSet = addIamRoleArnToPermissions(safeDepositBox.getIamRolePermissions());

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
    public void updateSafeDepositBox(final SafeDepositBox safeDepositBox, final Set<String> groups,
                                     final String user, final String id) {
        final Optional<SafeDepositBox> currentBox = getAssociatedSafeDepositBox(groups, id);

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
        final Set<IamRolePermission> iamRolePermissionSet = safeDepositBox.getIamRolePermissions();

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
        final Optional<SafeDepositBox> box = getAssociatedSafeDepositBox(groups, id);

        if (!box.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                    .build();
        }

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

    private void assertIsOwner(final Set<String> groups, final SafeDepositBox box) {
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
    private SafeDepositBoxRecord buildBoxToStore(final SafeDepositBox requestedBox,
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
                                                  final SafeDepositBox safeDepositBox,
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
    protected void modifyUserGroupPermissions(final SafeDepositBox currentBox,
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
    protected void modifyIamRolePermissions(final SafeDepositBox currentBox,
                                          final Set<IamRolePermission> iamRolePermissionSet,
                                          final String user,
                                          final OffsetDateTime dateTime) {
        Set<IamRolePermission> toAddSet = Sets.newHashSet();
        Set<IamRolePermission> toUpdateSet = Sets.newHashSet();
        Set<IamRolePermission> toDeleteSet = Sets.newHashSet();

        for (IamRolePermission iamRolePermission : iamRolePermissionSet) {
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

        final Set<IamRolePermission> updatedToAddSet = addIamRoleArnToPermissions(toAddSet);
        iamRolePermissionService.grantIamRolePermissions(safeDepositBoxId, updatedToAddSet, user, dateTime);

        final Set<IamRolePermission> updatedToUpdateSet = addIamRoleArnToPermissions(toUpdateSet);
        iamRolePermissionService.updateIamRolePermissions(safeDepositBoxId, updatedToUpdateSet, user, dateTime);

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
     * Populates the ARN field for new Iam Role Permission objects
     * @param iamRolePermissions - IAM role permissions to be modified
     * @return - Modified IAM role permissions
     */
    protected Set<IamRolePermission> addIamRoleArnToPermissions(Set<IamRolePermission> iamRolePermissions) {

        return iamRolePermissions.stream()
                .map(iamRolePermission ->
                        iamRolePermission.withIamRoleArn(String.format(AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_TEMPLATE,
                                iamRolePermission.getAccountId(),
                                iamRolePermission.getIamRoleName())))
                .collect(Collectors.toSet());
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
    public List<SafeDepositBox> getSafeDepositBoxes(int limit, int offset) {
        List<SafeDepositBoxRecord> records = safeDepositBoxDao.getSafeDepositBoxes(limit, offset);
        List<SafeDepositBox> result = new LinkedList<>();
        records.forEach(safeDepositBoxRecord -> {
            result.add(getSDBFromRecord(safeDepositBoxRecord));
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
     * Admin method for restoring sdb
     * @param safeDepositBox Safe Deposit Box to restore
     */
    @Transactional
    public void restoreSafeDepositBox(SafeDepositBox safeDepositBox,
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
            SafeDepositBox existingBox = getSDBFromRecord(existingBoxRecord.get());
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
