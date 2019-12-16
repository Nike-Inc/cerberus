package com.nike.cerberus.api

import org.testng.annotations.Test

import static com.nike.cerberus.api.CerberusApiActions.*

class CerberusDashboardApiTests {

    @Test
    void "test an unauthenticated user can load the dashboard page"() {
        def dashboardIndexHtmlURIs = ["/dashboard", "/dashboard/", "/", "/dashboard/index.html"]

        // make sure that all of the redirect and direct URLs work for the dashboard
        dashboardIndexHtmlURIs.each { loadDashboardIndexHtml(it) }

        // make sure that the dashboard can appropriately load it's version from CMS
        loadDashboardVersion()
    }
}
