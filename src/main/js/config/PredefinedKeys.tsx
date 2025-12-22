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
import { BaseConfig, NamespaceConfig, SinglePredefinedKey, ValueMode } from "../types";
import { useTranslation } from "react-i18next";
import EditAction from "../component/EditAction";
import CenteredTableFooter from "../component/CenteredTableFooter";
import { Icon, LinkButton } from "@scm-manager/ui-core";
import DeleteAction from "../component/DeleteAction";
import MinWidthTableCell from "../component/MinWidthTableCell";
import PropertyTag from "../component/PropertyTag";
import { SmallLoadingSpinner } from "@scm-manager/ui-components";

type PredefinedKeysProps<T extends BaseConfig> = {
  config: T;
  update: (config: T) => Promise<Response> | undefined;
  isLoading: boolean;
  editBaseUrl: string;
  isMissingPropertiesLoading: boolean;
  missingProperties: Record<string, string[]>;
  missingPropertiesBaseUrl: string;
};

type MandatoryValueSetProps = {
  propertyKey: string;
  mode: ValueMode;
  isLoading: boolean;
  violatingRepositories: string[] | undefined;
  missingPropertiesBaseUrl: string;
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

const MandatoryValueSet: FC<MandatoryValueSetProps> = ({
  propertyKey,
  mode,
  isLoading,
  violatingRepositories,
  missingPropertiesBaseUrl,
}) => {
  const [t] = useTranslation("plugins");

  if (mode !== "MANDATORY" || isLoading) {
    return null;
  }

  if (!violatingRepositories) {
    return (
      <Icon
        className="is-medium"
        aria-label={t("scm-custom-properties-plugin.config.missingMandatoryProperties.ariaLabelNoMissingRepos", {
          key: propertyKey,
        })}
      >
        check-circle
      </Icon>
    );
  }

  return (
    <LinkButton
      aria-label={t("scm-custom-properties-plugin.config.missingMandatoryProperties.ariaLabelShowMissingRepos", {
        key: propertyKey,
      })}
      to={`${missingPropertiesBaseUrl}/${encodeURIComponent(propertyKey)}`}
      variant="signal"
    >
      <Icon>exclamation</Icon>
    </LinkButton>
  );
};

const DefaultOrMandatoryColumn: FC<Pick<SinglePredefinedKey, "mode" | "defaultValue">> = ({ mode, defaultValue }) => {
  const [t] = useTranslation("plugins");

  if (mode === "MANDATORY") {
    return <span className="mr-2">{t("scm-custom-properties-plugin.editor.mandatoryValueTag")}</span>;
  }

  if (mode === "DEFAULT" && defaultValue) {
    return <>{`${t("scm-custom-properties-plugin.editor.defaultValueTag")}: ${defaultValue}`}</>;
  }

  return null;
};

const PredefinedKeys = <T extends BaseConfig>({
  config,
  update,
  isLoading,
  editBaseUrl,
  isMissingPropertiesLoading,
  missingProperties,
  missingPropertiesBaseUrl,
}: PredefinedKeysProps<T>) => {
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
          <th>
            {isMissingPropertiesLoading ? (
              <SmallLoadingSpinner />
            ) : (
              t("scm-custom-properties-plugin.table.header.mandatoryValue")
            )}
          </th>
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
              <td>
                <DefaultOrMandatoryColumn defaultValue={definition.defaultValue} mode={definition.mode} />
              </td>
              <td colSpan={2}>
                <MandatoryValueSet
                  propertyKey={key}
                  mode={definition.mode}
                  violatingRepositories={missingProperties[key]}
                  isLoading={isMissingPropertiesLoading}
                  missingPropertiesBaseUrl={missingPropertiesBaseUrl}
                />
              </td>
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
              <td>
                <DefaultOrMandatoryColumn defaultValue={definition.defaultValue} mode={definition.mode} />
              </td>
              <td>
                <MandatoryValueSet
                  propertyKey={key}
                  mode={definition.mode}
                  violatingRepositories={missingProperties[key]}
                  isLoading={isMissingPropertiesLoading}
                  missingPropertiesBaseUrl={missingPropertiesBaseUrl}
                />
              </td>
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
          <td colSpan={5}>
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
