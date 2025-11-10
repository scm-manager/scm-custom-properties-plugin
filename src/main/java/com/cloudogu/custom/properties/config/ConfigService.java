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
    globalConfigStore.set(globalConfig);
  }

  public NamespaceConfig getNamespaceConfig(String namespace) {
    return getNamespaceConfigStore(namespace).getOptional().orElseGet(NamespaceConfig::new);
  }

  public void setNamespaceConfig(String namespace, NamespaceConfig namespaceConfig) {
    getNamespaceConfigStore(namespace).set(namespaceConfig);
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
