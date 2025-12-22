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
import { Repository } from "@scm-manager/ui-types";
import { useLocation } from "react-router";
import styled from "styled-components";
import { Icon, LinkButton } from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";

// noinspection CssUnresolvedCustomProperty
const StyledBanner = styled.div`
  width: 100%;
  border: 1px solid var(--scm-warning-color);
  margin-bottom: 0.75rem;
  padding: 0.75rem;
  justify-content: space-between;
  display: flex;
  align-items: center;
  border-radius: 0.5rem;
`;

type Props = {
  repository: Repository;
};

const MissingPropertyBanner: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const location = useLocation();
  const locationParts = location.pathname.split("/");

  const missingMandatoryProperties = (repository._embedded?.missingMandatoryProperties as { missing: string[] })
    .missing;

  if (missingMandatoryProperties.length === 0) {
    return null;
  }

  if (locationParts.length > 4 && locationParts[4] === "custom-properties") {
    return null;
  }

  return (
    <StyledBanner>
      <div>
        <Icon className="is-warning">exclamation</Icon>
        <span>{t("scm-custom-properties-plugin.repository.banner.message")}</span>
      </div>
      <div>
        <LinkButton
          variant="signal"
          to={{
            pathname: `/repo/${repository.namespace}/${repository.name}/custom-properties`,
            hash: "missing-properties",
          }}
        >
          {t("scm-custom-properties-plugin.repository.banner.link")}
        </LinkButton>
      </div>
    </StyledBanner>
  );
};

export default MissingPropertyBanner;
