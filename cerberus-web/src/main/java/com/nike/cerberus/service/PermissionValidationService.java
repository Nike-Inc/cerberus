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

import static com.nike.cerberus.record.RoleRecord.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.collect.Sets;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.SecureDataAction;
import com.nike.cerberus.dao.PermissionsDao;
import com.nike.cerberus.domain.UserGroupPermission;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.CustomApiError;
import com.nike.cerberus.util.SdbAccessRequest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component("permissionValidationService")
public class PermissionValidationService {

  private final UserGroupPermissionService userGroupPermissionService;
  private final PermissionsDao permissionsDao;
  private final boolean userGroupsCaseSensitive;
  private final AwsIamRoleArnParser awsIamRoleArnParser;
  private final SafeDepositBoxService safeDepositBoxService;
  private final SdbAccessRequest sdbAccessRequest;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Autowired
  public PermissionValidationService(
      UserGroupPermissionService userGroupPermissionService,
      PermissionsDao permissionsDao,
      boolean userGroupsCaseSensitive,
      AwsIamRoleArnParser awsIamRoleArnParser,
      SafeDepositBoxService safeDepositBoxService,
      SdbAccessRequest sdbAccessRequest,
      AuditLoggingFilterDetails auditLoggingFilterDetails) {

    this.userGroupPermissionService = userGroupPermissionService;
    this.permissionsDao = permissionsDao;
    this.userGroupsCaseSensitive = userGroupsCaseSensitive;
    this.awsIamRoleArnParser = awsIamRoleArnParser;
    this.safeDepositBoxService = safeDepositBoxService;
    this.sdbAccessRequest = sdbAccessRequest;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
  }

  /**
   * Checks to see if a principal has owner permissions for a given SDB.
   *
   * @param principal The principal under question
   * @param sdbId The sdb to check
   * @return True if the principal has owner permissions
   */
  public boolean doesPrincipalHaveOwnerPermissions(CerberusPrincipal principal, String sdbId) {
    var sdb = safeDepositBoxService.getSafeDepositBoxDangerouslyWithoutPermissionValidation(sdbId);

    boolean principalHasOwnerPermissions = false;
    switch (principal.getPrincipalType()) {
      case IAM:
        principalHasOwnerPermissions =
            doesIamPrincipalHavePermission(principal, sdb.getId(), Sets.newHashSet(ROLE_OWNER));
        break;
      case USER:
        principalHasOwnerPermissions =
            userGroupsCaseSensitive
                ? principal.getUserGroups().contains(sdb.getOwner())
                : containsIgnoreCase(principal.getUserGroups(), sdb.getOwner());
        break;
    }

    return principalHasOwnerPermissions;
  }

  /**
   * Checks to see if a Principal has any permissions to a given SDB.
   *
   * @param principal The authenticated principal
   * @param sdbId The SDB to check.
   * @return True if the user has read permissions
   */
  public boolean doesPrincipalHaveReadPermission(CerberusPrincipal principal, String sdbId) {
    boolean principalHasPermissionAssociationWithSdb = false;

    switch (principal.getPrincipalType()) {
      case IAM:
        // if the authenticated principal is an IAM Principal check to see that the iam principal is
        // associated with the requested sdb
        principalHasPermissionAssociationWithSdb =
            doesIamPrincipalHavePermission(
                principal, sdbId, Sets.newHashSet(ROLE_READ, ROLE_OWNER, ROLE_WRITE));
        break;
      case USER:
        // if the the principal is a user principal ensure that one of the users groups is
        // associated with the sdb
        Set<UserGroupPermission> userGroupPermissions =
            userGroupPermissionService.getUserGroupPermissions(sdbId);
        Set<String> userGroups =
            userGroupPermissions.stream()
                .map(UserGroupPermission::getName)
                .collect(Collectors.toSet());
        principalHasPermissionAssociationWithSdb =
            userGroupsCaseSensitive
                ? doesHaveIntersection(userGroups, principal.getUserGroups())
                : doesHaveIntersectionIgnoreCase(userGroups, principal.getUserGroups());
        break;
    }
    return principalHasPermissionAssociationWithSdb;
  }

  public boolean doesPrincipalHavePermissionForSdb(
      CerberusPrincipal principal, String sdbId, SecureDataAction action) {
    boolean hasPermission = false;
    switch (principal.getPrincipalType()) {
      case IAM:
        hasPermission = doesIamPrincipalHavePermission(principal, sdbId, action.getAllowedRoles());
        break;
      case USER:
        hasPermission =
            userGroupsCaseSensitive
                ? permissionsDao.doesUserPrincipalHaveRoleForSdb(
                    sdbId, action.getAllowedRoles(), principal.getUserGroups())
                : permissionsDao.doesUserHavePermsForRoleAndSdbCaseInsensitive(
                    sdbId, action.getAllowedRoles(), principal.getUserGroups());
        break;
      default:
        log.error(
            "Unknown Principal Type: {}, returning hasPermission: false",
            principal.getPrincipalType().getName());
        break;
    }

    log.debug(
        "Principal: {}, Type: {}, SDB: {}, Action: {}, hasPermissions: {}",
        principal.getName(),
        principal.getPrincipalType().getName(),
        sdbId,
        action.name(),
        hasPermission);

    return hasPermission;
  }

