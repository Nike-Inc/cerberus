package com.nike.cerberus.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service class that holds all the feature-flag configuration items that we wish to expose to the
 * UI.
 */
@Component
public class FeatureFlagServiceV1 {

  /**
   * The prefix that all AD Groups must start with, if any, for enabling enforcement of naming
   * conventions.
   *
   * <p>If this prefix is not set or is an empty string, Cerberus will perform no checking of AD
   * Group names.
   */
  private final String adGroupNamePrefix;

  /**
   * A banner message to be displayed at the top of the UI, for announcements on feature or service
   * agreement changes.
   */
  private final String bannerMessage;

  @Autowired
  public FeatureFlagServiceV1(String adGroupNamePrefix, String bannerMessage) {
    this.adGroupNamePrefix = adGroupNamePrefix;
    this.bannerMessage = bannerMessage;
  }

  /**
   * Provides a flat map of all exposed feature flags.
   *
   * @return Map of all feature flags
   */
  public Map<String, String> getAllFeatureFlags() {
    Map<String, String> flags = new HashMap<>();
    flags.put("adGroupNamePrefix", this.adGroupNamePrefix);
    flags.put("bannerMessage", this.bannerMessage);
    return flags;
  }
}
