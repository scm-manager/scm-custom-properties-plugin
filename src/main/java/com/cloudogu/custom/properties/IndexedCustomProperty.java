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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sonia.scm.search.Indexed;
import sonia.scm.search.IndexedType;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@IndexedType(repositoryScoped = true, namespaceScoped = true)
public class IndexedCustomProperty implements Comparable<CustomProperty>, Serializable {

  static final int VERSION = 2;

  @Indexed(type = Indexed.Type.SEARCHABLE, defaultQuery = true)
  private String key;
  @Indexed(type = Indexed.Type.SEARCHABLE, defaultQuery = true)
  private String value;

  //Combined field containing the "key=value" string, indexed for search.
  @Indexed(type = Indexed.Type.SEARCHABLE, name = "property")
  private String property;

  public IndexedCustomProperty(String key, String value) {
    this.key = key;
    this.value = value;
    this.property = computeProperty(key, value);
  }

  public IndexedCustomProperty(CustomProperty property) {
    this(property.getKey(), property.getValue());
  }

  public void setKey(String key) {
    this.key = key;
    this.property = computeProperty(this.key, this.value);
  }

  public void setValue(String value) {
    this.value = value;
    this.property = computeProperty(this.key, this.value);
  }

  private String computeProperty(String key, String value) {
    if (key == null || value == null) {
      return null;
    }
    return key + "=" + value;
  }

  @Override
  public int compareTo(CustomProperty customProperty) {
    return this.key.compareTo(customProperty.getKey());
  }
}
