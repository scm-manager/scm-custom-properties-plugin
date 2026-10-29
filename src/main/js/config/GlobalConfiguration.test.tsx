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
import { screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { apiClient, useConfigLink } from "../../../test/mocks/ui-api";
import GlobalConfiguration from "./GlobalConfiguration";
import { makeGlobalConfig, makePredefinedKey, renderInProviders, t } from "../../../test/testUtils";

const configLink = (overrides: Record<string, unknown> = {}) =>
  (useConfigLink as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
    isLoading: false,
    error: null,
    initialConfiguration: makeGlobalConfig(),
    update: vi.fn().mockResolvedValue(undefined),
    isUpdating: false,
    isReadOnly: false,
    ...overrides,
  });

describe("GlobalConfiguration", () => {
  it("shows a loading indicator while the configuration loads", () => {
    configLink({ isLoading: true, initialConfiguration: undefined });
    renderInProviders(<GlobalConfiguration link="/config" />);

    expect(screen.getByRole("status")).toBeInTheDocument();
  });

  it("shows an error notification when loading fails", () => {
    configLink({ error: new Error("config load failed"), initialConfiguration: undefined });
    renderInProviders(<GlobalConfiguration link="/config" />);

    expect(screen.getByRole("alert")).toHaveTextContent("config load failed");
  });

  it("renders nothing when there is no configuration", () => {
    configLink({ initialConfiguration: undefined });
    renderInProviders(<GlobalConfiguration link="/config" />);

    expect(screen.queryByText(t("scm-custom-properties-plugin.config.title"))).toBeNull();
  });

  it("renders the title and the current general settings", () => {
    configLink({ initialConfiguration: makeGlobalConfig({ enabled: true, enableNamespaceConfig: false }) });
    renderInProviders(<GlobalConfiguration link="/config" />);

    expect(screen.getByText(t("scm-custom-properties-plugin.config.title"))).toBeInTheDocument();
    expect(screen.getByText(t("scm-custom-properties-plugin.config.general.enabled.radio.true"))).toBeInTheDocument();
    expect(
      screen.getByText(t("scm-custom-properties-plugin.config.general.enableNamespaceConfig.radio.false")),
    ).toBeInTheDocument();
  });

  it("lists a mandatory key and opens the violating-repositories subpage with deeplinks", async () => {
    const user = userEvent.setup();
    apiClient.get.mockResolvedValue({ json: async () => ({ license: ["hitchhiker/heart-of-gold"] }) });
    configLink({
      initialConfiguration: makeGlobalConfig({ predefinedKeys: { license: makePredefinedKey({ mode: "MANDATORY" }) } }),
    });
    renderInProviders(<GlobalConfiguration link="/config" />);

    expect(screen.getByText("license")).toBeInTheDocument();

    const showMissing = await screen.findByRole("button", {
      name: t("scm-custom-properties-plugin.config.missingMandatoryProperties.ariaLabelShowMissingRepos", { key: "license" }),
    });
    await user.click(showMissing);

    const repoLink = await screen.findByRole("link", { name: "hitchhiker/heart-of-gold" });
    expect(repoLink).toHaveAttribute("href", expect.stringContaining("/repo/hitchhiker/heart-of-gold/custom-properties"));
    expect(repoLink).toHaveAttribute("href", expect.stringContaining("#missing-properties"));
  });

  it("saves a toggled general setting", async () => {
    const user = userEvent.setup();
    const update = vi.fn().mockResolvedValue(undefined);
    configLink({ initialConfiguration: makeGlobalConfig({ enabled: true }), update });
    renderInProviders(<GlobalConfiguration link="/config" />);

    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.config.general.enabled.edit.ariaLabel") }));

    const disabledRadio = screen.getByRole("radio", {
      name: t("scm-custom-properties-plugin.config.general.enabled.radio.false"),
    });
    fireEvent.click(disabledRadio);
    await waitFor(() => expect(disabledRadio).toBeChecked());

    fireEvent.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() => expect(update).toHaveBeenCalledWith(expect.objectContaining({ enabled: false })));
  });

  it("hides editing controls and shows a warning icon in read-only mode", async () => {
    apiClient.get.mockResolvedValue({ json: async () => ({ license: ["hitchhiker/heart-of-gold"] }) });
    configLink({
      isReadOnly: true,
      initialConfiguration: makeGlobalConfig({ predefinedKeys: { license: makePredefinedKey({ mode: "MANDATORY" }) } }),
    });
    renderInProviders(<GlobalConfiguration link="/config" />);

    expect(screen.getByText("license")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: t("scm-custom-properties-plugin.config.general.enabled.edit.ariaLabel") }),
    ).toBeNull();
    // no interactive "show missing repos" button in read-only mode
    await waitFor(() =>
      expect(
        screen.queryByRole("button", {
          name: t("scm-custom-properties-plugin.config.missingMandatoryProperties.ariaLabelShowMissingRepos", { key: "license" }),
        }),
      ).toBeNull(),
    );
  });
});
