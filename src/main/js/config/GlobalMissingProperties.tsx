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

import React from "react";
import { useTranslation } from "react-i18next";
import { useMissingMandatoryProperties } from "../hooks";
import { Title, useDocumentTitle } from "@scm-manager/ui-core";
import MissingProperties from "./MissingProperties";

const GlobalMissingProperties = () => {
  const [t] = useTranslation("plugins");
  const { isLoading, error, data } = useMissingMandatoryProperties();
  useDocumentTitle(
    t("scm-custom-properties-plugin.config.title"),
    t("scm-custom-properties-plugin.config.missingMandatoryProperties.title"),
  );

  return (
    <>
      <Title>{t("scm-custom-properties-plugin.config.missingMandatoryProperties.title")}</Title>
      <MissingProperties isLoading={isLoading} error={error} missingProperties={data ?? {}} />
    </>
  );
};

export default GlobalMissingProperties;
