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

package com.nike.cerberus.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.nike.cerberus.mapper.CategoryMapper;
import com.nike.cerberus.record.CategoryRecord;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class CategoryDaoTest {

  private final String categoryId = "CATEGORY_ID";

  private final String displayName = "DISPLAY_NAME";

  private final String path = "PATH";

  private final String createdBy = "system";

  private final String lastUpdatedBy = "system";

  private final OffsetDateTime createdTs = OffsetDateTime.now(ZoneId.of("UTC"));

  private final OffsetDateTime lastUpdatedTs = OffsetDateTime.now(ZoneId.of("UTC"));

  private final CategoryRecord categoryRecord =
      new CategoryRecord()
          .setId(categoryId)
          .setDisplayName(displayName)
          .setPath(path)
          .setCreatedBy(createdBy)
          .setLastUpdatedBy(lastUpdatedBy)
          .setCreatedTs(createdTs)
          .setLastUpdatedTs(lastUpdatedTs);

  private final List<CategoryRecord> categoryRecordList = Lists.newArrayList(categoryRecord);

  private CategoryMapper categoryMapper;

  private CategoryDao subject;

  @Before
  public void setUp() throws Exception {
    categoryMapper = mock(CategoryMapper.class);
    subject = new CategoryDao(categoryMapper);
  }

  @Test
  public void getAllCategories_returns_list_of_records() {
    when(categoryMapper.getAllCategories()).thenReturn(categoryRecordList);

    List<CategoryRecord> actual = subject.getAllCategories();

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(categoryRecordList);
  }

  @Test
  public void getCategory_returns_record_when_found() {
    when(categoryMapper.getCategoryById(categoryId)).thenReturn(categoryRecord);

    final Optional<CategoryRecord> actual = subject.getCategory(categoryId);

    assertThat(actual.isPresent()).isTrue();
    assertThat(actual.get()).isEqualTo(categoryRecord);
  }

  @Test
  public void getCategory_returns_empty_when_record_not_found() {
    when(categoryMapper.getCategoryById(categoryId)).thenReturn(null);

    final Optional<CategoryRecord> actual = subject.getCategory(categoryId);

    assertThat(actual.isPresent()).isFalse();
  }

  @Test
  public void createCategory_returns_record_count() {
    final int recordCount = 1;
    when(categoryMapper.createCategory(categoryRecord)).thenReturn(recordCount);

    final int actualCount = subject.createCategory(categoryRecord);

    assertThat(actualCount).isEqualTo(recordCount);
  }

  @Test
  public void deleteCategory_returns_record_count() {
    final int recordCount = 1;
    when(categoryMapper.deleteCategory(categoryId)).thenReturn(recordCount);

    final int actualCount = subject.deleteCategory(categoryId);

    assertThat(actualCount).isEqualTo(recordCount);
  }
}
