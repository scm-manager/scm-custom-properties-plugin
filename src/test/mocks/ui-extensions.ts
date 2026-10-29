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

// Test double for @scm-manager/ui-extensions: a binder that records registrations.
import { vi } from "vitest";

export const binder = {
  bind: vi.fn(),
  getExtension: vi.fn(),
  getExtensions: vi.fn(() => []),
  hasExtension: vi.fn(() => false),
};

// `extensionPoints` is a types-only namespace at runtime.
export const extensionPoints = {};
