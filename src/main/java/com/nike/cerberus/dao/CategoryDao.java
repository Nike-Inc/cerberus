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

package com.nike.cerberus.dao;

import com.nike.cerberus.mapper.CategoryMapper;
import com.nike.cerberus.record.CategoryRecord;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for the category data.
 */
public class CategoryDao {

    private final CategoryMapper categoryMapper;

    @Inject
    public CategoryDao(final CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryRecord> getAllCategories() {
        return categoryMapper.getAllCategories();
    }

    public Optional<CategoryRecord> getCategory(final String id) {
        return Optional.ofNullable(categoryMapper.getCategoryById(id));
    }

    public int createCategory(final CategoryRecord record) {
        return categoryMapper.createCategory(record);
    }

    public int deleteCategory(final String id) {
        return categoryMapper.deleteCategory(id);
    }
}
