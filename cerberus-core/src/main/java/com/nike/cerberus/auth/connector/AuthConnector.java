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

package com.nike.cerberus.auth.connector;

import java.util.Map;
import java.util.Set;

public interface AuthConnector {

  AuthResponse authenticate(final String username, final String password);

  AuthResponse triggerChallenge(final String stateToken, final String deviceId);

  AuthResponse triggerPush(final String stateToken, final String deviceId);

  AuthResponse mfaCheck(final String stateToken, final String deviceId, final String otpToken);

  Set<String> getGroups(final AuthData data);

  Map<String, String> getValidatedUserPrincipal(String jwtString);
}
