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

package com.nike.cerberus.record;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Represents a category.
 */
public class CategoryRecord {

    private String id;

    private String displayName;

    private String path;

    private OffsetDateTime createdTs;

    private OffsetDateTime lastUpdatedTs;

    private String createdBy;

    private String lastUpdatedBy;

    public String getId() {
        return id;
    }

    public CategoryRecord setId(String id) {
        this.id = id;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CategoryRecord setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getPath() {
        return path;
    }

    public CategoryRecord setPath(String path) {
        this.path = path;
        return this;
    }

    public OffsetDateTime getCreatedTs() {
        return createdTs;
    }

    public CategoryRecord setCreatedTs(OffsetDateTime createdTs) {
        this.createdTs = createdTs;
        return this;
    }

    public OffsetDateTime getLastUpdatedTs() {
        return lastUpdatedTs;
    }

    public CategoryRecord setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
        this.lastUpdatedTs = lastUpdatedTs;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public CategoryRecord setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public CategoryRecord setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryRecord that = (CategoryRecord) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(path, that.path) &&
                Objects.equals(createdTs, that.createdTs) &&
                Objects.equals(lastUpdatedTs, that.lastUpdatedTs) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(lastUpdatedBy, that.lastUpdatedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, path, createdTs, lastUpdatedTs, createdBy, lastUpdatedBy);
    }
}
