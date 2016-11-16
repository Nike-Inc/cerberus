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

package com.nike.cerberus.vault;

import com.nike.vault.client.UrlResolver;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Resolves the vault URL from Guice.
 */
public class CmsVaultUrlResolver implements UrlResolver {

    private final String vaultAddr;

    @Inject
    public CmsVaultUrlResolver(@Named("vault.addr") final String vaultAddr) {
        this.vaultAddr = vaultAddr;
    }

    @Nullable
    @Override
    public String resolve() {
        return vaultAddr;
    }
}
