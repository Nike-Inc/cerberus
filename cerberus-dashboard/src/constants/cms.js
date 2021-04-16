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

export const TOKEN_HEADER = 'X-Cerberus-Token'
export const USER_AUTH_PATH = '/v2/auth/user'
export const USER_AUTH_MFA_PATH = '/v2/auth/mfa_check'
export const USER_AUTH_PATH_REFRESH = '/v2/auth/user/refresh'
export const TOKEN_DELETE_PATH = '/v1/auth'
export const RETRIEVE_CATEGORY_PATH = '/v1/category'
export const RETRIEVE_ROLE_PATH = '/v1/role'
export const BUCKET_RESOURCE = '/v2/safe-deposit-box'
export const RETRIEVE_METADATA = '/v1/metadata'
export const MFA_REQUIRED_STATUS = 'mfa_req'
export const TOKEN_EXCHANGE_PATH = '/v2/auth/user/oauth/exchange'

export const SDB_NAME_MAX_LENGTH = 100
export const SDB_DESC_MAX_LENGTH = 1000