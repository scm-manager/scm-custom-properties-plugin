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
import { render } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Repository } from "@scm-manager/ui-types";
import { GlobalConfig, NamespaceConfig } from "../main/js/types";
import { useTranslation as createTranslator } from "./mocks/react-i18next";

export function t(key: string, options?: Record<string, unknown>): string {
  const [translate] = createTranslator("plugins");
  const resolved = translate(key, options);
  if (resolved === key) {
    throw new Error(`Translation key is not set: "${key}" (check en/plugins.json or the commons test mock)`);
  }
  return resolved;
}

export const NAMESPACE = "hitchhiker";
export const NAME = "heart-of-gold";
export const BASE = `/repo/${NAMESPACE}/${NAME}/custom-properties`;

const LocationDisplay: React.FC = () => {
  const location = useLocation();
  return <div data-testid="location">{location.pathname + location.search}</div>;
};

/**
 * Renders a component under a MemoryRouter (matching the repository custom-properties route)
 * and a fresh QueryClient. `route` sets the initial URL incl. query string.
 */
export function renderWithProviders(ui: React.ReactElement, route: string = BASE) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return {
    queryClient,
    ...render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[route]}>
          <LocationDisplay />
          <Routes>
            <Route path="/repo/:namespace/:name/custom-properties" element={ui} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    ),
  };
}

/** Renders inside QueryClient + MemoryRouter without constraining to a specific route. */
export function renderInProviders(ui: React.ReactElement, route: string = "/") {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return {
    queryClient,
    ...render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
      </QueryClientProvider>,
    ),
  };
}

export function makePredefinedKey(overrides: Record<string, unknown> = {}) {
  return { allowedValues: [], defaultValue: "", mode: "NONE", ...overrides };
}

export function makeGlobalConfig(overrides: Record<string, unknown> = {}) {
  return {
    enabled: true,
    enableNamespaceConfig: false,
    predefinedKeys: {},
    _links: { update: { href: "/api/v2/config" } },
    ...overrides,
  } as GlobalConfig;
}

export function makeNamespaceConfig(overrides: Record<string, unknown> = {}) {
  return {
    predefinedKeys: {},
    globallyPredefinedKeys: {},
    _links: { update: { href: "/api/v2/namespace-config" } },
    ...overrides,
  } as NamespaceConfig;
}

type PropertyOverrides = {
  key?: string;
  value?: string;
  separator?: string;
  defaultProperty?: boolean;
  mandatory?: boolean;
  _links?: Record<string, unknown>;
};

export function makeProperty(overrides: PropertyOverrides = {}) {
  return {
    key: "lang",
    value: "java",
    separator: "\t",
    _links: { update: { href: "/api/v2/props/lang" }, delete: { href: "/api/v2/props/lang" } },
    ...overrides,
  };
}

type RepositoryOverrides = {
  properties?: ReturnType<typeof makeProperty>[];
  missing?: string[];
  create?: boolean;
};

export function makeRepository(overrides: RepositoryOverrides = {}) {
  const { properties, missing = [], create = true } = overrides;
  return {
    namespace: NAMESPACE,
    name: NAME,
    type: "git",
    _links: {
      self: { href: "/api/v2/repositories/hitchhiker/heart-of-gold" },
      predefinedCustomPropertyKeys: { href: "/api/v2/predefined" },
    },
    _embedded: {
      customProperties: {
        _links: create ? { create: { href: "/api/v2/props" } } : {},
        properties: properties ?? [makeProperty()],
      },
      missingMandatoryProperties: { missing },
    },
  } as Repository;
}
