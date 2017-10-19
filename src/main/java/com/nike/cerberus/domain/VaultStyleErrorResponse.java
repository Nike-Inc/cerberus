/*
 * Copyright (c) 2017 Nike, Inc.
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

import java.util.LinkedList;
import java.util.List;

public class VaultStyleErrorResponse {

    List<String> errors;

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }


    public static final class Builder {
        List<String> errors = new LinkedList<>();

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder withError(String error) {
            errors.add(error);
            return this;
        }

        public VaultStyleErrorResponse build() {
            VaultStyleErrorResponse vaultStyleErrorResponse = new VaultStyleErrorResponse();
            vaultStyleErrorResponse.setErrors(errors);
            return vaultStyleErrorResponse;
        }
    }
}
