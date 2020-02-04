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

package com.nike.cerberus.util;

import com.nike.cerberus.security.CerberusPrincipal;
import java.util.Optional;
import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * We create this proxy bean and set request state to it during the authentication / authorization
 * checks. So that we don't have to make duplicated db calls in the controllers.
 */
@Data
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SdbAccessRequest {
  private String category;
  private String sdbSlug;
  private String sdbId;
  private String subPath;
  private CerberusPrincipal principal;

  public String getPath() {
    return String.format("%s/%s", sdbSlug, Optional.ofNullable(subPath).orElse(""));
  }
}
