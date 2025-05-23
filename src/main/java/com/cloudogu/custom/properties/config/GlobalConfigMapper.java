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
import com.google.common.annotations.VisibleForTesting;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import jakarta.inject.Inject;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.config.ConfigurationPermissions;

import static de.otto.edison.hal.Links.linkingTo;

@Mapper
public abstract class GlobalConfigMapper {

  @Inject
  private ScmPathInfoStore scmPathInfoStore;

  @VisibleForTesting
  void setScmPathInfoStore(ScmPathInfoStore scmPathInfoStore) {
    this.scmPathInfoStore = scmPathInfoStore;
  }

  @Mapping(target = "links", ignore = true)
  @Mapping(target = "embedded", ignore = true)
  @Mapping(target = "attributes", ignore = true)
  public abstract GlobalConfigDto map(GlobalConfig globalConfig);
  public abstract GlobalConfig map(GlobalConfigDto globalConfigDto);

  @AfterMapping
  public void appendLinks(@MappingTarget GlobalConfigDto target) {
    Links.Builder linksBuilder = linkingTo();
    if (ConfigurationPermissions.read(CustomPropertiesContext.CONFIG_PERMISSION_NAME).isPermitted()) {
      linksBuilder.self(self());
    }

    if (ConfigurationPermissions.write(CustomPropertiesContext.CONFIG_PERMISSION_NAME).isPermitted()) {
      linksBuilder.single(Link.link("update", update()));
    }

    target.add(linksBuilder.build());
  }

  private String self() {
    LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get(), ConfigResource.class);
    return linkBuilder.method("getGlobalConfig").parameters().href();
  }

  private String update() {
    LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get(), ConfigResource.class);
    return linkBuilder.method("setGlobalConfig").parameters().href();
  }
}
