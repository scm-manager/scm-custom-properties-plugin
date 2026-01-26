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
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.RepositoryLinkProvider;
import sonia.scm.repository.Repository;

import java.util.Collection;

@Mapper
public abstract class RepositoryMapper {

  @Inject
  private RepositoryLinkProvider repositoryLinkProvider;

  @Inject
  private CustomPropertyMapper customPropertyMapper;

  @VisibleForTesting
  void setRepositoryLinkProvider(RepositoryLinkProvider repositoryLinkProvider) {
    this.repositoryLinkProvider = repositoryLinkProvider;
  }

  @VisibleForTesting
  void setCustomPropertyMapper(CustomPropertyMapper customPropertyMapper) {
    this.customPropertyMapper = customPropertyMapper;
  }

  @Mapping(target = "attributes", ignore = true)
  public abstract BasicRepositoryDto map(Repository repository, @Context Collection<CustomProperty> customProps, @Context boolean withProps);

  @ObjectFactory
  BasicRepositoryDto createDto(Repository repository, @Context Collection<CustomProperty> customProps, @Context boolean withProps) {
    BasicRepositoryDto dto = new BasicRepositoryDto();

    if (withProps) {
      dto.withEmbedded("customProperties", customPropertyMapper.mapToDtoCollection(customProps, repository));
    }

    appendLinks(dto, repository);
    return dto;
  }

  void appendLinks(BasicRepositoryDto basicRepositoryDto, Repository repository) {
    Links links = new Links.Builder()
      .self(
        repositoryLinkProvider.get(repository.getNamespaceAndName())
      ).build();

    basicRepositoryDto.add(links);
  }
}
