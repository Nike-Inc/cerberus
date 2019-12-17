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

package com.nike.cerberus.service;

import com.google.common.collect.Lists;
import com.nike.cerberus.dao.CategoryDao;
import com.nike.cerberus.domain.Category;
import com.nike.cerberus.record.CategoryRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for interacting with categories */
@Component
public class CategoryService {

  private final CategoryDao categoryDao;

  private final UuidSupplier uuidSupplier;

  private final Slugger slugger;

  private final DateTimeSupplier dateTimeSupplier;

  @Autowired
  public CategoryService(
      final CategoryDao categoryDao,
      final UuidSupplier uuidSupplier,
      final Slugger slugger,
      final DateTimeSupplier dateTimeSupplier) {
    this.categoryDao = categoryDao;
    this.uuidSupplier = uuidSupplier;
    this.slugger = slugger;
    this.dateTimeSupplier = dateTimeSupplier;
  }

  /**
   * Retrieves all categories from the data store and returns them.
   *
   * @return List of category domain objects.
   */
  public List<Category> getAllCategories() {
    final List<CategoryRecord> records = categoryDao.getAllCategories();
    final List<Category> categories = Lists.newArrayListWithCapacity(records.size());

    records.forEach(
        r -> {
          final Category category = new Category();
          categories.add(
              category
                  .setId(r.getId())
                  .setPath(r.getPath())
                  .setDisplayName(r.getDisplayName())
                  .setCreatedBy(r.getCreatedBy())
                  .setLastUpdatedBy(r.getLastUpdatedBy())
                  .setCreatedTs(r.getCreatedTs())
                  .setLastUpdatedTs(r.getLastUpdatedTs()));
        });

    return categories;
  }

  /**
   * Retrieves the specific category by ID.
   *
   * @param id The identifier for the category to retrieve.
   * @return The category, if it exists.
   */
  public Optional<Category> getCategory(final String id) {
    final Optional<CategoryRecord> record = categoryDao.getCategory(id);

    if (record.isPresent()) {
      final Category category =
          new Category()
              .setId(record.get().getId())
              .setPath(record.get().getPath())
              .setDisplayName(record.get().getDisplayName())
              .setCreatedBy(record.get().getCreatedBy())
              .setLastUpdatedBy(record.get().getLastUpdatedBy())
              .setCreatedTs(record.get().getCreatedTs())
              .setLastUpdatedTs(record.get().getLastUpdatedTs());
      return Optional.of(category);
    }

    return Optional.empty();
  }

  /**
   * Creates the new category.
   *
   * @param category Category to be created
   * @return ID for the new category
   */
  @Transactional
  public String createCategory(final Category category, final String user) {
    final OffsetDateTime now = dateTimeSupplier.get();
    final CategoryRecord record = new CategoryRecord();
    record
        .setId(uuidSupplier.get())
        .setPath(slugger.toSlug(category.getDisplayName()))
        .setDisplayName(category.getDisplayName())
        .setCreatedBy(user)
        .setLastUpdatedBy(user)
        .setCreatedTs(now)
        .setLastUpdatedTs(now);
    categoryDao.createCategory(record);
    return record.getId();
  }

  /**
   * Deletes the category specified. If the category is associated to any safe deposit boxes or
   * doesn't exist, the call will fail.
   *
   * @param id ID of category to be deleted
   * @return If category was deleted
   */
  @Transactional
  public boolean deleteCategory(final String id) {
    final int count = categoryDao.deleteCategory(id);
    return count > 0;
  }

  /**
   * Collects all the categories and creates an id to name map
   *
   * @return map of category ids to category name
   */
  public Map<String, String> getCategoryIdToCategoryNameMap() {
    List<CategoryRecord> categoryRecords = categoryDao.getAllCategories();
    Map<String, String> catIdToStringMap = new HashMap<>(categoryRecords.size());
    categoryRecords.forEach(
        categoryRecord ->
            catIdToStringMap.put(categoryRecord.getId(), categoryRecord.getDisplayName()));
    return catIdToStringMap;
  }

  public Optional<String> getCategoryIdByName(String categoryName) {
    return Optional.ofNullable(categoryDao.getCategoryIdByName(categoryName));
  }
}
