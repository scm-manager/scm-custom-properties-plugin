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
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { apiClient } from "../../../test/mocks/ui-api";
import CustomPropertiesOverview from "./CustomPropertiesOverview";
import { BASE, makeProperty, makeRepository, renderWithProviders, t } from "../../../test/testUtils";

describe("CustomPropertiesOverview", () => {
  it("renders the section header and an add link when creation is allowed", () => {
    renderWithProviders(<CustomPropertiesOverview repository={makeRepository()} />);

    expect(screen.getByText(t("scm-custom-properties-plugin.navLink"))).toBeInTheDocument();
    expect(document.querySelector('a[href*="mode=add"]')).toBeTruthy();
  });

  it("hides the add link when creation is not allowed", () => {
    renderWithProviders(<CustomPropertiesOverview repository={makeRepository({ create: false })} />);

    expect(document.querySelector('a[href*="mode=add"]')).toBeNull();
  });

  it("shows an empty-state message when there are no properties and none are missing", () => {
    renderWithProviders(<CustomPropertiesOverview repository={makeRepository({ properties: [], missing: [] })} />);

    expect(screen.getByText(t("scm-custom-properties-plugin.table.empty"))).toBeInTheDocument();
  });

  it("hides the empty-state message once a property exists", () => {
    renderWithProviders(
      <CustomPropertiesOverview repository={makeRepository({ properties: [makeProperty({ key: "lang" })] })} />,
    );

    expect(screen.queryByText(t("scm-custom-properties-plugin.table.empty"))).toBeNull();
  });

  it("hides the empty-state message when only a mandatory property is missing", () => {
    renderWithProviders(
      <CustomPropertiesOverview repository={makeRepository({ properties: [], missing: ["license"] })} />,
    );

    expect(screen.queryByText(t("scm-custom-properties-plugin.table.empty"))).toBeNull();
  });

  it("lists existing custom properties", () => {
    renderWithProviders(
      <CustomPropertiesOverview repository={makeRepository({ properties: [makeProperty({ key: "lang", value: "java" })] })} />,
    );

    expect(screen.getByRole("cell", { name: "lang" })).toBeInTheDocument();
    expect(screen.getByText("java")).toBeInTheDocument();
  });

  it("lists a missing mandatory property with an edit deeplink (must-have)", () => {
    renderWithProviders(
      <CustomPropertiesOverview repository={makeRepository({ properties: [], missing: ["license"] })} />,
    );

    expect(screen.getByText(t("scm-custom-properties-plugin.table.header.missingProperty"))).toBeInTheDocument();
    const editLink = screen.getByRole("link", {
      name: t("scm-custom-properties-plugin.table.body.edit", { key: "license" }),
    });
    expect(editLink).toHaveAttribute("href", expect.stringContaining("missingProperty=license"));
  });

  it("opens the create subpage when clicking add", async () => {
    const user = userEvent.setup();
    renderWithProviders(<CustomPropertiesOverview repository={makeRepository()} />);

    await user.click(document.querySelector('a[href*="mode=add"]') as Element);

    // breadcrumb now shows the "create" crumb and the editor key field is visible
    expect(await screen.findByText(t("scm-custom-properties-plugin.editor.subtitle_create"))).toBeInTheDocument();
    expect(screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label"))).toBeInTheDocument();
    // the table's own add link is gone while the subpage is open
    expect(document.querySelector('a[href*="mode=add"]')).toBeNull();
  });

  it("opens the edit subpage for an existing property", async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <CustomPropertiesOverview repository={makeRepository({ properties: [makeProperty({ key: "lang", value: "java" })] })} />,
    );

    await user.click(screen.getByRole("link", { name: t("scm-custom-properties-plugin.table.body.edit", { key: "lang" }) }));

    expect(
      await screen.findByText(t("scm-custom-properties-plugin.editor.subtitle_edit", { key: "lang" })),
    ).toBeInTheDocument();
  });

  it("deletes a property after confirming the dialog", async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <CustomPropertiesOverview repository={makeRepository({ properties: [makeProperty({ key: "lang" })] })} />,
    );

    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.table.body.delete", { key: "lang" }) }));
    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.modal.deleteCustomProperty.submit") }));

    await waitFor(() =>
      expect(apiClient.httpRequestWithJSONBody).toHaveBeenCalledWith(
        "DELETE",
        "/api/v2/props/lang",
        expect.any(String),
        {},
      ),
    );
  });

  it("returns to the table when clicking the root breadcrumb", async () => {
    const user = userEvent.setup();
    renderWithProviders(<CustomPropertiesOverview repository={makeRepository()} />, `${BASE}?mode=add`);

    // subpage is open
    expect(screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label"))).toBeInTheDocument();

    await user.click(screen.getByRole("link", { name: t("scm-custom-properties-plugin.repository.subtitle") }));

    // back on the table
    expect(await screen.findByText(t("scm-custom-properties-plugin.navLink"))).toBeInTheDocument();
    expect(document.querySelector('a[href*="mode=add"]')).toBeTruthy();
  });

  it("shows a default-value tag and a default-property edit deeplink", () => {
    renderWithProviders(
      <CustomPropertiesOverview
        repository={makeRepository({ properties: [makeProperty({ key: "lang", value: "java", defaultProperty: true, _links: {} })] })}
      />,
    );

    expect(screen.getByText(t("scm-custom-properties-plugin.editor.defaultValueTag"))).toBeInTheDocument();
    const link = screen.getByRole("link", { name: t("scm-custom-properties-plugin.table.body.edit", { key: "lang" }) });
    expect(link).toHaveAttribute("href", expect.stringContaining("defaultProperty=lang"));
  });

  it("shows a mandatory-value tag", () => {
    renderWithProviders(
      <CustomPropertiesOverview
        repository={makeRepository({ properties: [makeProperty({ key: "lang", value: "java", mandatory: true })] })}
      />,
    );

    expect(screen.getByText(t("scm-custom-properties-plugin.editor.mandatoryValueTag"))).toBeInTheDocument();
  });

  it("renders multi-value properties as a list", () => {
    renderWithProviders(
      <CustomPropertiesOverview
        repository={makeRepository({ properties: [makeProperty({ key: "langs", value: "java\tkotlin", separator: "\t" })] })}
      />,
    );

    expect(screen.getByText("java")).toBeInTheDocument();
    expect(screen.getByText("kotlin")).toBeInTheDocument();
  });

  it("omits edit and delete actions when the property has no links", () => {
    renderWithProviders(
      <CustomPropertiesOverview
        repository={makeRepository({ properties: [makeProperty({ key: "lang", value: "java", _links: {} })] })}
      />,
    );

    expect(screen.queryByRole("link", { name: t("scm-custom-properties-plugin.table.body.edit", { key: "lang" }) })).toBeNull();
    expect(screen.queryByRole("button", { name: t("scm-custom-properties-plugin.table.body.delete", { key: "lang" }) })).toBeNull();
  });

  it("does not offer an edit action for missing properties without create permission", () => {
    renderWithProviders(
      <CustomPropertiesOverview repository={makeRepository({ properties: [], missing: ["license"], create: false })} />,
    );

    expect(screen.getByText("license")).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: t("scm-custom-properties-plugin.table.body.edit", { key: "license" }) }),
    ).toBeNull();
  });

  it("renders properties sorted alphabetically by key", () => {
    renderWithProviders(
      <CustomPropertiesOverview
        repository={makeRepository({
          properties: [
            makeProperty({ key: "zebra", value: "z" }),
            makeProperty({ key: "alpha", value: "a" }),
          ],
        })}
      />,
    );

    const rows = screen.getAllByRole("row");
    // rows[0] is the header row; the first two data rows must be alpha then zebra
    expect(within(rows[1]).getByText("alpha")).toBeInTheDocument();
    expect(within(rows[2]).getByText("zebra")).toBeInTheDocument();
  });

  it("scrolls the missing-properties section into view when linked with the hash", () => {
    const scrollSpy = vi.spyOn(Element.prototype, "scrollIntoView");
    renderWithProviders(
      <CustomPropertiesOverview repository={makeRepository({ properties: [], missing: ["beta", "alpha"] })} />,
      `${BASE}#missing-properties`,
    );

    // both missing keys are sorted and shown, and the hash triggers a scroll into view
    const rows = screen.getAllByRole("row");
    expect(within(rows[rows.length - 2]).getByText("alpha")).toBeInTheDocument();
    expect(within(rows[rows.length - 1]).getByText("beta")).toBeInTheDocument();
    expect(scrollSpy).toHaveBeenCalled();
  });

  it("stays on the active subpage when clicking the current breadcrumb crumb", async () => {
    const user = userEvent.setup();
    renderWithProviders(<CustomPropertiesOverview repository={makeRepository()} />, `${BASE}?mode=add`);

    await user.click(screen.getByRole("link", { name: t("scm-custom-properties-plugin.editor.subtitle_create") }));

    // clicking the current (active) crumb is a no-op: the editor subpage stays open
    expect(screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label"))).toBeInTheDocument();
  });

  it("does not delete when the dialog is cancelled", async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <CustomPropertiesOverview repository={makeRepository({ properties: [makeProperty({ key: "lang" })] })} />,
    );

    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.table.body.delete", { key: "lang" }) }));
    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.modal.deleteCustomProperty.cancel") }));

    expect(apiClient.httpRequestWithJSONBody).not.toHaveBeenCalled();
  });
});
