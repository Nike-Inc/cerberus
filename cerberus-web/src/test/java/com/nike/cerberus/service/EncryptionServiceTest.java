/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.service;

import static org.junit.Assert.assertTrue;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.google.common.collect.Lists;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class EncryptionServiceTest {

  @Test
  public void test_that_provider_has_current_region_first() {
    String arns =
        "arn:aws:kms:us-east-1:11111111:key/1111111-1d89-43ce-957b-0f705990e9d0,arn:aws:kms:us-west-2:11111111:key/11111111-aecd-4089-85e0-18536efa5c90";
    List<String> list =
        EncryptionService.getSortedArnListByCurrentRegion(
            Lists.newArrayList(StringUtils.split(arns, ",")), Region.getRegion(Regions.US_WEST_2));
    assertTrue(list.get(0).contains(Regions.US_WEST_2.getName()));
  }
}
