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
import com.nike.cerberus.domain.Category;
import com.nike.cerberus.record.CategoryRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

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

  @Test
  public void testGetAllCategoriesWhenNoCategoriesPresent() {
    Mockito.when(categoryDao.getAllCategories()).thenReturn(Collections.emptyList());
    List<Category> allCategories = categoryService.getAllCategories();
    Assert.assertTrue(allCategories.isEmpty());
  }

  @Test
  public void testGetAllCategoriesWhenCategoriesPresent() {
    CategoryRecord categoryRecord = constructCategoryRecord();
    List<CategoryRecord> categoryRecords = new ArrayList<>();
    categoryRecords.add(categoryRecord);
    Mockito.when(categoryDao.getAllCategories()).thenReturn(categoryRecords);
    List<Category> allCategories = categoryService.getAllCategories();
    Assert.assertEquals(1, allCategories.size());
    Assert.assertEquals(categoryRecord, categoryRecords.get(0));
  }

  @Test
  public void testCategoryByIdWhenCategoryIsNotPresent() {
    Mockito.when(categoryDao.getCategory("id")).thenReturn(Optional.empty());
    Optional<Category> optionalCategory = categoryService.getCategory("id");
    Assert.assertFalse(optionalCategory.isPresent());
  }

  @Test
  public void testCateGoryByIdWhenCategoryIsPresent() {
    CategoryRecord categoryRecord = constructCategoryRecord();
    Mockito.when(categoryDao.getCategory("id")).thenReturn(Optional.of(categoryRecord));
    Optional<Category> optionalCategory = categoryService.getCategory("id");
    Assert.assertTrue(optionalCategory.isPresent());
    Category expectedCategory = createCategory();
    Category actualCategory = optionalCategory.get();
    Assert.assertEquals(expectedCategory.getId(), actualCategory.getId());
    Assert.assertEquals(expectedCategory.getDisplayName(), actualCategory.getDisplayName());
    Assert.assertEquals(expectedCategory.getPath(), actualCategory.getPath());
    Assert.assertEquals(expectedCategory.getCreatedBy(), actualCategory.getCreatedBy());
    Assert.assertEquals(expectedCategory.getCreatedTs(), actualCategory.getCreatedTs());
    Assert.assertEquals(expectedCategory.getLastUpdatedBy(), actualCategory.getLastUpdatedBy());
    Assert.assertEquals(expectedCategory.getLastUpdatedTs(), actualCategory.getLastUpdatedTs());
  }

  @Test
  public void testCreateCategory() {
    Category category = createCategory();
    Mockito.when(dateTimeSupplier.get()).thenReturn(OffsetDateTime.MAX);
    Mockito.when(dateTimeSupplier.get()).thenReturn(OffsetDateTime.MAX);
    Mockito.when(uuidSupplier.get()).thenReturn("id");
    String id = categoryService.createCategory(category, "user");
    Assert.assertEquals("id", id);
    CategoryRecord categoryRecord = constructCategoryRecord();
    categoryRecord.setLastUpdatedTs(OffsetDateTime.MAX);
    categoryRecord.setCreatedTs(OffsetDateTime.MAX);
    categoryRecord.setCreatedBy("user");
    categoryRecord.setLastUpdatedBy("user");
    categoryRecord.setPath("displayname");
    Mockito.verify(categoryDao).createCategory(categoryRecord);
  }

  @Test
  public void testDeleteCategoryWhenDeletedRecordsAreGraterThanOrEqualToOne() {
    Mockito.when(categoryDao.deleteCategory("id")).thenReturn(1);
    Assert.assertTrue(categoryService.deleteCategory("id"));
  }

  @Test
  public void testDeleteCategoryWhenDeletedRecordsAreEqualToZero() {
    Mockito.when(categoryDao.deleteCategory("id")).thenReturn(0);
    Assert.assertFalse(categoryService.deleteCategory("id"));
  }

  @Test
  public void testCategoryIdByNameWhenNoCategoryPresentWithTheGivenName() {
    Assert.assertFalse(categoryService.getCategoryIdByName("name").isPresent());
  }

  @Test
  public void testCategoryIdByNameWhenCategoryPresentWithTheGivenName() {
    Mockito.when(categoryDao.getCategoryIdByName("name")).thenReturn("name");
    Optional<String> categoryIdByName = categoryService.getCategoryIdByName("name");
    Assert.assertTrue(categoryIdByName.isPresent());
    Assert.assertEquals("name", categoryIdByName.get());
  }

  private CategoryRecord constructCategoryRecord() {
    CategoryRecord categoryRecord =
        new CategoryRecord()
            .setId("id")
            .setLastUpdatedBy("path")
            .setDisplayName("displayName")
            .setCreatedBy("createdBy")
            .setLastUpdatedBy("lastUpdateBy")
            .setCreatedTs(OffsetDateTime.MAX)
            .setLastUpdatedTs(OffsetDateTime.MAX)
            .setPath("path");
    return categoryRecord;
  }

  private Category createCategory() {
    Category category =
        new Category()
            .setCreatedBy("createdBy")
            .setCreatedTs(OffsetDateTime.MAX)
            .setPath("path")
            .setDisplayName("displayName")
            .setLastUpdatedBy("lastUpdateBy")
            .setLastUpdatedTs(OffsetDateTime.MAX)
            .setId("id");
    return category;
  }
}
