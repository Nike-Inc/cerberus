package com.nike.cerberus;

import com.nike.cerberus.config.ApplicationConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Map;

/**
 * Main entry point for Cerberus Web Application
 */
public class Main {
  public static void main(String... args) {
    new SpringApplicationBuilder()
      .properties(Map.of(
        "spring.config.additional-location", "${user.home}/.cerberus/",
        "spring.application.name", "cerberus",
        "spring.config.name", "cerberus",
        "spring.profiles.active", "${cerberus.environment:local}"
      )).sources(ApplicationConfiguration.class).run(args);
  }
}
