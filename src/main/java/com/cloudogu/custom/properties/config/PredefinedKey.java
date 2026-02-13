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
import com.google.common.base.Strings;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.cloudogu.custom.properties.config.ValueMode.NONE;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class PredefinedKey {
  private List<String> allowedValues = new ArrayList<>();

  //DEFAULT is chosen to not break with the current API
  private ValueMode mode = ValueMode.DEFAULT;
  private String defaultValue = "";

  public PredefinedKey(List<String> allowedValues) {
    this(allowedValues, NONE, "");
  }

  public PredefinedKey(List<String> allowedValues, String defaultValue) {
    this(allowedValues, ValueMode.DEFAULT, defaultValue);
  }

  public boolean isValueValid(String value) {
    if (mode == ValueMode.MULTIPLE_CHOICE) {
      return isMultipleChoiceValueValid(value);
    }

    return isSingleValueValid(value);
  }

  private boolean isMultipleChoiceValueValid(String value) {
    String[] choices = value.split(CustomPropertiesContext.MULTIPLE_CHOICE_VALUE_SEPARATOR);

    if (choices.length == 0) {
      return false;
    }

    return Arrays.stream(choices)
      .filter(choice -> !allowedValues.contains(choice))
      .findFirst()
      .isEmpty();
  }

  private boolean isSingleValueValid(String value) {
    return allowedValues.isEmpty() || allowedValues.contains(value);
  }

  public boolean isDefaultValueValid() {
    return defaultValue.isEmpty() || isValueValid(defaultValue);
  }

  public ValueMode getMode() {
    // for legacy values before the introduction of the mode field, we have to check whether a default value is really set
    return mode == ValueMode.DEFAULT && Strings.isNullOrEmpty(defaultValue) ? NONE : mode;
  }
}
