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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.store.InMemoryByteConfigurationStoreFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigServiceTest {

  @Nested
  class GlobalConfigTest {

    private ConfigService configService;

    @BeforeEach
    void setUp() {
      ConfigurationStoreFactory configurationStoreFactory = new InMemoryByteConfigurationStoreFactory();
      configService = new ConfigService(configurationStoreFactory);
    }

    @Test
    void shouldSetAndGetConfig() {
      GlobalConfig globalConfig = new GlobalConfig();
      globalConfig.setEnabled(false);

      configService.setGlobalConfig(globalConfig);
      GlobalConfig result = configService.getGlobalConfig();

      assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void shouldGetDefaultConfig() {
      GlobalConfig result = configService.getGlobalConfig();
      assertThat(result.isEnabled()).isTrue();
    }
  }
}
