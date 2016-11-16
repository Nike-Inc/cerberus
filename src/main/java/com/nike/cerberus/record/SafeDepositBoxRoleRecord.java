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

/**
 * POJO for representing the safe deposit box to role association.  This is used to generate a list of records
 * showing what safe deposit boxes a user has access to and with what role.
 */
public class SafeDepositBoxRoleRecord {

    private String safeDepositBoxName;

    private String roleName;

    public String getSafeDepositBoxName() {
        return safeDepositBoxName;
    }

    public SafeDepositBoxRoleRecord setSafeDepositBoxName(String safeDepositBoxName) {
        this.safeDepositBoxName = safeDepositBoxName;
        return this;
    }

    public String getRoleName() {
        return roleName;
    }

    public SafeDepositBoxRoleRecord setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }
}
