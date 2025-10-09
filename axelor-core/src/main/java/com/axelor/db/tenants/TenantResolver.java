/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tenants;

import com.axelor.common.StringUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/** The tenant identifier resolver. */
public class TenantResolver implements CurrentTenantIdentifierResolver<String> {

  static final ThreadLocal<String> CURRENT_HOST = new ThreadLocal<>();
  static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

  private static boolean enabled;

  public TenantResolver() {
    enabled = TenantModule.isEnabled();
  }

  public static void setCurrentTenant(String tenantId, String tenantHost) {
    if (!enabled) return;
    CURRENT_TENANT.set(tenantId);
    CURRENT_HOST.set(tenantHost);
  }

  public static String currentTenantIdentifier() {
    if (!enabled) return null;
    final String tenant = CURRENT_TENANT.get();
    return tenant == null ? TenantConfig.DEFAULT_TENANT_ID : tenant;
  }

  public static String currentTenantHost() {
    if (!enabled) return null;
    final String tenant = CURRENT_HOST.get();
    return tenant == null ? null : tenant;
  }

  public static Map<String, String> getTenants() {
    return getTenants(true);
  }

  public static Map<String, String> getTenants(boolean onlyVisible) {
    return configsToNames(getTenantConfigs(onlyVisible));
  }

  public static TenantInfo getTenantInfo() {
    return getTenantInfo(true);
  }

  public static TenantInfo getTenantInfo(boolean onlyVisible) {
    final Map<String, TenantConfig> configs = getTenantConfigs(onlyVisible);

    // Check if a single tenant was host-resolved.
    if (configs.size() == 1) {
      final TenantConfig config = configs.values().iterator().next();
      if (StringUtils.notBlank(config.getTenantHosts())) {
        return TenantInfo.single(config.getTenantId());
      }
    }

    // User-selectable tenants
    return TenantInfo.multiple(configsToNames(configs));
  }

  private static Map<String, TenantConfig> getTenantConfigs(boolean onlyVisible) {
    final Map<String, TenantConfig> map = new LinkedHashMap<>();
    if (enabled) {
      final TenantConfigProvider provider = TenantSupport.get().getConfigProvider();
      for (TenantConfig config : provider.findAll(TenantResolver.CURRENT_HOST.get())) {
        if (!Boolean.FALSE.equals(config.getActive())
            && (!onlyVisible || !Boolean.FALSE.equals(config.getVisible()))) {
          map.put(config.getTenantId(), config);
        }
      }
    }
    return map;
  }

  private static Map<String, String> configsToNames(Map<String, TenantConfig> configs) {
    return configs.values().stream()
        .collect(
            Collectors.toMap(
                TenantConfig::getTenantId,
                config ->
                    Optional.ofNullable(config.getTenantName()).orElseGet(config::getTenantId),
                (existing, replacement) -> existing,
                LinkedHashMap::new));
  }

  @Override
  public String resolveCurrentTenantIdentifier() {
    return currentTenantIdentifier();
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}
