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
import PropertyTag from "../component/PropertyTag";

type PredefinedKeysProps<T extends BaseConfig> = {
  config: T;
  update: (config: T) => Promise<Response> | undefined;
  isLoading: boolean;
  editBaseUrl: string;
};

const AllowedValuesColumn: FC<{ key: string; allowedValues: string[] }> = ({ key, allowedValues }) => {
  return (
    <ul>
      {[...allowedValues]
        .sort((valueA, valueB) => valueA.localeCompare(valueB))
        .map((value) => (
          <li key={`${key}-${value}`}>{value}</li>
        ))}
    </ul>
  );
};

const PredefinedKeys = <T extends BaseConfig>({ config, update, isLoading, editBaseUrl }: PredefinedKeysProps<T>) => {
  const [t] = useTranslation("plugins");

  const globallyPredefinedKeys =
    "globallyPredefinedKeys" in config ? (config as NamespaceConfig).globallyPredefinedKeys : {};
  const globallyPredefinedTag = t("scm-custom-properties-plugin.config.template.global");

  return (
    <table className="table">
      <thead>
        <tr>
          <th>{t("scm-custom-properties-plugin.table.header.key")}</th>
          <th>{t("scm-custom-properties-plugin.table.header.allowedValues")}</th>
          <th>{t("scm-custom-properties-plugin.table.header.defaultValue")}</th>
          <th>{t("scm-custom-properties-plugin.table.header.action")}</th>
        </tr>
      </thead>
      <tbody>
        {Object.entries(globallyPredefinedKeys)
          .sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
          .map(([key, definition]) => (
            <tr key={key}>
              <td>
                {key}
                <PropertyTag>{globallyPredefinedTag}</PropertyTag>
              </td>
              <td>
                <AllowedValuesColumn key={key} allowedValues={definition.allowedValues} />
              </td>
              <td colSpan={2}>{definition.defaultValue}</td>
            </tr>
          ))}
        {Object.entries(config.predefinedKeys)
          .sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
          .map(([key, definition]) => (
            <tr key={key}>
              <td>{key}</td>
              <td>
                <AllowedValuesColumn key={key} allowedValues={definition.allowedValues} />
              </td>
              <td>{definition.defaultValue}</td>
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
          <td colSpan={4}>
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
