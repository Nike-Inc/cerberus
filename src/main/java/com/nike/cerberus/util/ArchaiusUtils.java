package com.nike.cerberus.util;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.typesafe.config.Config;

import java.util.Properties;

public class ArchaiusUtils {

    /**
     * Hystrix expects configuration via Archaius so we initialize it here
     */
    public static void initializeArchiaus(Config appConfig) {
        // Initialize Archaius
        DynamicPropertyFactory.getInstance();
        // Load properties from Typesafe config for Hystrix, etc.
        ConfigurationManager.loadProperties(toProperties(appConfig));
    }

    public static void loadProperties(Properties properties) {
        ConfigurationManager.loadProperties(properties);
    }

    /**
     * Convert Typesafe config to Properties
     * From https://github.com/typesafehub/config/issues/357
     */
    public static Properties toProperties(Config config) {
        Properties properties = new Properties();
        config.entrySet().forEach(e -> properties.setProperty(e.getKey(), config.getString(e.getKey())));
        return properties;
    }
}
