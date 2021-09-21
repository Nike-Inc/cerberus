package com.nike.cerberus.controller;

import static com.nike.cerberus.security.CerberusPrincipal.ROLE_USER;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.nike.cerberus.service.FeatureFlagServiceV1;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API controller for accessing the state of feature-flag configuration items. Used by the UI to get
 * selective access to the environment-aware Spring config, as to keep all such items specified in
 * one place.
 */
@Slf4j
@RestController
@RequestMapping("/v1/feature-flag")
public class FeatureFlagControllerV1 {

  private final FeatureFlagServiceV1 featureFlagService;

  @Autowired
  public FeatureFlagControllerV1(FeatureFlagServiceV1 ffService) {
    this.featureFlagService = ffService;
  }

  /**
   * Provides a flat map of all exposed feature flags.
   *
   * @return Map of all feature flags
   */
  @RolesAllowed(ROLE_USER)
  @RequestMapping(method = GET)
  public Map<String, String> getAllFeatureFlags() {
    return this.featureFlagService.getAllFeatureFlags();
  }
}
