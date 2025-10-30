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
import { BaseConfig, NamespaceConfig } from "../types";
import { useTranslation } from "react-i18next";
import EditAction from "../component/EditAction";
import CenteredTableFooter from "../component/CenteredTableFooter";
import { LinkButton } from "@scm-manager/ui-core";
import DeleteAction from "../component/DeleteAction";
import MinWidthTableCell from "../component/MinWidthTableCell";

type PredefinedKeysProps<T extends BaseConfig> = {
  config: T;
  update: (config: T) => Promise<Response> | undefined;
  isLoading: boolean;
  editBaseUrl: string;
};

const Tag: FC<{ children: React.ReactNode }> = ({ children }) => {
  return <span className="tag is-outlined is-link is-rounded ml-2">{children}</span>;
};

const PredefinedKeys = <T extends BaseConfig>({ config, update, isLoading, editBaseUrl }: PredefinedKeysProps<T>) => {
  const [t] = useTranslation("plugins");

  const globallyPredefinedKeys =
    "globallyPredefinedKeys" in config ? (config as NamespaceConfig).globallyPredefinedKeys : [];
  const globallyPredefinedTag = t("scm-custom-properties-plugin.config.template.global");

  return (
    <table className="table">
      <thead>
        <tr>
          <th>{t("scm-custom-properties-plugin.table.header.key")}</th>
          <th>{t("scm-custom-properties-plugin.table.header.action")}</th>
        </tr>
      </thead>
      <tbody>
        {globallyPredefinedKeys.map((key) => (
          <tr key={key}>
            <td colSpan={2}>
              {key}
              <Tag>{globallyPredefinedTag}</Tag>
            </td>
          </tr>
        ))}
        {config.predefinedKeys.map((key) => (
          <tr key={key}>
            <td>{key}</td>
            <MinWidthTableCell>
              <EditAction
                editUrl={`${editBaseUrl}/predefinedKeys/${encodeURIComponent(key)}`}
                ariaLabel={t("scm-custom-properties-plugin.table.body.edit", { key })}
              />
              <DeleteAction originalKey={key} update={update} config={config} isLoading={isLoading} />
            </MinWidthTableCell>
          </tr>
        ))}
      </tbody>
      <CenteredTableFooter>
        <tr>
          <td colSpan={2}>
            <LinkButton to={`${editBaseUrl}/predefinedKeys`} variant="primary">
              {t("scm-custom-properties-plugin.table.footer.addKey")}
            </LinkButton>
          </td>
        </tr>
      </CenteredTableFooter>
    </table>
  );
};

export default PredefinedKeys;
