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

// Test doubles for the @scm-manager/ui-components used by the repository custom-properties UI.
import React from "react";
import { Link } from "react-router-dom";
import { vi } from "vitest";

export const SmallLoadingSpinner: React.FC = () => <span aria-label="loading" />;

export const SecondaryNavigationItem: React.FC<Record<string, any>> = ({ label, to, icon }) => (
  <Link to={to} data-icon={icon}>
    {label}
  </Link>
);

export const RepositoryAvatar: React.FC<Record<string, any>> = ({ repository, size }) => (
  <span data-avatar data-size={size} aria-hidden="true">
    {repository?.namespace}
  </span>
);

export const ConfigurationBinder = {
  bindAdminSetting: vi.fn(),
  bindNamespaceSetting: vi.fn(),
};
