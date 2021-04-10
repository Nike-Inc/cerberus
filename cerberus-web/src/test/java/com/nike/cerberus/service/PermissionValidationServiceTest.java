package com.nike.cerberus.service;

import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.SecureDataAction;
import com.nike.cerberus.dao.PermissionsDao;
import com.nike.cerberus.domain.SafeDepositBoxV2;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.SdbAccessRequest;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

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
    Set<String> userGroups = new HashSet<>();
    userGroups.add("userGroup1");
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
    Set<String> userGroups = new HashSet<>();
    userGroups.add("userGroup1");
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
