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

import sonia.scm.BadRequestException;
import sonia.scm.ContextEntry;
import sonia.scm.repository.Repository;

public class InvalidValueException extends BadRequestException {

  public InvalidValueException(Repository repository, CustomProperty entity) {
    super(
      ContextEntry.ContextBuilder.entity("custom-property", entity.getKey()).in(repository).build(),
      String.format("'%s' is not an allowed value for the predefined key '%s'", entity.getValue(), entity.getKey())
    );
  }

  @Override
  public String getCode() {
    return "3q5Pm3kktO";
  }
}
