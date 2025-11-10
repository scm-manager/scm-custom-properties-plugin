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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;
import java.util.stream.Collectors;

@Mapper
public abstract class PredefinedKeyMapper {

  @Mapping(target = "attributes", ignore = true)
  public abstract PredefinedKeyDto map(PredefinedKey predefinedKey);

  public Map<String, PredefinedKeyDto> mapAll(Map<String, PredefinedKey> predefinedKeys) {
    return predefinedKeys.entrySet().stream()
      .map(entry -> Map.entry(entry.getKey(), this.map(entry.getValue())))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