  protected boolean doesIamPrincipalHavePermission(
      CerberusPrincipal principal, String sdbId, Set<String> roles) {
    String iamPrincipalArn = principal.getName();
    String iamRootArn = awsIamRoleArnParser.convertPrincipalArnToRootArn(iamPrincipalArn);
    if (awsIamRoleArnParser.isAssumedRoleArn(iamPrincipalArn)) {
      String iamRoleArn = awsIamRoleArnParser.convertPrincipalArnToRoleArn(iamPrincipalArn);
      return permissionsDao.doesAssumedRoleHaveRoleForSdb(
          sdbId, iamPrincipalArn, iamRoleArn, iamRootArn, roles);
    } else {
      return permissionsDao.doesIamPrincipalHaveRoleForSdb(
          sdbId, iamPrincipalArn, iamRootArn, roles);
    }
  }

  /**
   * Does a case-insensitive check to see if the collection contains the given String
   *
   * @param items List of strings from which to search
   * @param object String for which to search
   * @return True if the object exists in the array, false if not
   */
  private boolean containsIgnoreCase(Collection<String> items, String object) {
    return items.stream().anyMatch(item -> item.equalsIgnoreCase(object));
  }

  /**
   * Checks if any string is contained in both the first collection and the second collection
   *
   * @return True if both collections contain any one string, false if not
   */
  private boolean doesHaveIntersection(Collection<String> co1, Collection<String> co2) {
    return co2.stream().anyMatch(co1::contains);
  }

  /**
   * Checks (ignoring case) if any string is contained in both the first collection and the second
   * collection
   *
   * @return True if both collections contain any one string, false if not
   */
  private boolean doesHaveIntersectionIgnoreCase(Collection<String> co1, Collection<String> co2) {
    Set<String> co1LowerCase = co1.stream().map(String::toLowerCase).collect(Collectors.toSet());

    return co2.stream().map(String::toLowerCase).anyMatch(co1LowerCase::contains);
  }

  public boolean doesPrincipalHaveSdbPermissionsForAction(String action) {
    var request =
        Optional.ofNullable(getRequestAttributesFromContext())
            .filter(
                requestAttributes ->
                    ServletRequestAttributes.class.isAssignableFrom(requestAttributes.getClass()))
            .map(requestAttributes -> ((ServletRequestAttributes) requestAttributes))
            .map(ServletRequestAttributes::getRequest)
            .orElseThrow(() -> new RuntimeException("Failed to get request from context"));

    var requestPath = request.getServletPath();

    List.of("/v1/secret", "/v1/sdb-secret-version-paths", "/v1/secure-file").stream()
        .filter(requestPath::startsWith)
        .findAny()
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Only secure data endpoints can use this perms checking method"));

    parseRequestPathInfo(requestPath);

    if (isBlank(sdbAccessRequest.getCategory()) || isBlank(sdbAccessRequest.getSdbSlug())) {
      auditLoggingFilterDetails.setAction("Required path params missing");
      String msg = "Request path is invalid.";
      throw ApiException.newBuilder()
          .withApiErrors(
              CustomApiError.createCustomApiError(DefaultApiError.GENERIC_BAD_REQUEST, msg))
          .withExceptionMessage(msg)
          .build();
    }

    var principal = getCerberusPrincipalFromContext();
    var sdbBasePath =
        String.format("%s/%s/", sdbAccessRequest.getCategory(), sdbAccessRequest.getSdbSlug());
    var secureDataAction = SecureDataAction.fromString(action);

    String sdbId =
        safeDepositBoxService
            .getSafeDepositBoxIdByPath(sdbBasePath)
            .orElseThrow(
                () -> {
                  auditLoggingFilterDetails.setAction(
                      "A request was made for an SDB that did not exist");
                  String msg = "The SDB for the path: " + sdbBasePath + " was not found.";
                  return ApiException.newBuilder()
                      .withApiErrors(
                          CustomApiError.createCustomApiError(
                              DefaultApiError.ENTITY_NOT_FOUND, msg))
                      .withExceptionMessage(msg)
                      .build();
                });

    if (!doesPrincipalHavePermissionForSdb(principal, sdbId, secureDataAction)) {
      auditLoggingFilterDetails.setAction("Permission was not granted for principal");
      String msg =
          String.format(
              "Permission was not granted for principal: %s for path: %s",
              principal.getName(), sdbBasePath);
      throw ApiException.newBuilder()
          .withApiErrors(CustomApiError.createCustomApiError(DefaultApiError.ACCESS_DENIED, msg))
          .withExceptionMessage(msg)
          .build();
    }

    sdbAccessRequest.setPrincipal(principal);
    sdbAccessRequest.setSdbId(sdbId);

    return true;
  }

  RequestAttributes getRequestAttributesFromContext() {
    return RequestContextHolder.getRequestAttributes();
  }

  CerberusPrincipal getCerberusPrincipalFromContext() {
    return (CerberusPrincipal) SecurityContextHolder.getContext().getAuthentication();
  }

  private void parseRequestPathInfo(String requestPath) {
    String[] parts = requestPath.replace("//", "/").split("/", 6);

    if (parts.length >= 4) {
      sdbAccessRequest.setCategory(parts[3]);
    }
    if (parts.length >= 5) {
      sdbAccessRequest.setSdbSlug(parts[4]);
    }

    if (parts.length >= 6) {
      sdbAccessRequest.setSubPath(parts[5]);
    }
  }
}
