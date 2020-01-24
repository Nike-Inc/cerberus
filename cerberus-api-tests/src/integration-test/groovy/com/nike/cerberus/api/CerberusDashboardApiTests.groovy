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

package com.nike.cerberus.api

import org.testng.annotations.Test

import static com.nike.cerberus.api.CerberusApiActions.*

class CerberusDashboardApiTests {

    @Test
    void "test an unauthenticated user can load the dashboard page"() {
        def dashboardIndexHtmlURIs = ["/dashboard", "/dashboard/", "/", "/dashboard/index.html"]

        // make sure that all of the redirect and direct URLs work for the dashboard
        dashboardIndexHtmlURIs.each { loadDashboardIndexHtml(it) }
    }
}
