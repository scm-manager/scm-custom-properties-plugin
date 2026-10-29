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
import {ErrorNotification, Loading, Title} from "@scm-manager/ui-core";
import { useConfigLink } from "@scm-manager/ui-api";
import { GlobalConfig } from "../types";
import { useMissingMandatoryProperties } from "../hooks";
import ConfigForm from "./ConfigForm";
import {useTranslation} from "react-i18next";

const GlobalConfiguration: FC<{ link: string }> = ({ link }) => {
  const { isLoading, error, initialConfiguration: config, update, isUpdating, isReadOnly } =
    useConfigLink<GlobalConfig>(link);
  const [t] = useTranslation("plugins");
  const {
    isLoading: isLoadingMissingProperties,
    error: missingPropertiesError,
    data: missingProperties,
  } = useMissingMandatoryProperties();

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
    <>
      <Title className="is-spaced">{t("scm-custom-properties-plugin.config.title")}</Title>
      <ConfigForm
        level="global"
        config={config}
        update={update}
        isReadOnly={isReadOnly}
        isUpdating={isUpdating}
        missingProperties={missingProperties ?? {}}
        isMissingPropertiesLoading={isLoadingMissingProperties}
      />
    </>

  );
};

export default GlobalConfiguration;
