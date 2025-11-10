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
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.store.InMemoryByteConfigurationStoreFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class ConfigServiceTest {

  private final Repository REPOSITORY = RepositoryTestData.create42Puzzle();
  private ConfigService configService;

  @BeforeEach
  void setUp() {
    ConfigurationStoreFactory configurationStoreFactory = new InMemoryByteConfigurationStoreFactory();
    configService = new ConfigService(configurationStoreFactory);
  }

  @Nested
  class GlobalConfigTest {

    @Test
    void shouldSetAndGetConfig() {
      GlobalConfig globalConfig = new GlobalConfig();
      globalConfig.setEnabled(false);
      globalConfig.setEnableNamespaceConfig(false);
      globalConfig.setPredefinedKeys(
        Map.of(
          "lang", new PredefinedKey(List.of("Java", "TypeScript")),
          "arbitrary", new PredefinedKey(List.of())
        )
      );

      configService.setGlobalConfig(globalConfig);
      GlobalConfig result = configService.getGlobalConfig();

      assertThat(result.isEnabled()).isFalse();
      assertThat(result.isEnableNamespaceConfig()).isFalse();
      assertThat(result.getPredefinedKeys()).containsOnly(
        entry("lang", new PredefinedKey(List.of("Java", "TypeScript"))),
        entry("arbitrary", new PredefinedKey(List.of()))
      );
    }

    @Test
    void shouldGetDefaultConfig() {
      GlobalConfig result = configService.getGlobalConfig();
      assertThat(result.isEnabled()).isTrue();
      assertThat(result.isEnableNamespaceConfig()).isTrue();
      assertThat(result.getPredefinedKeys()).isEmpty();
    }
  }

  @Nested
  class NamespaceConfigTest {

    @Test
    void shouldSetAndGetConfig() {
      NamespaceConfig namespaceConfig = new NamespaceConfig();
      namespaceConfig.setPredefinedKeys(
        Map.of(
          "lang", new PredefinedKey(List.of("Java", "TypeScript")),
          "arbitrary", new PredefinedKey(List.of())
        )
      );

      configService.setNamespaceConfig("Kanto", namespaceConfig);
      NamespaceConfig result = configService.getNamespaceConfig("Kanto");

      assertThat(result.getPredefinedKeys()).containsOnly(
        entry("lang", new PredefinedKey(List.of("Java", "TypeScript"))),
        entry("arbitrary", new PredefinedKey(List.of()))
      );
    }

    @Test
    void shouldGetDefaultConfig() {
      NamespaceConfig result = configService.getNamespaceConfig("Kanto");

      assertThat(result.getPredefinedKeys()).isEmpty();
    }
  }

  @Nested
  class GetAllPredefinedKeys {

    @Test
    void shouldGetAllPredefinedKeysDefinedGloballyAndOnNamespaceLevel() {
      GlobalConfig globalConfig = new GlobalConfig();
      globalConfig.setPredefinedKeys(
        Map.of(
          "lang", new PredefinedKey(List.of("Java", "TypeScript"))
        )
      );
      configService.setGlobalConfig(globalConfig);

      NamespaceConfig namespaceConfig = new NamespaceConfig();
      namespaceConfig.setPredefinedKeys(
        Map.of(
          "arbitrary", new PredefinedKey(List.of())
        )
      );
      configService.setNamespaceConfig(REPOSITORY.getNamespace(), namespaceConfig);

      Map<String, PredefinedKey> result = configService.getAllPredefinedKeys(REPOSITORY);
      assertThat(result).containsOnly(
        entry("lang", new PredefinedKey(List.of("Java", "TypeScript"))),
        entry("arbitrary", new PredefinedKey(List.of()))
      );
    }

    @Test
    void shouldNotGetNamespaceKeysBecauseItsDisabledOnGlobalLevel() {
      GlobalConfig globalConfig = new GlobalConfig();
      globalConfig.setPredefinedKeys(
        Map.of(
          "lang", new PredefinedKey(List.of("Java", "TypeScript"))
        )
      );
      globalConfig.setEnableNamespaceConfig(false);
      configService.setGlobalConfig(globalConfig);

      NamespaceConfig namespaceConfig = new NamespaceConfig();
      namespaceConfig.setPredefinedKeys(
        Map.of(
          "arbitrary", new PredefinedKey(List.of())
        )
      );
      configService.setNamespaceConfig(REPOSITORY.getNamespace(), namespaceConfig);

      Map<String, PredefinedKey> result = configService.getAllPredefinedKeys(REPOSITORY);
      assertThat(result).containsOnly(
        entry("lang", new PredefinedKey(List.of("Java", "TypeScript")))
      );
    }

    @Test
    void shouldOverrideGlobalKeyIfNamespaceDefinesSameKey() {
      GlobalConfig globalConfig = new GlobalConfig();
      globalConfig.setPredefinedKeys(
        Map.of(
          "lang", new PredefinedKey(List.of("Java", "TypeScript"))
        )
      );
      globalConfig.setEnableNamespaceConfig(true);
      configService.setGlobalConfig(globalConfig);

      NamespaceConfig namespaceConfig = new NamespaceConfig();
      namespaceConfig.setPredefinedKeys(
        Map.of(
          "lang", new PredefinedKey(List.of("Rust", "Go"))
        )
      );
      configService.setNamespaceConfig(REPOSITORY.getNamespace(), namespaceConfig);

      Map<String, PredefinedKey> result = configService.getAllPredefinedKeys(REPOSITORY);
      assertThat(result).containsOnly(
        entry("lang", new PredefinedKey(List.of("Rust", "Go")))
      );
    }
  }
}
