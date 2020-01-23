/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.validation;

import java.util.Arrays;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/** Validator class for validating that a string matches at least one of the given regex patterns */
public class PatternListAnyMatchValidator
    implements ConstraintValidator<PatternListAnyMatch, String> {

  private Pattern[] patterns;

  @Override
  public void initialize(PatternListAnyMatch constraint) {
    String[] patterns = constraint.value();
    Pattern[] compiledPatterns = new Pattern[patterns.length];
    for (int i = 0; i < patterns.length; i++) {
      compiledPatterns[i] = Pattern.compile(patterns[i]);
    }

    this.patterns = compiledPatterns;
  }

  @Override
  public boolean isValid(String stringToMatch, ConstraintValidatorContext context) {
    return Arrays.stream(patterns).anyMatch(pattern -> pattern.matcher(stringToMatch).matches());
  }
}
