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

type WithChildren = { children: React.ReactNode };

export const ButtonsContainer = styled.div`
  display: flex;
  gap: 0.75rem;
`;

export const Row = ({ children }: WithChildren) => {
  return <div className="columns">{children}</div>;
};

export const Field = ({ children }: WithChildren) => {
  return <div className="field column">{children}</div>;
};
