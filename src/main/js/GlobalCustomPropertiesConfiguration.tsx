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

import { ConfigurationForm, Form, Title } from "@scm-manager/ui-core";
import { HalRepresentation } from "@scm-manager/ui-types";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";

type GlobalCustomPropertiesConfigurationDto = HalRepresentation & {
  enabled: boolean;
};

const GlobalCustomPropertiesConfiguration: FC<{ link: string }> = ({ link }) => {
  const [t] = useTranslation("plugins");
  return (
    <ConfigurationForm<GlobalCustomPropertiesConfigurationDto>
      link={link}
      translationPath={["plugins", "scm-custom-properties-plugin.config"]}
    >
      <Title>{t("scm-custom-properties-plugin.config.title")}</Title>
      <Form.Row>
        <Form.Checkbox name="enabled" />
      </Form.Row>
    </ConfigurationForm>
  );
};

export default GlobalCustomPropertiesConfiguration;
