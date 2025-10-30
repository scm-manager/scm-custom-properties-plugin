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
import { HitProps, RepositoryAvatar } from "@scm-manager/ui-components";
import { CardList, Notification } from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";
import { ValueHitField } from "@scm-manager/ui-types";
import styled from "styled-components";
import { Link } from "react-router-dom";

const StyledLink = styled(Link)`
  gap: 0.5rem;
`;

const CustomPropertyHitRenderer: FC<HitProps> = ({ hit }) => {
  const [t] = useTranslation("plugins");
  const repository = hit._embedded?.repository;

  if (!repository) {
    return <Notification type="danger">{t("scm-custom-properties-plugin.search.invalidResult")}</Notification>;
  }

  return (
    <CardList.Card key={`${repository.namespace}/${repository.name}/${(hit.fields.key as ValueHitField).value}`}>
      <CardList.Card.Row>
        <CardList.Card.Title>
          <StyledLink
            to={`/repo/${repository.namespace}/${repository.name}`}
            className="is-flex is-justify-content-flex-start is-align-items-center"
          >
            <RepositoryAvatar repository={repository} size={16} />
            <span>{`${repository.namespace}/${repository.name}`}</span>
          </StyledLink>
        </CardList.Card.Title>
      </CardList.Card.Row>
      <CardList.Card.Row>
        <p>
          {t("scm-custom-properties-plugin.search.customProperty.key", {
            key: (hit.fields.key as ValueHitField).value,
          })}
        </p>
        <p>
          {t("scm-custom-properties-plugin.search.customProperty.value", {
            value: (hit.fields.value as ValueHitField).value,
          })}
        </p>
      </CardList.Card.Row>
    </CardList.Card>
  );
};

export default CustomPropertyHitRenderer;
