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
import { ErrorNotification, Loading } from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router";
import { Link } from "react-router-dom";

type MissingPropertiesProps = {
  isLoading: boolean;
  error: Error | null;
  missingProperties: Record<string, string[]>;
};

const MissingProperties: FC<MissingPropertiesProps> = ({ isLoading, error, missingProperties }) => {
  const [t] = useTranslation("plugins");
  const { propertyKey } = useParams<{ propertyKey: string }>();

  if (isLoading) {
    return <Loading />;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (!missingProperties) {
    return null;
  }

  return (
    <>
      {missingProperties[propertyKey] ? (
        <div className="content">
          <p>{t("scm-custom-properties-plugin.config.missingMandatoryProperties.paragraph", { key: propertyKey })}</p>
          <ul>
            {missingProperties[propertyKey]
              .sort((a, b) => a.localeCompare(b))
              .map((repo) => (
                <li key={repo}>
                  {/* @ts-expect-error Weird typing error, that should become irrelevant with react 19 */}
                  <Link to={{ pathname: `/repo/${repo}/custom-properties`, hash: "missing-properties" }}>{repo}</Link>
                </li>
              ))}
          </ul>
        </div>
      ) : (
        <p>{t("scm-custom-properties-plugin.config.missingMandatoryProperties.notMissing", { key: propertyKey })}</p>
      )}
    </>
  );
};

export default MissingProperties;
