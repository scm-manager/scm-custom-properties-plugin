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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;

@Mapper
public abstract class CustomPropertyMapper {

  @Mapping(target = "links", ignore = true)
  @Mapping(target = "embedded", ignore = true)
  @Mapping(target = "attributes", ignore = true)
  public abstract CustomPropertyDto map(CustomProperty customProperty);
  public abstract CustomProperty map(CustomPropertyDto customPropertyDto);

  public Collection<CustomPropertyDto> mapToDtoCollection(Collection<CustomProperty> customProperties) {
    return customProperties.stream().map(this::map).toList();
  }

  public CustomPropertyReplacement map(ReplaceCustomPropertyDto replaceCustomPropertyDto) {
    return new CustomPropertyReplacement(
      new CustomProperty(replaceCustomPropertyDto.getOldKey(), replaceCustomPropertyDto.getOldValue()),
      new CustomProperty(replaceCustomPropertyDto.getNewKey(), replaceCustomPropertyDto.getNewValue())
    );
  }

  public record CustomPropertyReplacement(CustomProperty oldEntity, CustomProperty newEntity) {
  }
}
