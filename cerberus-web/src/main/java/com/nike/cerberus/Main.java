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

package com.nike.cerberus;

import com.nike.cerberus.config.ApplicationConfiguration;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Main entry point for Cerberus Web Application */
public class Main {
  public static void main(String... args) {
    new SpringApplicationBuilder()
        .properties(
            Map.of(
                "spring.config.additional-location", "optional:${user.home}/.cerberus/",
                "spring.application.name", "cerberus",
                "spring.config.name", "cerberus",
                "spring.profiles.active", "${cerberus.environment:local}"))
        .sources(ApplicationConfiguration.class)
        .run(args);
  }
}
