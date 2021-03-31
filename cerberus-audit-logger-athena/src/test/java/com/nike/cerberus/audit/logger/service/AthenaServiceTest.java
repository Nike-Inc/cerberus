package com.nike.cerberus.audit.logger.service;

import static junit.framework.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import com.nike.cerberus.audit.logger.AthenaClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class AthenaServiceTest {

  AthenaClientFactory athenaClientFactory = new AthenaClientFactory();
  @Mock private AthenaService mockAthenaService;
  private AthenaService athenaService;

  @Before
  public void before() {
    initMocks(this);
    athenaService = new AthenaService("fake-bucket", athenaClientFactory);
  }

  @Test
  public void test_that_addPartition_works() {
    String fileName = "localhost-audit.2018-01-29_12-58.log.gz";
    try {
      athenaService.addPartitionIfMissing("us-west-2", "fake-bucket", "2018", "01", "29", "12");
    } catch (Exception e) {
      assertTrue(false);
    }
    assertTrue(true);
  }

  @Test(expected = NullPointerException.class)
  public void test_that_addPartition_fails() {
    String fileName = "localhost-audit.2018-01-29_12-58.log.gz";
    athenaService = new AthenaService("fake-bucket", null);
    athenaService.addPartitionIfMissing("us-west-2", "fake-bucket", "2018", "01", "29", "12");
  }
}
