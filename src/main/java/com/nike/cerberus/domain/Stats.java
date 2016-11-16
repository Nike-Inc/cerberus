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

package com.nike.cerberus.domain;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Stats for safe deposit box meta data.
 */
public class Stats {

    private Set<SafeDepositBoxStats> safeDepositBoxStats = new HashSet<>();

    private OffsetDateTime generatedTs;

    public long getSafeDepositBoxTotal() {
        return this.safeDepositBoxStats.size();
    }

    public Set<SafeDepositBoxStats> getSafeDepositBoxStats() {
        return safeDepositBoxStats;
    }

    public Stats setSafeDepositBoxStats(Set<SafeDepositBoxStats> safeDepositBoxStats) {
        this.safeDepositBoxStats = safeDepositBoxStats;
        return this;
    }

    public OffsetDateTime getGeneratedTs() {
        return generatedTs;
    }

    public Stats setGeneratedTs(OffsetDateTime generatedTs) {
        this.generatedTs = generatedTs;
        return this;
    }
}
