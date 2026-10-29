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
import styled from "styled-components";
import { Icon } from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";

const BreadcrumbNav = styled.nav`
  ol {
    align-items: center;
    gap: 0 0.5em;
    white-space: break-spaces;
  }

  li {
    gap: 0 0.5em;
    word-break: break-all;
  }

  li:not(.is-active) a:hover {
    text-decoration: underline;
  }

  && li + li::before,
  && li:not(:last-child)::after {
    content: none !important;
  }

  a {
    color: inherit;
    padding-right: 0;
    padding-left: 0;

    &:focus-visible {
      outline-width: 3px;
      outline-offset: 3px;
      border-radius: 0.25em;
    }
  }

  .subtitle {
    color: var(--scm-secondary-more-color, #363636);
    font-size: 20px;
    font-weight: 600;
    line-height: 24px;
  }

  li + li .subtitle {
    font-weight: 400;
  }

  .icon {
    height: auto;
    color: var(--scm-secondary-more-color, #363636);
    width: 0.5em;
    padding-top: 2px;

    &:first-child {
      margin: 0;
    }
  }
`;

const Divider = styled.hr`
  background-color: var(--scm-border-color, #dbdbdb);
  border: none;
  height: 1px;
  margin: 1.5rem 0;
`;

type Props = {
  title: string;
  activeSubpageTitle?: string;
  onBack: () => void;
};

const CustomPropertiesBreadcrumb: FC<Props> = ({ title, activeSubpageTitle, onBack }) => {
  const [t] = useTranslation("commons");

  return (
    <>
      <BreadcrumbNav className="breadcrumb subtitle" aria-label={t("subpageForm.breadcrumb.ariaLabel")}>
        <ol role="list">
          <li className={!activeSubpageTitle ? "is-active" : undefined}>
            <a
              href="#"
              aria-current={!activeSubpageTitle ? "page" : undefined}
              onClick={(e) => {
                e.preventDefault();
                onBack();
              }}
            >
              <span className="subtitle">{title}</span>
            </a>
          </li>
          {activeSubpageTitle ? (
            <li className="is-active">
              <Icon>angle-right</Icon>
              <a href="#" aria-current="page" onClick={(e) => e.preventDefault()}>
                <span className="subtitle">{activeSubpageTitle}</span>
              </a>
            </li>
          ) : null}
        </ol>
      </BreadcrumbNav>
      <Divider />
    </>
  );
};

export default CustomPropertiesBreadcrumb;
