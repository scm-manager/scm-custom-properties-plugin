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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "customProperty")
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomProperty implements Comparable<CustomProperty>, Serializable {
  private String key;
  private String value;

  /*
  * Ignoring this field for serialization
  * Since every Custom Property that gets serialized will never be a default property
  * Therefore isDefaultProperty will always be false anyway after deserialization
  */
  @XmlTransient
  private boolean isDefaultProperty = false;

  public CustomProperty(String key, String value) {
    this(key, value, false);
  }

  @Override
  public int compareTo(CustomProperty customProperty) {
    return this.key.compareTo(customProperty.getKey());
  }
}
