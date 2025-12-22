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
import { useTranslation } from "react-i18next";
import { useMissingMandatoryPropertiesForNamespace } from "../hooks";
import { Subtitle, useDocumentTitle } from "@scm-manager/ui-core";
import MissingProperties from "./MissingProperties";
import { Namespace } from "@scm-manager/ui-types";

const NamespaceMissingProperties: FC<{ namespace: Namespace }> = ({ namespace }) => {
  const [t] = useTranslation("plugins");
  const { isLoading, error, data } = useMissingMandatoryPropertiesForNamespace(namespace);
  useDocumentTitle(
    t("scm-custom-properties-plugin.config.title"),
    t("scm-custom-properties-plugin.config.missingMandatoryProperties.title"),
    namespace.namespace,
  );

  return (
    <>
      <Subtitle>{t("scm-custom-properties-plugin.config.missingMandatoryProperties.title")}</Subtitle>
      <MissingProperties isLoading={isLoading} error={error} missingProperties={data ?? {}} />
    </>
  );
};

export default NamespaceMissingProperties;
