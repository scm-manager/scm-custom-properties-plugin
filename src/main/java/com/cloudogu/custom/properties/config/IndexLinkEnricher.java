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
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.Index;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.config.ConfigurationPermissions;
import sonia.scm.plugin.Extension;

@Extension
@Enrich(Index.class)
public class IndexLinkEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> pathInfoStore;

  @Inject
  public IndexLinkEnricher(Provider<ScmPathInfoStore> pathInfoStore) {
    this.pathInfoStore = pathInfoStore;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    if (ConfigurationPermissions.read(CustomPropertiesContext.CONFIG_PERMISSION_NAME).isPermitted()) {
      LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), ConfigResource.class);
      appender.appendLink(
        CustomPropertiesContext.CONFIG_LINK_NAME,
        linkBuilder.method("getGlobalConfig").parameters().href()
      );
    }
  }
}
