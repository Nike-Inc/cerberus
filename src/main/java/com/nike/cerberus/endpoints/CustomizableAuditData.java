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

package com.nike.cerberus.endpoints;

public class CustomizableAuditData {

    private String description;
    private String sdbNameSlug;

    public String getDescription() {
        return description;
    }

    public CustomizableAuditData setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getSdbNameSlug() {
        return sdbNameSlug;
    }

    public CustomizableAuditData setSdbNameSlug(String sdbNameSlug) {
        this.sdbNameSlug = sdbNameSlug;
        return this;
    }
}
