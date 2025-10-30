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
import EditAction from "../component/EditAction";
import MinWidthTableCell from "../component/MinWidthTableCell";

type GeneralSettingsProps = {
  editBaseUrl: string;
  settings: { name: string; value: string; ariaLabel: string }[];
};

const GeneralSettings: FC<GeneralSettingsProps> = ({ editBaseUrl, settings }) => {
  const [t] = useTranslation("plugins");

  return (
    <table className="table">
      <thead>
        <tr>
          <th>{t("scm-custom-properties-plugin.table.header.setting")}</th>
          <th>{t("scm-custom-properties-plugin.table.header.action")}</th>
        </tr>
      </thead>
      <tbody>
        {settings.map((property) => (
          <tr key={property.name}>
            <td>{property.value}</td>
            <MinWidthTableCell>
              <EditAction editUrl={`${editBaseUrl}/${property.name}`} ariaLabel={property.ariaLabel} />
            </MinWidthTableCell>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

export default GeneralSettings;
