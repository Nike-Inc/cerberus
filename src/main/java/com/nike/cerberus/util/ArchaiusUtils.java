package com.nike.cerberus.util;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.LoggerFactory;

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
        config.entrySet().forEach(prop -> {
            try {
                properties.setProperty(prop.getKey(), config.getString(prop.getKey()));
            } catch (ConfigException e) { // Not everything in a type safe config can be retrieved as a string
                LoggerFactory.getLogger("com.nike.cerberus.util.ArchaiusUtils")
                        .error("Failed to process prop: {} with value: {} as a string", prop.getKey(), prop.getValue());
            }
        });
        return properties;
    }
}
