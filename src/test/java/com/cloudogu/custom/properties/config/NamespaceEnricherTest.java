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

import com.google.inject.util.Providers;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.Namespace;

import java.net.URI;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware("Trainer Red")
class NamespaceEnricherTest {

  private final String domain = "https://scm-test.de/scm/api/";
  private final Namespace namespace = new Namespace("hitchhiker");

  private NamespaceEnricher enricher;
  @Mock
  private ConfigService configService;

  @Mock
  private HalAppender appender;

  @Mock
  private HalEnricherContext context;

  @BeforeEach
  void setup() {
    ScmPathInfoStore pathInfoStore = new ScmPathInfoStore();
    pathInfoStore.set(() -> URI.create(domain));

    enricher = new NamespaceEnricher(Providers.of(pathInfoStore), configService);

    when(context.oneRequireByType(Namespace.class)).thenReturn(namespace);
  }

  @Test
  void shouldNotEnrichBecausePermissionIsMissing() {
    enricher.enrich(context, appender);
    verifyNoInteractions(appender);
  }

  @Test
  @SubjectAware(permissions = {"namespace:customProperties:hitchhiker"})
  void shouldNotEnrichBecauseFeatureIsDisabled() {
    GlobalConfig disabledConfig = new GlobalConfig();
    disabledConfig.setEnabled(false);
    when(configService.getGlobalConfig()).thenReturn(disabledConfig);

    enricher.enrich(context, appender);
    verifyNoInteractions(appender);
  }

  @Test
  @SubjectAware(permissions = {"namespace:customProperties:hitchhiker"})
  void shouldNotEnrichBecauseNamespaceConfigIsDisabledByGlobalConfig() {
    GlobalConfig disabledNamespaceConfig = new GlobalConfig();
    disabledNamespaceConfig.setEnabled(true);
    disabledNamespaceConfig.setEnableNamespaceConfig(false);
    when(configService.getGlobalConfig()).thenReturn(disabledNamespaceConfig);

    enricher.enrich(context, appender);
    verifyNoInteractions(appender);
  }

  @Test
  @SubjectAware(permissions = {"namespace:customProperties:hitchhiker"})
  void shouldEnrich() {
    GlobalConfig enabledConfig = new GlobalConfig();
    enabledConfig.setEnabled(true);
    enabledConfig.setEnableNamespaceConfig(true);
    when(configService.getGlobalConfig()).thenReturn(enabledConfig);

    enricher.enrich(context, appender);
    verify(appender).appendLink(
      "customPropertiesConfig",
      "https://scm-test.de/scm/api/v2/custom-properties/namespace-configuration/hitchhiker"
    );
  }
}
