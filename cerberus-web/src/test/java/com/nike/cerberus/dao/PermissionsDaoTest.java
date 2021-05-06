package com.nike.cerberus.dao;

import com.nike.cerberus.mapper.PermissionsMapper;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PermissionsDaoTest {

  @Mock private PermissionsMapper permissionsMapper;

  @InjectMocks private PermissionsDao permissionsDao;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testDoesAssumedRoleHaveRoleForSdb() {
    permissionsDao.doesAssumedRoleHaveRoleForSdb(
        "sdbId", "assumedRoleArn", "iamRoleArn", "iamRootArn", Collections.EMPTY_SET);
    Mockito.verify(permissionsMapper)
        .doesAssumedRoleHaveGivenRoleForSdb(
            "sdbId", "assumedRoleArn", "iamRoleArn", "iamRootArn", Collections.EMPTY_SET);
  }

  @Test
  public void testDoesIamPrincipalHaveRoleForSdb() {
    permissionsDao.doesIamPrincipalHaveRoleForSdb(
        "sdbId", "iamPrincipalArn", "iamRootArn", Collections.EMPTY_SET);
    Mockito.verify(permissionsMapper)
        .doesIamPrincipalHaveGivenRoleForSdb(
            "sdbId", "iamPrincipalArn", "iamRootArn", Collections.EMPTY_SET);
  }

  @Test
  public void testDoesUserPrincipalHaveRoleForSdb() {
    permissionsDao.doesUserPrincipalHaveRoleForSdb(
        "sdbId", Collections.EMPTY_SET, Collections.EMPTY_SET);
    Mockito.verify(permissionsMapper)
        .doesUserPrincipalHaveGivenRoleForSdb(
            "sdbId", Collections.EMPTY_SET, Collections.EMPTY_SET);
  }

  @Test
  public void testDoesUserHavePermsForRoleAndSdbCaseInsensitive() {
    permissionsDao.doesUserHavePermsForRoleAndSdbCaseInsensitive(
        "sdbId", Collections.EMPTY_SET, Collections.EMPTY_SET);
    Mockito.verify(permissionsMapper)
        .doesUserHavePermsForRoleAndSdbCaseInsensitive(
            "sdbId", Collections.EMPTY_SET, Collections.EMPTY_SET);
  }
}
