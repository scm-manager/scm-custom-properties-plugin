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
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;

public class ConfigService {

  private static final String GLOBAL_CONFIG_STORE_NAME = "custom-properties-config";

  private final ConfigurationStore<GlobalConfig> globalConfigStore;

  @Inject
  public ConfigService(ConfigurationStoreFactory configurationStoreFactory) {
    this.globalConfigStore = configurationStoreFactory
      .withType(GlobalConfig.class)
      .withName(GLOBAL_CONFIG_STORE_NAME)
      .build();
  }

  public GlobalConfig getGlobalConfig() {
    return globalConfigStore.getOptional().orElseGet(GlobalConfig::new);
  }

  public void setGlobalConfig(GlobalConfig globalConfig) {
    globalConfigStore.set(globalConfig);
  }
}
