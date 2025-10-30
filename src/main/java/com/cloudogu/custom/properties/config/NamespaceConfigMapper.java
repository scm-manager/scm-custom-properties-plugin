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
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.NamespacePermissions;

import java.util.TreeSet;

import static de.otto.edison.hal.Links.linkingTo;

@Mapper
public abstract class NamespaceConfigMapper {

  @Inject
  private ScmPathInfoStore scmPathInfoStore;

  @VisibleForTesting
  void setScmPathInfoStore(ScmPathInfoStore scmPathInfoStore) {
    this.scmPathInfoStore = scmPathInfoStore;
  }

  @Mapping(target = "globallyPredefinedKeys", ignore = true)
  @Mapping(target = "attributes", ignore = true)
  public abstract NamespaceConfigDto map(NamespaceConfig namespaceConfig, @Context GlobalConfig globalConfig, @Context String namespace);
  public abstract NamespaceConfig map(NamespaceConfigDto namespaceConfigDto);

  @AfterMapping
  public void appendLinks(@MappingTarget NamespaceConfigDto target, @Context GlobalConfig globalConfig, @Context String namespace) {
    target.setGloballyPredefinedKeys(new TreeSet<>(globalConfig.getPredefinedKeys()));
    target.setPredefinedKeys(new TreeSet<>(target.getPredefinedKeys()));

    Links.Builder linksBuilder = linkingTo();

    if (NamespacePermissions.custom(CustomPropertiesContext.CONFIG_PERMISSION_NAME, namespace).isPermitted()) {
      linksBuilder.self(self(namespace));
      linksBuilder.single(Link.link("update", update(namespace)));
    }

    target.add(linksBuilder.build());
  }

  private String self(String namespace) {
    LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get(), ConfigResource.class);
    return linkBuilder.method("getNamespaceConfig").parameters(namespace).href();
  }

  private String update(String namespace) {
    LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get(), ConfigResource.class);
    return linkBuilder.method("setNamespaceConfig").parameters(namespace).href();
  }
}
