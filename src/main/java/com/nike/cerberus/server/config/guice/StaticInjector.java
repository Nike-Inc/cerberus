package com.nike.cerberus.server.config.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class StaticInjector {

  @Inject static Injector injector;

  public static Injector getInstance() {
    return injector;
  }
}
