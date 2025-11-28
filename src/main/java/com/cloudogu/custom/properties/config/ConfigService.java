/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.custom.properties.config;

import jakarta.inject.Inject;
import sonia.scm.repository.Repository;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConfigService {

  private static final String CONFIG_STORE_NAME = "custom-properties-config";

  private final ConfigurationStore<GlobalConfig> globalConfigStore;
  private final ConfigurationStoreFactory configStoreFactory;

  @Inject
  public ConfigService(ConfigurationStoreFactory configurationStoreFactory) {
    this.globalConfigStore = configurationStoreFactory
      .withType(GlobalConfig.class)
      .withName(CONFIG_STORE_NAME)
      .build();

    this.configStoreFactory = configurationStoreFactory;
  }

  public GlobalConfig getGlobalConfig() {
    return globalConfigStore.getOptional().orElseGet(GlobalConfig::new);
  }

  public void setGlobalConfig(GlobalConfig globalConfig) {
    findInvalidDefaultValue(globalConfig.getPredefinedKeys()).ifPresent(entry -> {
      throw new InvalidDefaultValueException(entry.getKey(), entry.getValue());
    });

    globalConfigStore.set(globalConfig);
  }

  public NamespaceConfig getNamespaceConfig(String namespace) {
    return getNamespaceConfigStore(namespace).getOptional().orElseGet(NamespaceConfig::new);
  }

  public void setNamespaceConfig(String namespace, NamespaceConfig namespaceConfig) {
    findInvalidDefaultValue(namespaceConfig.getPredefinedKeys()).ifPresent(entry -> {
      throw new InvalidDefaultValueException(namespace, entry.getKey(), entry.getValue());
    });

    getNamespaceConfigStore(namespace).set(namespaceConfig);
  }

  private Optional<Map.Entry<String, PredefinedKey>> findInvalidDefaultValue(Map<String, PredefinedKey> predefinedKeys) {
    return predefinedKeys
      .entrySet()
      .stream()
      .filter(entry -> !entry.getValue().isDefaultValueValid())
      .findFirst();
  }

  private ConfigurationStore<NamespaceConfig> getNamespaceConfigStore(String namespace) {
    return configStoreFactory
      .withType(NamespaceConfig.class)
      .withName(CONFIG_STORE_NAME)
      .forNamespace(namespace)
      .build();
  }

  public Map<String, PredefinedKey> getAllPredefinedKeys(Repository repository) {
    GlobalConfig globalConfig = getGlobalConfig();
    Map<String, PredefinedKey> result = new HashMap<>(globalConfig.getPredefinedKeys());

    if (globalConfig.isEnableNamespaceConfig()) {
      result.putAll(getNamespaceConfig(repository.getNamespace()).getPredefinedKeys());
    }

    return result;
  }
}
