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

import com.google.common.annotations.VisibleForTesting;
import de.otto.edison.hal.Links;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import java.util.Collection;
import java.util.List;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class CustomPropertyMapper {

  private static final ThreadLocal<Boolean> WRITE_PERMISSION = new ThreadLocal<>();

  @Inject
  private Provider<ScmPathInfoStore> scmPathInfoStore;

  @VisibleForTesting
  public void setScmPathInfoStore(Provider<ScmPathInfoStore> scmPathInfoStore) {
    this.scmPathInfoStore = scmPathInfoStore;
  }

  @Mapping(target = "attributes", ignore = true)
  public abstract CustomPropertyDto map(CustomProperty customProperty, @Context Repository repository);
  public abstract CustomProperty map(CustomPropertyDto customPropertyDto);
  @Mapping(target = "defaultProperty", ignore = true)
  @Mapping(target = "mandatory", ignore = true)
  public abstract CustomProperty map(WriteCustomPropertyDto customPropertyDto);

  public List<CustomPropertyDto> mapToDtoCollection(Collection<CustomProperty> customProperties, Repository repository) {
    boolean hasWritePermission = RepositoryPermissions.modify(repository).isPermitted();
    WRITE_PERMISSION.set(hasWritePermission);

    try {
      return customProperties.stream().map(customProperty -> this.map(customProperty, repository)).toList();
    } finally {
      WRITE_PERMISSION.remove();
    }
  }

  @AfterMapping
  void appendLinks(@MappingTarget CustomPropertyDto customPropertyDto, @Context Repository repository) {
    if (WRITE_PERMISSION.get() != null) {
      if (!WRITE_PERMISSION.get()) {
        return;
      }
    } else if (!RepositoryPermissions.modify(repository).isPermitted()) {
      return;
    }

    LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), CustomPropertiesResource.class);
    Links.Builder links = new Links.Builder();

    if (!customPropertyDto.isDefaultProperty()) {
      links.single(
        link(
          "update",
          linkBuilder.method("update").parameters(repository.getNamespace(), repository.getName(), customPropertyDto.getKey()).href()
        )
      );

      links.single(
        link(
          "delete",
          linkBuilder.method("delete").parameters(repository.getNamespace(), repository.getName(), customPropertyDto.getKey()).href()
        )
      );
    }

    customPropertyDto.add(links.build());
  }
}
