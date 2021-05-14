package com.nike.cerberus.audit.logger.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.nike.cerberus.audit.logger.AthenaClientFactory;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

public class AthenaServiceTest {

  private AthenaClientFactory athenaClientFactory;

  private AthenaService athenaService;

  @Mock private Set<String> partitions = new HashSet<>();

  @Before
  public void before() {
    athenaClientFactory = mock(AthenaClientFactory.class);
    athenaService = new AthenaService("fake-bucket", athenaClientFactory);
    Whitebox.setInternalState(athenaService, "partitions", partitions);
  }

  @Test
  public void test_that_addPartition_works() {
    String awsRegion = "us-west-2";
    AmazonAthena athena = mock(AmazonAthena.class);
    when(athenaClientFactory.getClient(awsRegion)).thenReturn(athena);
    when(athena.startQueryExecution(Mockito.any()))
        .thenReturn(new StartQueryExecutionResult().withQueryExecutionId("query-execution-id"));
    athenaService.addPartitionIfMissing(awsRegion, "fake-bucket", "2018", "01", "29", "12");
    verify(athenaClientFactory, times(1)).getClient(anyString());
    assertEquals(1, partitions.size());
  }

  @Test
  public void test_addPartition_already_exist() {
    String awsRegion = "us-west-2";
    String partition = String.format("year=%s/month=%s/day=%s/hour=%s", "2018", "01", "29", "12");
    Set<String> pars = new HashSet<>();
    pars.add(partition);
    Whitebox.setInternalState(athenaService, "partitions", pars);
    athenaService.addPartitionIfMissing(awsRegion, "fake-bucket", "2018", "01", "29", "12");
    verify(athenaClientFactory, never()).getClient(anyString());
    assertEquals(1, pars.size());
  }

  @Test
  public void test_addPartition_fails_when_amazon_client_exception() {
    String awsRegion = "us-west-2";
    AmazonAthena athena = mock(AmazonAthena.class);
    when(athenaClientFactory.getClient(awsRegion)).thenReturn(athena);
    when(athena.startQueryExecution(Mockito.any())).thenThrow(AmazonClientException.class);
    athenaService.addPartitionIfMissing(awsRegion, "fake-bucket", "2018", "01", "29", "12");
    verify(athenaClientFactory, times(1)).getClient(anyString());
    assertEquals(0, partitions.size());
  }
}
