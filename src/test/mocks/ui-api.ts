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

// Test mock for @scm-manager/ui-api. Exposes a controllable apiClient and a real
// implementation of urls.getValueStringFromLocationByKey (single-value query param lookup,
// matching the production semantics). Types mirror the plugin's usage so the sources
// type-check against this mock (see tsconfig.test.json).
import { vi } from "vitest";

type JsonResponse = { json: () => Promise<any> };

export const apiClient = {
  get: vi.fn<(url: string) => Promise<JsonResponse>>(),
  post: vi.fn<(url: string, payload?: unknown, contentType?: string) => Promise<unknown>>(),
  put: vi.fn<(url: string, payload?: unknown, contentType?: string) => Promise<unknown>>(),
  httpRequestWithJSONBody: vi.fn<(method: string, url: string, contentType: string, payload: unknown) => Promise<unknown>>(),
};

export const useRequiredIndexLink = vi.fn((_name?: string) => "/api/v2/custom-properties/missing-mandatory");

type ConfigLinkResult<T> = {
  isLoading: boolean;
  error: Error | null;
  initialConfiguration?: T;
  update: (config: T) => Promise<Response> | undefined;
  isUpdating: boolean;
  isReadOnly: boolean;
};
const configLinkMock = vi.fn();
export const useConfigLink = configLinkMock as unknown as (<T = any>(link: string) => ConfigLinkResult<T>) &
  typeof configLinkMock;

function getValueStringFromLocationByKey(location: { search?: string }, key: string): string | undefined {
  if (location && location.search) {
    const value = new URLSearchParams(location.search).get(key);
    if (value !== null) {
      return value;
    }
  }
  return undefined;
}

export const urls = { getValueStringFromLocationByKey };

/** Reset all api mocks to their defaults. Called from setup.ts in beforeEach. */
export function __resetUiApi() {
  apiClient.get.mockReset().mockResolvedValue({ json: async () => ({}) });
  apiClient.post.mockReset().mockResolvedValue(undefined);
  apiClient.put.mockReset().mockResolvedValue(undefined);
  apiClient.httpRequestWithJSONBody.mockReset().mockResolvedValue(undefined);
  useRequiredIndexLink.mockReset().mockReturnValue("/api/v2/custom-properties/missing-mandatory");
  configLinkMock.mockReset().mockReturnValue({
    isLoading: false,
    error: null,
    initialConfiguration: undefined,
    update: vi.fn().mockResolvedValue(undefined),
    isUpdating: false,
    isReadOnly: false,
  });
}
