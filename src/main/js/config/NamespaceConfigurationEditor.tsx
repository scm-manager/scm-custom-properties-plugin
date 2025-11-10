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
import ConfigEditor from "./ConfigEditor";
import { useConfigLink } from "@scm-manager/ui-api";
import { NamespaceConfig } from "../types";
import { ErrorNotification, Loading, useDocumentTitle } from "@scm-manager/ui-core";
import { Namespace } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";

const NamespaceConfigurationEditor: FC<{ link: string; namespace: Namespace }> = ({ link, namespace }) => {
  const [t] = useTranslation("plugins");
  const { isLoading, error, initialConfiguration: config, update } = useConfigLink<NamespaceConfig>(link);
  useDocumentTitle(t("scm-custom-properties-plugin.config.title"), t("scm-custom-properties-plugin.config.edit"));
  const baseUrl = `/namespace/${namespace.namespace}/settings/custom-properties`;

  if (isLoading) {
    return <Loading />;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (!config) {
    return null;
  }

  return <ConfigEditor config={config} update={update} redirectUrl={baseUrl} />;
};

export default NamespaceConfigurationEditor;
