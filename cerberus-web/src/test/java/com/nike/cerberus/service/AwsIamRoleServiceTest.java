package com.nike.cerberus.service;

import static com.nike.cerberus.service.AuthenticationService.SYSTEM_USER;

import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AwsIamRoleServiceTest {

  @Mock private AwsIamRoleDao awsIamRoleDao;
  @Mock private UuidSupplier uuidSupplier;
  @Mock private DateTimeSupplier dateTimeSupplier;

  @InjectMocks private AwsIamRoleService awsIamRoleService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void checkCreateIamRole() {
    Mockito.when(dateTimeSupplier.get()).thenReturn(OffsetDateTime.MAX);
    Mockito.when(uuidSupplier.get()).thenReturn("UUID");
    AwsIamRoleRecord awsIamRoleRecord = awsIamRoleService.createIamRole("iamPrincipalArn");
    Mockito.verify(awsIamRoleDao).createIamRole(awsIamRoleRecord);
    Assert.assertEquals("UUID", awsIamRoleRecord.getId());
    Assert.assertEquals("iamPrincipalArn", awsIamRoleRecord.getAwsIamRoleArn());
    Assert.assertEquals(SYSTEM_USER, awsIamRoleRecord.getCreatedBy());
    Assert.assertEquals(SYSTEM_USER, awsIamRoleRecord.getLastUpdatedBy());
    Assert.assertEquals(OffsetDateTime.MAX, awsIamRoleRecord.getCreatedTs());
    Assert.assertEquals(OffsetDateTime.MAX, awsIamRoleRecord.getLastUpdatedTs());
  }
}
