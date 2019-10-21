package com.nike.cerberus.server.config.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * This is needed for Classes created outside our normal process that can be created with Guice such as MyBatis caches.
 */
public class StaticInjector {

  @Inject static Injector injector;

  public static Injector getInstance() {
    return injector;
  }
}
