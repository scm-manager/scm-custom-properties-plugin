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
import { describe, it, expect, vi } from "vitest";
import { screen } from "@testing-library/react";
import { useConfigLink } from "../../../test/mocks/ui-api";
import NamespaceConfiguration from "./NamespaceConfiguration";
import { makeNamespaceConfig, makePredefinedKey, renderInProviders, t } from "../../../test/testUtils";

const configLink = (value: Record<string, unknown>) =>
  (useConfigLink as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
    isLoading: false,
    error: null,
    initialConfiguration: undefined,
    update: vi.fn().mockResolvedValue(undefined),
    isUpdating: false,
    isReadOnly: false,
    ...value,
  });

describe("NamespaceConfiguration", () => {
  it("shows a loading indicator while loading", () => {
    configLink({ isLoading: true });
    renderInProviders(<NamespaceConfiguration />);
    expect(screen.getByRole("status")).toBeInTheDocument();
  });

  it("shows an error notification on failure", () => {
    configLink({ error: new Error("namespace config failed") });
    renderInProviders(<NamespaceConfiguration />);
    expect(screen.getByRole("alert")).toHaveTextContent("namespace config failed");
  });

  it("renders nothing when there is no configuration", () => {
    configLink({ initialConfiguration: undefined });
    renderInProviders(<NamespaceConfiguration />);
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("renders globally predefined keys read-only and the namespace's own keys", () => {
    (useConfigLink as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      isLoading: false,
      error: null,
      initialConfiguration: makeNamespaceConfig({
        globallyPredefinedKeys: { globalKey: makePredefinedKey({ mode: "NONE" }) },
        predefinedKeys: { namespaceKey: makePredefinedKey({ mode: "NONE" }) },
      }),
      update: vi.fn().mockResolvedValue(undefined),
      isUpdating: false,
      isReadOnly: false,
    });

    renderInProviders(<NamespaceConfiguration />);

    expect(screen.getByText("globalKey")).toBeInTheDocument();
    expect(screen.getByText(t("scm-custom-properties-plugin.config.template.global"))).toBeInTheDocument();
    expect(screen.getByText("namespaceKey")).toBeInTheDocument();
  });
});
