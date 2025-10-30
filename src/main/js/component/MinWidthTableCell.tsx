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

import styled from "styled-components";
import React, { FC } from "react";

const MinWidthTableData = styled.td`
  width: 0;
  white-space: nowrap;
`;

const MinWidthDiv = styled.div`
  width: fit-content;
`;

const MinWidthTableCell: FC<{ children?: React.ReactNode }> = ({ children }) => {
  return (
    <MinWidthTableData>
      {/* @ts-expect-error */}
      <MinWidthDiv>{children}</MinWidthDiv>
    </MinWidthTableData>
  );
};

export default MinWidthTableCell;
