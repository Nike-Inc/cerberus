package com.nike.cerberus.controller;

import org.junit.Assert;
import org.junit.Test;

public class DashboardControllerTest {

  @Test
  public void testRoot() {
    String root = new DashboardController().root();
    Assert.assertEquals("redirect:/dashboard/index.html", root);
  }
}
