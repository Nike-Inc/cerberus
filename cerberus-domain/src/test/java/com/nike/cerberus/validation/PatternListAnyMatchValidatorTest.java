package com.nike.cerberus.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PatternListAnyMatchValidatorTest {
  PatternListAnyMatch patternListAnyMatch;
  PatternListAnyMatchValidator patternListAnyMatchValidator = new PatternListAnyMatchValidator();

  @Before
  public void setup() {
    patternListAnyMatch = Mockito.mock(PatternListAnyMatch.class);
  }

  @Test
  public void test_isValid() {
    Mockito.when(patternListAnyMatch.value()).thenReturn(new String[] {"\\d"});
    patternListAnyMatchValidator.initialize(patternListAnyMatch);
    Assert.assertTrue(patternListAnyMatchValidator.isValid("1", null));
    Assert.assertFalse(patternListAnyMatchValidator.isValid("s", null));
  }
}
