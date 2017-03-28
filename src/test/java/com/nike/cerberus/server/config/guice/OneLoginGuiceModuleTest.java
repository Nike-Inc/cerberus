package com.nike.cerberus.server.config.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.nike.cerberus.auth.connector.onelogin.OneLoginAuthConnector;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class OneLoginGuiceModuleTest {

    @Test
    public void sanityTest() {
        Injector injector = Guice.createInjector(new OneLoginGuiceModule(), new AbstractModule() {
            @Override
            protected void configure() {

                bindConstant().annotatedWith(Names.named("auth.connector.onelogin.api_region")).to("us");
                bindConstant().annotatedWith(Names.named("auth.connector.onelogin.client_id")).to("fake-client-id");
                bindConstant().annotatedWith(Names.named("auth.connector.onelogin.client_secret")).to("fake-client-secret");
                bindConstant().annotatedWith(Names.named("auth.connector.onelogin.subdomain")).to("fake-subdomain");

            }
        });

        OneLoginAuthConnector connector = injector.getInstance(OneLoginAuthConnector.class);
        assertNotNull(connector);
    }
}