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

import React from "react";
import styled from "styled-components";
import classNames from "classnames";

type WithChildren = { children: React.ReactNode; className?: string; style?: React.CSSProperties };

export const ButtonsContainer = styled.div`
  display: flex;
  gap: 0.75rem;
`;

export const Row = ({ children, className, style }: WithChildren) => {
  return (
    <div className={classNames("columns", className)} style={style}>
      {children}
    </div>
  );
};

export const Field = ({ children, className, style }: WithChildren) => {
  return (
    <div className={classNames("field column", className)} style={style}>
      {children}
    </div>
  );
};
