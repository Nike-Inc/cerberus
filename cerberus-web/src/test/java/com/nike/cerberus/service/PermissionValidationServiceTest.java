package com.nike.cerberus.service;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.SecureDataAction;
import com.nike.cerberus.dao.PermissionsDao;
import com.nike.cerberus.domain.SafeDepositBoxV2;
import com.nike.cerberus.domain.UserGroupPermission;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.SdbAccessRequest;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

public class PermissionValidationServiceTest {

  public static final String IAM_PRINCIPAL_ARN = "iamPrincipalArn";
  @Mock private UserGroupPermissionService userGroupPermissionService;
  @Mock private PermissionsDao permissionsDao;
  @Mock private AwsIamRoleArnParser awsIamRoleArnParser;
  @Mock private SafeDepositBoxService safeDepositBoxService;
  @Mock private SdbAccessRequest sdbAccessRequest;
  @Mock private AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testDoesPrincipalHaveOwnerPermissionsWithGroupsCaseSensitive() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(true);
    Set<String> userGroups = new HashSet<>();
    userGroups.add("userGroup1");
    SafeDepositBoxV2 safeDepositBoxV2 = mockSafeDepositBoxV2WithOwner("userGroup1");
    Mockito.when(
            safeDepositBoxService.getSafeDepositBoxDangerouslyWithoutPermissionValidation("sdbId"))
        .thenReturn(safeDepositBoxV2);
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    boolean hasOwnerPermission =
        permissionValidationService.doesPrincipalHaveOwnerPermissions(cerberusPrincipal, "sdbId");
    Assert.assertTrue(hasOwnerPermission);
  }

  @Test
  public void
      testDoesPrincipalHaveOwnerPermissionsWithGroupsCaseSensitiveAndUserGroupsInUpperCase() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(true);
    Set<String> userGroups = new HashSet<>();
    userGroups.add("USERGROUP1");
    SafeDepositBoxV2 safeDepositBoxV2 = mockSafeDepositBoxV2WithOwner("userGroup1");
    Mockito.when(
            safeDepositBoxService.getSafeDepositBoxDangerouslyWithoutPermissionValidation("sdbId"))
        .thenReturn(safeDepositBoxV2);
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    boolean hasOwnerPermission =
        permissionValidationService.doesPrincipalHaveOwnerPermissions(cerberusPrincipal, "sdbId");
    Assert.assertFalse(hasOwnerPermission);
  }

  @Test
  public void testDoesPrincipalHaveOwnerPermissionsWithGroupsCaseInSensitiveUserGroupsInLowerCse() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(false);
    Set<String> userGroups = new HashSet<>();
    userGroups.add("usergroup1");
    SafeDepositBoxV2 safeDepositBoxV2 = mockSafeDepositBoxV2WithOwner("userGroup1");
    Mockito.when(
            safeDepositBoxService.getSafeDepositBoxDangerouslyWithoutPermissionValidation("sdbId"))
        .thenReturn(safeDepositBoxV2);
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    boolean hasOwnerPermission =
        permissionValidationService.doesPrincipalHaveOwnerPermissions(cerberusPrincipal, "sdbId");
    Assert.assertTrue(hasOwnerPermission);
  }

  @Test
  public void testDoesPrincipalHaveOwnerPermissionsWithGroupsCaseInSensitiveUserGroupsInUpperCse() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(false);
    Set<String> userGroups = new HashSet<>();
    userGroups.add("USERGROUP1");
    SafeDepositBoxV2 safeDepositBoxV2 = mockSafeDepositBoxV2WithOwner("userGroup1");
    Mockito.when(
            safeDepositBoxService.getSafeDepositBoxDangerouslyWithoutPermissionValidation("sdbId"))
        .thenReturn(safeDepositBoxV2);
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    boolean hasOwnerPermission =
        permissionValidationService.doesPrincipalHaveOwnerPermissions(cerberusPrincipal, "sdbId");
    Assert.assertTrue(hasOwnerPermission);
  }

  @Test
  public void testDoesPrincipalHaveOwnerPermissionsWithPrincipalTypeIAM() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(false);
    SafeDepositBoxV2 safeDepositBoxV2 = mockSafeDepositBoxV2WithId("id");
    Mockito.when(
            safeDepositBoxService.getSafeDepositBoxDangerouslyWithoutPermissionValidation("sdbId"))
        .thenReturn(safeDepositBoxV2);
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndName(PrincipalType.IAM, IAM_PRINCIPAL_ARN);
    String iamRootArn = "iamRootArn";
    Mockito.when(awsIamRoleArnParser.convertPrincipalArnToRootArn(IAM_PRINCIPAL_ARN))
        .thenReturn(iamRootArn);
    Mockito.when(awsIamRoleArnParser.isAssumedRoleArn(IAM_PRINCIPAL_ARN)).thenReturn(true);
    String iamRoleArn = "iamRoleArn";
    Mockito.when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(IAM_PRINCIPAL_ARN))
        .thenReturn(iamRoleArn);
    Mockito.when(
            permissionsDao.doesAssumedRoleHaveRoleForSdb(
                Mockito.eq("id"),
                Mockito.eq(IAM_PRINCIPAL_ARN),
                Mockito.eq(iamRoleArn),
                Mockito.eq(iamRootArn),
                Mockito.anySet()))
        .thenReturn(true);
    boolean hasOwnerPermission =
        permissionValidationService.doesPrincipalHaveOwnerPermissions(cerberusPrincipal, "sdbId");

    Assert.assertTrue(hasOwnerPermission);
    Mockito.verify(awsIamRoleArnParser).convertPrincipalArnToRootArn(IAM_PRINCIPAL_ARN);
    Mockito.verify(awsIamRoleArnParser).isAssumedRoleArn(IAM_PRINCIPAL_ARN);
    Mockito.verify(awsIamRoleArnParser).convertPrincipalArnToRoleArn(IAM_PRINCIPAL_ARN);
    Mockito.verify(permissionsDao, Mockito.never())
        .doesIamPrincipalHaveRoleForSdb(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anySet());
  }

  @Test
  public void testDoesPrincipalHaveOwnerPermissionsWithPrincipalTypeIAMWhenRoleIsNotAssumed() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(false);
    SafeDepositBoxV2 safeDepositBoxV2 = mockSafeDepositBoxV2WithId("id");
    Mockito.when(
            safeDepositBoxService.getSafeDepositBoxDangerouslyWithoutPermissionValidation("sdbId"))
        .thenReturn(safeDepositBoxV2);
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndName(PrincipalType.IAM, IAM_PRINCIPAL_ARN);
    String iamRootArn = "iamRootArn";
    Mockito.when(awsIamRoleArnParser.convertPrincipalArnToRootArn(IAM_PRINCIPAL_ARN))
        .thenReturn(iamRootArn);
    Mockito.when(awsIamRoleArnParser.isAssumedRoleArn(IAM_PRINCIPAL_ARN)).thenReturn(false);
    String iamRoleArn = "iamRoleArn";
    Mockito.when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(IAM_PRINCIPAL_ARN))
        .thenReturn(iamRoleArn);
    Mockito.when(
            permissionsDao.doesIamPrincipalHaveRoleForSdb(
                Mockito.eq("id"),
                Mockito.eq(IAM_PRINCIPAL_ARN),
                Mockito.eq(iamRootArn),
                Mockito.anySet()))
        .thenReturn(true);
    boolean hasOwnerPermission =
        permissionValidationService.doesPrincipalHaveOwnerPermissions(cerberusPrincipal, "sdbId");

    Assert.assertTrue(hasOwnerPermission);
    Mockito.verify(awsIamRoleArnParser).convertPrincipalArnToRootArn(IAM_PRINCIPAL_ARN);
    Mockito.verify(awsIamRoleArnParser).isAssumedRoleArn(IAM_PRINCIPAL_ARN);
    Mockito.verify(awsIamRoleArnParser, Mockito.never())
        .convertPrincipalArnToRoleArn(IAM_PRINCIPAL_ARN);
    Mockito.verify(permissionsDao, Mockito.never())
        .doesAssumedRoleHaveRoleForSdb(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anySet());
  }

  @Test
  public void testDoesPrincipalHavePermissionForSdbWithPrincipalTypeUserAndCaseInsensitive() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(false);
    Set<String> userGroups = new HashSet<>();
    userGroups.add("userGroup1");
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    Mockito.when(
            permissionsDao.doesUserHavePermsForRoleAndSdbCaseInsensitive(
                Mockito.eq("sdbId"), Mockito.anySet(), Mockito.anySet()))
        .thenReturn(true);
    boolean hasPermission =
        permissionValidationService.doesPrincipalHavePermissionForSdb(
            cerberusPrincipal, "sdbId", SecureDataAction.READ);
    Assert.assertTrue(hasPermission);
    Mockito.verify(permissionsDao)
        .doesUserHavePermsForRoleAndSdbCaseInsensitive(
            Mockito.eq("sdbId"), Mockito.anySet(), Mockito.anySet());
    Mockito.verify(permissionsDao, Mockito.never())
        .doesUserPrincipalHaveRoleForSdb(Mockito.anyString(), Mockito.anySet(), Mockito.anySet());
  }

  @Test
  public void testDoesPrincipalHavePermissionForSdbWithPrincipalTypeUserAndCaseSensitive() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(true);
    Set<String> userGroups = new HashSet<>();
    userGroups.add("userGroup1");
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    Mockito.when(
            permissionsDao.doesUserPrincipalHaveRoleForSdb(
                Mockito.eq("sdbId"), Mockito.anySet(), Mockito.anySet()))
        .thenReturn(true);
    boolean hasPermission =
        permissionValidationService.doesPrincipalHavePermissionForSdb(
            cerberusPrincipal, "sdbId", SecureDataAction.READ);
    Assert.assertTrue(hasPermission);
    Mockito.verify(permissionsDao, Mockito.never())
        .doesUserHavePermsForRoleAndSdbCaseInsensitive(
            Mockito.eq("sdbId"), Mockito.anySet(), Mockito.anySet());
    Mockito.verify(permissionsDao)
        .doesUserPrincipalHaveRoleForSdb(Mockito.anyString(), Mockito.anySet(), Mockito.anySet());
  }

  @Test
  public void testDoesPrincipalHavePermissionForSdbWithPrincipalTypeIAMAndRoleIsAssumed() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(true);
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndName(PrincipalType.IAM, IAM_PRINCIPAL_ARN);
    String iamRootArn = "iamRootArn";
    Mockito.when(awsIamRoleArnParser.convertPrincipalArnToRootArn(IAM_PRINCIPAL_ARN))
        .thenReturn(iamRootArn);
    Mockito.when(awsIamRoleArnParser.isAssumedRoleArn(IAM_PRINCIPAL_ARN)).thenReturn(true);
    String iamRoleArn = "iamRoleArn";
    Mockito.when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(IAM_PRINCIPAL_ARN))
        .thenReturn(iamRoleArn);
    Mockito.when(
            permissionsDao.doesAssumedRoleHaveRoleForSdb(
                Mockito.eq("sdbId"),
                Mockito.eq(IAM_PRINCIPAL_ARN),
                Mockito.eq(iamRoleArn),
                Mockito.eq(iamRootArn),
                Mockito.anySet()))
        .thenReturn(true);

    boolean hasPermission =
        permissionValidationService.doesPrincipalHavePermissionForSdb(
            cerberusPrincipal, "sdbId", SecureDataAction.READ);
    Assert.assertTrue(hasPermission);
    Mockito.verify(awsIamRoleArnParser).convertPrincipalArnToRootArn(IAM_PRINCIPAL_ARN);
    Mockito.verify(awsIamRoleArnParser).isAssumedRoleArn(IAM_PRINCIPAL_ARN);
    Mockito.verify(awsIamRoleArnParser).convertPrincipalArnToRoleArn(IAM_PRINCIPAL_ARN);
    Mockito.verify(permissionsDao, Mockito.never())
        .doesIamPrincipalHaveRoleForSdb(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anySet());
  }

  @Test
  public void testDoesPrincipalHavePermissionForSdbWithPrincipalTypeIAMAndRoleIsNotAssumed() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(true);
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndName(PrincipalType.IAM, IAM_PRINCIPAL_ARN);
    String iamRootArn = "iamRootArn";
    Mockito.when(awsIamRoleArnParser.convertPrincipalArnToRootArn(IAM_PRINCIPAL_ARN))
        .thenReturn(iamRootArn);
    Mockito.when(awsIamRoleArnParser.isAssumedRoleArn(IAM_PRINCIPAL_ARN)).thenReturn(false);
    String iamRoleArn = "iamRoleArn";
    Mockito.when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(IAM_PRINCIPAL_ARN))
        .thenReturn(iamRoleArn);
    Mockito.when(
            permissionsDao.doesIamPrincipalHaveRoleForSdb(
                Mockito.eq("sdbId"),
                Mockito.eq(IAM_PRINCIPAL_ARN),
                Mockito.eq(iamRootArn),
                Mockito.anySet()))
        .thenReturn(true);
    boolean hasPermission =
        permissionValidationService.doesPrincipalHavePermissionForSdb(
            cerberusPrincipal, "sdbId", SecureDataAction.READ);
    Assert.assertTrue(hasPermission);
    Mockito.verify(permissionsDao, Mockito.never())
        .doesUserHavePermsForRoleAndSdbCaseInsensitive(
            Mockito.eq("sdbId"), Mockito.anySet(), Mockito.anySet());
    Mockito.verify(permissionsDao)
        .doesIamPrincipalHaveRoleForSdb(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anySet());
  }

  @Test
  public void testDoesPrincipalHaveReadPermissionWithPrincipalTypeAndGroupsCaseSensitive() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(true);
    Set<String> userGroups = new HashSet<>();
    userGroups.add("userGroup1");
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    Set<UserGroupPermission> userGroupPermissions = mockUserGroupPermissionWithName();
    Mockito.when(userGroupPermissionService.getUserGroupPermissions("sdbId"))
        .thenReturn(userGroupPermissions);
    boolean hasPermission =
        permissionValidationService.doesPrincipalHaveReadPermission(cerberusPrincipal, "sdbId");
    Assert.assertTrue(hasPermission);
  }

  @Test
  public void
      testDoesPrincipalHaveReadPermissionWithPrincipalTypeAndGroupsCaseSensitiveHavingUserGroupsInUpperCase() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(true);
    Set<String> userGroups = new HashSet<>();
    userGroups.add("USERGROUP1");
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    Set<UserGroupPermission> userGroupPermissions = mockUserGroupPermissionWithName();
    Mockito.when(userGroupPermissionService.getUserGroupPermissions("sdbId"))
        .thenReturn(userGroupPermissions);
    boolean hasPermission =
        permissionValidationService.doesPrincipalHaveReadPermission(cerberusPrincipal, "sdbId");
    Assert.assertFalse(hasPermission);
  }

  @Test
  public void
      testDoesPrincipalHaveReadPermissionWithPrincipalTypeAndGroupsCaseInSensitiveHavingUserGroupsInUpperCase() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(false);
    Set<String> userGroups = new HashSet<>();
    userGroups.add("USERGROUP1");
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    Set<UserGroupPermission> userGroupPermissions = mockUserGroupPermissionWithName();
    Mockito.when(userGroupPermissionService.getUserGroupPermissions("sdbId"))
        .thenReturn(userGroupPermissions);
    boolean hasPermission =
        permissionValidationService.doesPrincipalHaveReadPermission(cerberusPrincipal, "sdbId");
    Assert.assertTrue(hasPermission);
  }

  @Test
  public void
      testDoesPrincipalHaveReadPermissionWithPrincipalTypeAndGroupsCaseInSensitiveHavingUserGroupsInLowerCase() {
    PermissionValidationService permissionValidationService =
        createPermissionValidationServiceWithGroupCaseSensitive(false);
    Set<String> userGroups = new HashSet<>();
    userGroups.add("usergroup1");
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    Set<UserGroupPermission> userGroupPermissions = mockUserGroupPermissionWithName();
    Mockito.when(userGroupPermissionService.getUserGroupPermissions("sdbId"))
        .thenReturn(userGroupPermissions);
    boolean hasPermission =
        permissionValidationService.doesPrincipalHaveReadPermission(cerberusPrincipal, "sdbId");
    Assert.assertTrue(hasPermission);
  }

  @Test
  public void testDoesPrincipalHaveSdbPermissionsForActionWhenRequestAttributesFromContextIsNull() {
    PermissionValidationService permissionValidationService =
        Mockito.spy(createPermissionValidationServiceWithGroupCaseSensitive(false));
    Mockito.when(permissionValidationService.getRequestAttributesFromContext()).thenReturn(null);
    String exceptionMessage = "";
    try {
      permissionValidationService.doesPrincipalHaveSdbPermissionsForAction("action");
    } catch (RuntimeException e) {
      exceptionMessage = e.getMessage();
    }
    Assert.assertTrue("Failed to get request from context".equals(exceptionMessage));
  }

  @Test
  public void
      testDoesPrincipalHaveSdbPermissionsForActionWhenRequestAttributesWhenServletPathIsNotSecured() {
    PermissionValidationService permissionValidationService =
        Mockito.spy(createPermissionValidationServiceWithGroupCaseSensitive(false));
    RequestAttributes requestAttributes =
        mockServletRequestAttributesWithRequestWithServletPath("/v1/sample");
    Mockito.when(permissionValidationService.getRequestAttributesFromContext())
        .thenReturn(requestAttributes);
    String exceptionMessage = "";
    try {
      permissionValidationService.doesPrincipalHaveSdbPermissionsForAction("action");
    } catch (RuntimeException e) {
      exceptionMessage = e.getMessage();
    }
    Assert.assertTrue(
        "Only secure data endpoints can use this perms checking method".equals(exceptionMessage));
  }

  @Test
  public void
      testDoesPrincipalHaveSdbPermissionsForActionWhenRequestAttributesWhenServletPathIsSecuredAndVerifyPathIsInvalid() {
    PermissionValidationService permissionValidationService =
        Mockito.spy(createPermissionValidationServiceWithGroupCaseSensitive(false));
    RequestAttributes requestAttributes =
        mockServletRequestAttributesWithRequestWithServletPath("/v1/secret/1/2/3/4");
    Mockito.when(permissionValidationService.getRequestAttributesFromContext())
        .thenReturn(requestAttributes);
    String exceptionMessage = "";
    try {
      permissionValidationService.doesPrincipalHaveSdbPermissionsForAction("action");
    } catch (ApiException apiException) {
      exceptionMessage = apiException.getMessage();
    }
    Mockito.verify(sdbAccessRequest).setSdbSlug("2");
    Mockito.verify(sdbAccessRequest).setCategory("1");
    Mockito.verify(sdbAccessRequest).setSubPath("3/4");
    Assert.assertTrue("Request path is invalid.".equals(exceptionMessage));
  }

  @Test
  public void
      testDoesPrincipalHaveSdbPermissionsForActionWhenRequestAttributesWhenServletPathIsSecuredAndVerifyPathIsValid() {
    PermissionValidationService permissionValidationService =
        Mockito.spy(createPermissionValidationServiceWithGroupCaseSensitive(false));
    RequestAttributes requestAttributes =
        mockServletRequestAttributesWithRequestWithServletPath("/v1/secret/1/2/3/4");
    Mockito.when(permissionValidationService.getRequestAttributesFromContext())
        .thenReturn(requestAttributes);
    Mockito.when(sdbAccessRequest.getCategory()).thenReturn("category");
    Mockito.when(sdbAccessRequest.getSdbSlug()).thenReturn("slug");
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndName(PrincipalType.USER, "name");
    Mockito.when(permissionValidationService.getCerberusPrincipalFromContext())
        .thenReturn(cerberusPrincipal);
    Mockito.when(safeDepositBoxService.getSafeDepositBoxIdByPath("category/slug/"))
        .thenReturn(Optional.empty());
    String exceptionMessage = "";
    try {
      permissionValidationService.doesPrincipalHaveSdbPermissionsForAction("READ");
    } catch (ApiException apiException) {
      exceptionMessage = apiException.getMessage();
    }
    Assert.assertTrue(
        "The SDB for the path: category/slug/ was not found.".equals(exceptionMessage));
  }

  @Test
  public void
      testDoesPrincipalHaveSdbPermissionsForActionWhenRequestAttributesWhenServletPathIsSecuredAndVerifySdbidPresent() {
    PermissionValidationService permissionValidationService =
        Mockito.spy(createPermissionValidationServiceWithGroupCaseSensitive(false));
    RequestAttributes requestAttributes =
        mockServletRequestAttributesWithRequestWithServletPath("/v1/secret/1/2/3/4");
    Mockito.when(permissionValidationService.getRequestAttributesFromContext())
        .thenReturn(requestAttributes);
    Mockito.when(sdbAccessRequest.getCategory()).thenReturn("category");
    Mockito.when(sdbAccessRequest.getSdbSlug()).thenReturn("slug");
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndName(PrincipalType.USER, "name");
    Mockito.when(permissionValidationService.getCerberusPrincipalFromContext())
        .thenReturn(cerberusPrincipal);
    Mockito.when(safeDepositBoxService.getSafeDepositBoxIdByPath("category/slug/"))
        .thenReturn(Optional.of("sdbId"));
    String exceptionMessage = "";
    try {
      permissionValidationService.doesPrincipalHaveSdbPermissionsForAction("READ");
    } catch (ApiException apiException) {
      exceptionMessage = apiException.getMessage();
    }
    Assert.assertEquals(
        "Permission was not granted for principal: name for path: category/slug/",
        exceptionMessage);
  }

  @Test
  public void
      testDoesPrincipalHaveSdbPermissionsForActionWhenRequestAttributesWhenServletPathIsSecuredAndHasPermission() {
    PermissionValidationService permissionValidationService =
        Mockito.spy(createPermissionValidationServiceWithGroupCaseSensitive(false));
    RequestAttributes requestAttributes =
        mockServletRequestAttributesWithRequestWithServletPath("/v1/secret/1/2/3/4");
    Mockito.when(permissionValidationService.getRequestAttributesFromContext())
        .thenReturn(requestAttributes);
    Mockito.when(sdbAccessRequest.getCategory()).thenReturn("category");
    Mockito.when(sdbAccessRequest.getSdbSlug()).thenReturn("slug");
    Set<String> userGroups = new HashSet<>();
    userGroups.add("userGroup1");
    CerberusPrincipal cerberusPrincipal =
        mockCerberusPrincipalWithPrincipalTypeAndUserGroups(PrincipalType.USER, userGroups);
    Mockito.when(permissionValidationService.getCerberusPrincipalFromContext())
        .thenReturn(cerberusPrincipal);
    Mockito.when(safeDepositBoxService.getSafeDepositBoxIdByPath("category/slug/"))
        .thenReturn(Optional.of("sdbId"));
    Mockito.when(
            permissionsDao.doesUserHavePermsForRoleAndSdbCaseInsensitive(
                Mockito.eq("sdbId"), Mockito.anySet(), Mockito.anySet()))
        .thenReturn(true);
    boolean hasPermission =
        permissionValidationService.doesPrincipalHaveSdbPermissionsForAction("READ");
    Assert.assertTrue(hasPermission);
    Mockito.verify(sdbAccessRequest).setPrincipal(Mockito.any(CerberusPrincipal.class));
    Mockito.verify(sdbAccessRequest).setSdbId("sdbId");
  }

  private ServletRequestAttributes mockServletRequestAttributesWithRequestWithServletPath(
      String servletRequestPath) {
    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
    ServletRequestAttributes servletRequestAttributes =
        new ServletRequestAttributes(httpServletRequest);
    Mockito.when(httpServletRequest.getServletPath()).thenReturn(servletRequestPath);
    return servletRequestAttributes;
  }

  private Set<UserGroupPermission> mockUserGroupPermissionWithName() {
    UserGroupPermission userGroupPermission = Mockito.mock(UserGroupPermission.class);
    Mockito.when(userGroupPermission.getName()).thenReturn("userGroup1");
    Set<UserGroupPermission> userGroupPermissions = new HashSet<>();
    userGroupPermissions.add(userGroupPermission);
    return userGroupPermissions;
  }

  private SafeDepositBoxV2 mockSafeDepositBoxV2WithId(String id) {
    SafeDepositBoxV2 safeDepositBoxV2 = Mockito.mock(SafeDepositBoxV2.class);
    Mockito.when(safeDepositBoxV2.getId()).thenReturn(id);
    return safeDepositBoxV2;
  }

  private SafeDepositBoxV2 mockSafeDepositBoxV2WithOwner(String owner) {
    SafeDepositBoxV2 safeDepositBoxV2 = Mockito.mock(SafeDepositBoxV2.class);
    Mockito.when(safeDepositBoxV2.getOwner()).thenReturn(owner);
    return safeDepositBoxV2;
  }

  private CerberusPrincipal mockCerberusPrincipalWithPrincipalTypeAndName(
      PrincipalType principalType, String name) {
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    Mockito.when(cerberusPrincipal.getPrincipalType()).thenReturn(principalType);
    Mockito.when(cerberusPrincipal.getName()).thenReturn(name);
    return cerberusPrincipal;
  }

  private CerberusPrincipal mockCerberusPrincipalWithPrincipalTypeAndUserGroups(
      PrincipalType principalType, Set<String> userGroups) {
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    Mockito.when(cerberusPrincipal.getPrincipalType()).thenReturn(principalType);
    Mockito.when(cerberusPrincipal.getUserGroups()).thenReturn(userGroups);
    return cerberusPrincipal;
  }

  private PermissionValidationService createPermissionValidationServiceWithGroupCaseSensitive(
      final boolean isGroupsCaseSensitive) {
    PermissionValidationService permissionValidationService =
        new PermissionValidationService(
            userGroupPermissionService,
            permissionsDao,
            isGroupsCaseSensitive,
            awsIamRoleArnParser,
            safeDepositBoxService,
            sdbAccessRequest,
            auditLoggingFilterDetails);
    return permissionValidationService;
  }
}
