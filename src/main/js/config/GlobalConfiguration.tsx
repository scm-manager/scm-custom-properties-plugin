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

import { Subtitle, Title, Loading, ErrorNotification } from "@scm-manager/ui-core";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { GlobalConfig } from "../types";
import { useConfigLink } from "@scm-manager/ui-api";
import GeneralSettings from "./GeneralSettings";
import PredefinedKeys from "./PredefinedKeys";

const GlobalConfiguration: FC<{ link: string }> = ({ link }) => {
  const [t] = useTranslation("plugins");
  const { isLoading, error, initialConfiguration: config, update, isUpdating } = useConfigLink<GlobalConfig>(link);
  const baseUrl = "/admin/settings/custom-properties";
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

  const generalSettings = [
    {
      name: "enabled",
      value: t(`scm-custom-properties-plugin.config.general.enabled.radio.${config.enabled}`),
      ariaLabel: t("scm-custom-properties-plugin.config.general.enabled.edit.ariaLabel"),
    },
    {
      name: "enableNamespaceConfig",
      value: t(
        `scm-custom-properties-plugin.config.general.enableNamespaceConfig.radio.${config.enableNamespaceConfig}`,
      ),
      ariaLabel: t("scm-custom-properties-plugin.config.general.enableNamespaceConfig.edit.ariaLabel"),
    },
  ];

  return (
    <>
      <Title>{t("scm-custom-properties-plugin.config.title")}</Title>
      <Subtitle>{t("scm-custom-properties-plugin.config.general.subtitle")}</Subtitle>
      <GeneralSettings editBaseUrl={editUrl} settings={generalSettings} />
      <Subtitle>{t("scm-custom-properties-plugin.config.predefinedKeys.subtitle")}</Subtitle>
      <PredefinedKeys config={config} update={update} isLoading={isUpdating} editBaseUrl={editUrl} />
    </>
  );
};

export default GlobalConfiguration;
