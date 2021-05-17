package com.nike.cerberus.jobs;

import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.metric.MetricsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.service.UserGroupPermissionService;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class KpiMetricsProcessingJobTest {

  @Mock private MetricsService metricsService;

  @Mock private SafeDepositBoxService safeDepositBoxService;

  @Mock private AwsIamRoleDao awsIamRoleDao;

  @Mock private SecureDataService secureDataService;

  @Mock private UserGroupPermissionService userGroupPermissionService;

  @InjectMocks private KpiMetricsProcessingJob kpiMetricsProcessingJob;

  @Captor private ArgumentCaptor<Supplier<Number>> supplierArgumentCaptor;

  @Captor private ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testExecute() {
    int numberOfUniqueIamRoles = 1;
    int numUniqueOwnerGroups = 2;
    int numUniqueNonOwnerGroups = 3;
    int totalUniqueUserGroups = 4;
    int numSDBs = 5;
    int numDataNodes = 6;
    int numKeyValuePairs = 7;
    int numFiles = 8;
    Mockito.when(awsIamRoleDao.getTotalNumberOfUniqueIamRoles()).thenReturn(numberOfUniqueIamRoles);
    Mockito.when(userGroupPermissionService.getTotalNumUniqueOwnerGroups())
        .thenReturn(numUniqueOwnerGroups);
    Mockito.when(userGroupPermissionService.getTotalNumUniqueNonOwnerGroups())
        .thenReturn(numUniqueNonOwnerGroups);
    Mockito.when(userGroupPermissionService.getTotalNumUniqueUserGroups())
        .thenReturn(totalUniqueUserGroups);
    Mockito.when(safeDepositBoxService.getTotalNumberOfSafeDepositBoxes()).thenReturn(numSDBs);
    Mockito.when(secureDataService.getTotalNumberOfDataNodes()).thenReturn(numDataNodes);
    Mockito.when(secureDataService.getTotalNumberOfKeyValuePairs()).thenReturn(numKeyValuePairs);
    Mockito.when(secureDataService.getTotalNumberOfFiles()).thenReturn(numFiles);
    kpiMetricsProcessingJob.execute();
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("numberOfUniqueIamRoles"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(numberOfUniqueIamRoles, supplierArgumentCaptor.getValue().get());
    Assert.assertTrue(mapArgumentCaptor.getValue().isEmpty());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("numberOfUniqueOwnerGroups"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(numUniqueOwnerGroups, supplierArgumentCaptor.getValue().get());
    Assert.assertTrue(mapArgumentCaptor.getValue().isEmpty());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("numberOfUniqueNonOwnerGroups"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(numUniqueNonOwnerGroups, supplierArgumentCaptor.getValue().get());
    Assert.assertTrue(mapArgumentCaptor.getValue().isEmpty());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("totalUniqueUserGroups"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(totalUniqueUserGroups, supplierArgumentCaptor.getValue().get());
    Assert.assertTrue(mapArgumentCaptor.getValue().isEmpty());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("numberOfSdbs"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(numSDBs, supplierArgumentCaptor.getValue().get());
    Assert.assertTrue(mapArgumentCaptor.getValue().isEmpty());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("numberOfDataNodes"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(numDataNodes, supplierArgumentCaptor.getValue().get());
    Assert.assertTrue(mapArgumentCaptor.getValue().isEmpty());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("numberOfKeyValuePairs"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(numKeyValuePairs, supplierArgumentCaptor.getValue().get());
    Assert.assertTrue(mapArgumentCaptor.getValue().isEmpty());
    Mockito.verify(metricsService)
        .getOrCreateCallbackGauge(
            Mockito.eq("numberOfFiles"),
            supplierArgumentCaptor.capture(),
            mapArgumentCaptor.capture());
    Assert.assertEquals(numFiles, supplierArgumentCaptor.getValue().get());
    Assert.assertTrue(mapArgumentCaptor.getValue().isEmpty());
  }
}
