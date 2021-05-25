package com.nike.cerberus.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.mock;

import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.UuidSupplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AwsIamRoleServiceTest {

  private AwsIamRoleService awsIamRoleService;
  private AwsIamRoleDao awsIamRoleDao;
  private UuidSupplier uuidSupplier;
  private DateTimeSupplier dateTimeSupplier;

  @Before
  public void setUp() {
    awsIamRoleDao = mock(AwsIamRoleDao.class);
    uuidSupplier = new UuidSupplier();
    dateTimeSupplier = new DateTimeSupplier();
    awsIamRoleService = new AwsIamRoleService(awsIamRoleDao, uuidSupplier, dateTimeSupplier);
  }

  // To test create Iam Role
  @Test
  public void test_createIamRole() {
    Mockito.when(awsIamRoleDao.createIamRole(anyObject())).thenReturn(1);
    AwsIamRoleRecord awsIamRoleRecord = awsIamRoleService.createIamRole("iamPrincipalArn");
    assertEquals(awsIamRoleRecord.getAwsIamRoleArn(), "iamPrincipalArn");
  }
}
