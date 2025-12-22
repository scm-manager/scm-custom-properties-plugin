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

import com.cloudogu.custom.properties.CustomPropertiesContext;
import com.cloudogu.custom.properties.MandatoryPropertiesResource;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Namespace;
import sonia.scm.repository.NamespacePermissions;

@Extension
@Enrich(Namespace.class)
public class NamespaceEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStoreProvider;
  private final ConfigService configService;

  @Inject
  public NamespaceEnricher(Provider<ScmPathInfoStore> scmPathInfoStoreProvider, ConfigService configService) {
    this.scmPathInfoStoreProvider = scmPathInfoStoreProvider;
    this.configService = configService;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    Namespace namespace = context.oneRequireByType(Namespace.class);
    if (!NamespacePermissions.custom(CustomPropertiesContext.CONFIG_PERMISSION_NAME, namespace).isPermitted()) {
      return;
    }

    GlobalConfig globalConfig = configService.getGlobalConfig();
    if (!globalConfig.isEnabled() || !globalConfig.isEnableNamespaceConfig()) {
      return;
    }

    appender.appendLink(
      CustomPropertiesContext.CONFIG_LINK_NAME,
      new LinkBuilder(scmPathInfoStoreProvider.get().get(), ConfigResource.class)
        .method("getNamespaceConfig")
        .parameters(namespace.getNamespace())
        .href()
    );

    appender.appendLink(
      CustomPropertiesContext.MISSING_MANDATORY_PROPERTIES_LINK_NAME,
      new LinkBuilder(scmPathInfoStoreProvider.get().get(), MandatoryPropertiesResource.class)
        .method("readMissingMandatoryPropertiesFromNamespace")
        .parameters(namespace.getNamespace())
        .href()
    );
  }
}
