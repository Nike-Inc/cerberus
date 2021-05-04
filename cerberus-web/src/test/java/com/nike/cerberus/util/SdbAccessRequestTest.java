package com.nike.cerberus.util;

import org.junit.Assert;
import org.junit.Test;

public class SdbAccessRequestTest {

  @Test
  public void testGetPath() {
    SdbAccessRequest sdbAccessRequest = new SdbAccessRequest();
    sdbAccessRequest.setSdbSlug("sdbSlug");
    sdbAccessRequest.setSubPath("subPath");
    String actualPath = sdbAccessRequest.getPath();
    Assert.assertEquals("sdbSlug/subPath", actualPath);
  }

  @Test
  public void testGetPathWhenSubPathIsNull() {
    SdbAccessRequest sdbAccessRequest = new SdbAccessRequest();
    sdbAccessRequest.setSdbSlug("sdbSlug");
    String actualPath = sdbAccessRequest.getPath();
    Assert.assertEquals("sdbSlug/", actualPath);
  }
}
