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
 *
 */

package com.nike.cerberus.record;

import java.util.Set;

/**
 * Represents a category.
 */
public class SafeDepositBoxVersionRecord {

    private String sdbID;

    /**
     * The secure data version paths that exist for the SDB.
     */
    private Set<String> paths;

    public String getSdbID() {
        return sdbID;
    }

    public SafeDepositBoxVersionRecord setSdbID(String sdbID) {
        this.sdbID = sdbID;
        return this;
    }

    public Set<String> getPaths() {
        return paths;
    }

    public SafeDepositBoxVersionRecord setPaths(Set<String> paths) {
        this.paths = paths;
        return this;
    }
}
