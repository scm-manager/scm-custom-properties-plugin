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
import { ErrorNotification, Loading, useNamespaceFromParams } from "@scm-manager/ui-core";
import { useConfigLink } from "@scm-manager/ui-api";
import { Link } from "@scm-manager/ui-types";
import { NamespaceConfig } from "../types";
import { useMissingMandatoryPropertiesForNamespace } from "../hooks";
import ConfigForm from "./ConfigForm";

const configLinkName = "customPropertiesConfig";

const NamespaceConfiguration: FC = () => {
  const namespace = useNamespaceFromParams();
  const link = (namespace._links[configLinkName] as Link).href;
  const { isLoading, error, initialConfiguration: config, update, isUpdating, isReadOnly } =
    useConfigLink<NamespaceConfig>(link);
  const {
    isLoading: isLoadingMissingProperties,
    error: missingPropertiesError,
    data: missingProperties,
  } = useMissingMandatoryPropertiesForNamespace(namespace);

  if (isLoading) {
    return <Loading />;
  }

  if (error || missingPropertiesError) {
    return <ErrorNotification error={error || missingPropertiesError} />;
  }

  if (!config) {
    return null;
  }

  return (
    <ConfigForm
      level="namespace"
      config={config}
      update={update}
      isReadOnly={isReadOnly}
      isUpdating={isUpdating}
      missingProperties={missingProperties ?? {}}
      isMissingPropertiesLoading={isLoadingMissingProperties}
    />
  );
};

export default NamespaceConfiguration;
