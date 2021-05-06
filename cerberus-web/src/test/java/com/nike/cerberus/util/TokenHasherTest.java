package com.nike.cerberus.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Test;

public class TokenHasherTest {

  @Test
  public void testShouldThrowExceptionIfKeyLengthisConfiguredWrongly() {
    String sampleString =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Neque viverra justo nec ultrices dui sapien. A iaculis at erat pellentesque adipiscing commodo elit at imperdiet. Viverra justo nec ultrices dui sapien eget mi. Risus ultricies tristique nulla aliquet enim tortor. Consectetur libero id faucibus nisl tincidunt eget. Sed ullamcorper morbi tincidunt ornare massa eget egestas. Interdum varius sit amet mattis vulputate enim nulla aliquet porttitor. "
            + "Mauris in aliquam sem fringilla ut morbi tincidunt augue interdum. Id semper risus in hendrerit gravida. Mollis aliquam ut porttitor leo a.";
    byte[] encodedData = Base64.getEncoder().encode(sampleString.getBytes(StandardCharsets.UTF_8));
    String exceptionMessage = "";
    try {
      new TokenHasher(new String(encodedData, StandardCharsets.UTF_8), "algorithm", 4, 0);
    } catch (IllegalArgumentException illegalArgumentException) {
      exceptionMessage = illegalArgumentException.getMessage();
    }
    Assert.assertEquals(
        "${cerberus.auth.token.hash.keyLength} must be at least 256 but was 4", exceptionMessage);
  }

  @Test
  public void testShouldThrowExceptionIfIterationsConfiguredWrongly() {
    String sampleString =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Neque viverra justo nec ultrices dui sapien. A iaculis at erat pellentesque adipiscing commodo elit at imperdiet. Viverra justo nec ultrices dui sapien eget mi. Risus ultricies tristique nulla aliquet enim tortor. Consectetur libero id faucibus nisl tincidunt eget. Sed ullamcorper morbi tincidunt ornare massa eget egestas. Interdum varius sit amet mattis vulputate enim nulla aliquet porttitor. "
            + "Mauris in aliquam sem fringilla ut morbi tincidunt augue interdum. Id semper risus in hendrerit gravida. Mollis aliquam ut porttitor leo a.";
    byte[] encodedData = Base64.getEncoder().encode(sampleString.getBytes(StandardCharsets.UTF_8));
    String exceptionMessage = "";
    try {
      new TokenHasher(new String(encodedData, StandardCharsets.UTF_8), "algorithm", 256, 0);
    } catch (IllegalArgumentException illegalArgumentException) {
      exceptionMessage = illegalArgumentException.getMessage();
    }
    Assert.assertEquals(
        "${cerberus.auth.token.hash.iterations} must be at 100 but was 0", exceptionMessage);
  }

  @Test
  public void testShouldThrowExceptionIfHashSaltIsLength() {
    String sampleString = "leo a.";
    byte[] encodedData = Base64.getEncoder().encode(sampleString.getBytes(StandardCharsets.UTF_8));
    String exceptionMessage = "";
    try {
      new TokenHasher(new String(encodedData, StandardCharsets.UTF_8), "algorithm", 256, 0);
    } catch (IllegalArgumentException illegalArgumentException) {
      exceptionMessage = illegalArgumentException.getMessage();
    }
    Assert.assertEquals(
        "${cerberus.auth.token.hash.salt} must be at least 64 bytes but was 6", exceptionMessage);
  }

  @Test
  public void testHashToken() {
    String sampleString =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Neque viverra justo nec ultrices dui sapien. A iaculis at erat pellentesque adipiscing commodo elit at imperdiet. Viverra justo nec ultrices dui sapien eget mi. Risus ultricies tristique nulla aliquet enim tortor. Consectetur libero id faucibus nisl tincidunt eget. Sed ullamcorper morbi tincidunt ornare massa eget egestas. Interdum varius sit amet mattis vulputate enim nulla aliquet porttitor. "
            + "Mauris in aliquam sem fringilla ut morbi tincidunt augue interdum. Id semper risus in hendrerit gravida. Mollis aliquam ut porttitor leo a.";
    byte[] encodedData = Base64.getEncoder().encode(sampleString.getBytes(StandardCharsets.UTF_8));
    TokenHasher tokenHasher =
        new TokenHasher(
            new String(encodedData, StandardCharsets.UTF_8), "PBKDF2WithHmacSHA1", 256, 100);
    String token = tokenHasher.hashToken("token");
    Assert.assertNotNull(token);
  }
}
