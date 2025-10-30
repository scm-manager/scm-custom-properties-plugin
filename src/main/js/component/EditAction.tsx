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

import React, { FC } from "react";
import { Icon, LinkButton } from "@scm-manager/ui-core";

type EditActionProps = {
  editUrl: string;
  ariaLabel: string;
};

const EditAction: FC<EditActionProps> = ({ editUrl, ariaLabel }) => {
  return (
    <span className="mr-4">
      <LinkButton className="px-2" to={editUrl} aria-label={ariaLabel}>
        <Icon>edit</Icon>
      </LinkButton>
    </span>
  );
};

export default EditAction;
