package com.nike.cerberus.audit.logger;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.Assert;
import org.junit.Test;

public class S3ClientFactoryTest {

  @Test
  public void testS3ClientFactoryAlwaysReturnSameInstance() {
    S3ClientFactory s3ClientFactory = new S3ClientFactory();
    AmazonS3 s3Instance1 = s3ClientFactory.getClient("region-1");
    AmazonS3 s3Instance2 = s3ClientFactory.getClient("region-1");
    Assert.assertSame(s3Instance1, s3Instance2);
  }
}
