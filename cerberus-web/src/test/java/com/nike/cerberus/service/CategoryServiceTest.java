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

package com.nike.cerberus.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.nike.cerberus.dao.CategoryDao;
import com.nike.cerberus.record.CategoryRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
import com.nike.cerberus.util.UuidSupplier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class CategoryServiceTest {

  @Mock private CategoryDao categoryDao;

  @Mock private UuidSupplier uuidSupplier;

  @Mock private Slugger slugger;

  @Mock private DateTimeSupplier dateTimeSupplier;

  @InjectMocks private CategoryService categoryService;

  @Before
  public void before() {
    initMocks(this);
  }

  @Test
  public void test_that_getCategoryIdToStringMap_returns_valid_map() {
    Map<String, String> expected = new HashMap<>();
    expected.put("abc", "foo");

    when(categoryDao.getAllCategories())
        .thenReturn(Arrays.asList(new CategoryRecord().setId("abc").setDisplayName("foo")));

    Map<String, String> actual = categoryService.getCategoryIdToCategoryNameMap();
    assertEquals(expected, actual);
  }
}
