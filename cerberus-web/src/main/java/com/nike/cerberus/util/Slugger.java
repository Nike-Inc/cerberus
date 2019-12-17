/*
 * Copyright (c) 2016 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.stereotype.Component;

/**
 * Naive utility for transforming strings into slugs (lower-case alphanumeric with dashes in place
 * of whitespace.
 */
@Component
public class Slugger {

  public static final Pattern NONLATIN = Pattern.compile("[^\\w-]");

  public static final Pattern WHITESPACE = Pattern.compile("[\\s]");

  public static String toSlug(String input) {
    final String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
    final String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
    final String slug = NONLATIN.matcher(normalized).replaceAll("");
    return slug.toLowerCase(Locale.ENGLISH);
  }

  public String slugifyKmsAliases(String input) {
    return RegExUtils.replacePattern(input, "[^a-zA-Z0-9:/_-]+", "-");
  }
}
