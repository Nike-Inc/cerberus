package com.nike.cerberus.util;

import static junit.framework.TestCase.assertEquals;

import com.nike.backstopper.apierror.ApiError;
import com.nike.cerberus.error.DefaultApiError;
import org.junit.Test;

public class CustomApiErrorTest {

  @Test(expected = NullPointerException.class)
  public void test_creat_custom_api_error_fails() {
    CustomApiError.createCustomApiError(null, "message");
  }

  @Test
  public void test_creat_custom_api_error_works() {
    DefaultApiError err = DefaultApiError.SDB_UNIQUE_NAME;
    ApiError error = CustomApiError.createCustomApiError(err, "message");
    String actual = error.getMessage();
    String expected = err.getMessage() + " " + "message";
    assertEquals(expected, actual);
  }
}
