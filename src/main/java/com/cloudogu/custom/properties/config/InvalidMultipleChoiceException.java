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

import sonia.scm.BadRequestException;
import sonia.scm.ContextEntry;

public class InvalidMultipleChoiceException extends BadRequestException {

  public InvalidMultipleChoiceException(String namespace, String keyName) {
    this(NamespaceConfig.class, namespace, keyName);
  }

  public InvalidMultipleChoiceException(String keyName) {
    this(GlobalConfig.class, "GlobalConfig", keyName);
  }

  private InvalidMultipleChoiceException(Class<?> clazz, String contextId, String keyName) {
    super(
      ContextEntry.ContextBuilder.entity("predefined-key", keyName).in(clazz, contextId).build(),
      String.format("'%s' cannot be a multiple choice property, because no allowed values were defined", keyName)
    );
  }

  @Override
  public String getCode() {
    return "wPE5M2fxdR";
  }
}
