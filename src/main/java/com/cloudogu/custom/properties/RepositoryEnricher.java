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

package com.cloudogu.custom.properties;

import com.cloudogu.custom.properties.config.ConfigService;
import com.google.common.annotations.VisibleForTesting;
import de.otto.edison.hal.HalRepresentation;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import java.util.Collection;

import static com.cloudogu.custom.properties.CustomPropertiesContext.LINK_NAME;

@Extension
@Enrich(Repository.class)
public class RepositoryEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> pathInfoStore;
  private final CustomPropertiesService customPropertiesService;
  private final ConfigService configService;
  private final CustomPropertyMapper customPropertyMapper;

  @Inject
  public RepositoryEnricher(Provider<ScmPathInfoStore> pathInfoStore,
                            CustomPropertiesService customPropertiesService,
                            ConfigService configService,
                            CustomPropertyMapper customPropertyMapper) {
    this.pathInfoStore = pathInfoStore;
    this.customPropertiesService = customPropertiesService;
    this.configService = configService;
    this.customPropertyMapper = customPropertyMapper;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    if (!configService.getGlobalConfig().isEnabled()) {
      return;
    }

    Repository repository = context.oneRequireByType(Repository.class);
    if (!RepositoryPermissions.read().isPermitted(repository)) {
      return;
    }

    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), CustomPropertiesResource.class);
    appender.appendLink(LINK_NAME + "Read", linkBuilder.method("read").parameters(repository.getNamespace(), repository.getName()).href());

    if (RepositoryPermissions.modify().isPermitted(repository)) {
      appender.appendLink(LINK_NAME + "Create", linkBuilder.method("create").parameters(repository.getNamespace(), repository.getName()).href());
      appender.appendLink(LINK_NAME + "Update", linkBuilder.method("update").parameters(repository.getNamespace(), repository.getName()).href());
      appender.appendLink(LINK_NAME + "Delete", linkBuilder.method("delete").parameters(repository.getNamespace(), repository.getName()).href());
    }

    Collection<CustomProperty> properties = customPropertiesService.get(repository);
    appender.appendEmbedded(
      "customProperties",
      new CustomPropertyCollection(properties.stream().map(customPropertyMapper::map).toList())
    );
  }

  @Getter
  @AllArgsConstructor
  @VisibleForTesting
  static class CustomPropertyCollection extends HalRepresentation {
    private final Collection<CustomPropertyDto> properties;
  }
}
