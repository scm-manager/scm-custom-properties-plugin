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

import { Namespace } from "@scm-manager/ui-types";
import React, { FC } from "react";
import { ErrorNotification, Loading, SubSubtitle, Subtitle } from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";
import { useConfigLink } from "@scm-manager/ui-api";
import { NamespaceConfig } from "../types";
import PredefinedKeys from "./PredefinedKeys";

const NamespaceConfiguration: FC<{ link: string; namespace: Namespace }> = ({ link, namespace }) => {
  const [t] = useTranslation("plugins");
  const { isLoading, error, initialConfiguration: config, update, isUpdating } = useConfigLink<NamespaceConfig>(link);
  const baseUrl = `/namespace/${namespace.namespace}/settings/custom-properties`;
  const editUrl = `${baseUrl}/edit`;

  if (isLoading) {
    return <Loading />;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (!config) {
    return null;
  }

  return (
    <>
      <Subtitle>{t("scm-custom-properties-plugin.config.title")}</Subtitle>
      <SubSubtitle>{t("scm-custom-properties-plugin.config.predefinedKeys.subtitle")}</SubSubtitle>
      <PredefinedKeys config={config} update={update} isLoading={isUpdating} editBaseUrl={editUrl} />
    </>
  );
};

export default NamespaceConfiguration;
