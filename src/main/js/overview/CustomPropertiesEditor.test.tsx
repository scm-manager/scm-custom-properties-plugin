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
import { describe, it, expect } from "vitest";
import { screen, waitFor, fireEvent, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { apiClient } from "../../../test/mocks/ui-api";
import CustomPropertiesEditor from "./CustomPropertiesEditor";
import { BASE, makeProperty, makeRepository, renderWithProviders, t } from "../../../test/testUtils";

const submitForm = () => fireEvent.submit(document.getElementById("form-custom-property") as HTMLFormElement);

describe("CustomPropertiesEditor", () => {
  it("creates a new property via POST to the create link", async () => {
    const user = userEvent.setup();
    renderWithProviders(<CustomPropertiesEditor repository={makeRepository({ properties: [] })} />, `${BASE}?mode=add`);

    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label")), "license");
    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.value.label")), "MIT");

    expect(screen.getByRole("button", { name: t("scm-custom-properties-plugin.config.general.submit") })).toBeEnabled();
    submitForm();

    await waitFor(() =>
      expect(apiClient.post).toHaveBeenCalledWith(
        "/api/v2/props",
        expect.objectContaining({ key: "license", value: "MIT" }),
        expect.any(String),
      ),
    );
  });

  it("presets the key read-only and creates a missing mandatory property (must-have)", async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <CustomPropertiesEditor repository={makeRepository({ properties: [] })} />,
      `${BASE}?missingProperty=license`,
    );

    const keyField = screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label"));
    await waitFor(() => expect(keyField).toHaveValue("license"));
    expect(keyField).toHaveAttribute("readonly");

    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.value.label")), "MIT");
    submitForm();

    await waitFor(() =>
      expect(apiClient.post).toHaveBeenCalledWith(
        "/api/v2/props",
        expect.objectContaining({ key: "license", value: "MIT" }),
        expect.any(String),
      ),
    );
  });

  it("edits an existing property via PUT to the update link", async () => {
    const user = userEvent.setup();
    const repository = makeRepository({
      properties: [
        makeProperty({
          key: "lang",
          value: "java",
          _links: { update: { href: "/api/v2/props/lang" }, delete: { href: "/api/v2/props/lang" } },
        }),
      ],
    });
    renderWithProviders(<CustomPropertiesEditor repository={repository} />, `${BASE}?key=lang`);

    const valueField = screen.getByLabelText(t("scm-custom-properties-plugin.editor.value.label"));
    await waitFor(() => expect(valueField).toHaveValue("java"));

    await user.clear(valueField);
    await user.type(valueField, "kotlin");
    submitForm();

    await waitFor(() =>
      expect(apiClient.put).toHaveBeenCalledWith(
        "/api/v2/props/lang",
        expect.objectContaining({ key: "lang", value: "kotlin" }),
        expect.any(String),
      ),
    );
  });

  it("returns to the overview URL (without /modify) after creating", async () => {
    const user = userEvent.setup();
    renderWithProviders(<CustomPropertiesEditor repository={makeRepository({ properties: [] })} />, `${BASE}?mode=add`);

    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label")), "license");
    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.value.label")), "MIT");
    submitForm();

    await waitFor(() => expect(screen.getByTestId("location").textContent).toBe(BASE));
  });

  it("keeps submit disabled while an edited property is unchanged", async () => {
    const repository = makeRepository({ properties: [makeProperty({ key: "lang", value: "java" })] });
    renderWithProviders(<CustomPropertiesEditor repository={repository} />, `${BASE}?key=lang`);

    const valueField = screen.getByLabelText(t("scm-custom-properties-plugin.editor.value.label"));
    await waitFor(() => expect(valueField).toHaveValue("java"));

    expect(screen.getByRole("button", { name: t("scm-custom-properties-plugin.config.general.submit") })).toBeDisabled();
  });

  it("shows an error and disables submit when the key already exists", async () => {
    const user = userEvent.setup();
    const repository = makeRepository({ properties: [makeProperty({ key: "lang", value: "java" })] });
    renderWithProviders(<CustomPropertiesEditor repository={repository} />, `${BASE}?mode=add`);

    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label")), "lang");
    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.value.label")), "x");

    expect(screen.getByText(t("scm-custom-properties-plugin.editor.key.alreadyExists"))).toBeInTheDocument();
    expect(screen.getByRole("button", { name: t("scm-custom-properties-plugin.config.general.submit") })).toBeDisabled();
  });

  it("renders a select for a predefined key with allowed values", async () => {
    apiClient.get.mockResolvedValue({
      json: async () => ({ lang: { allowedValues: ["java", "kotlin"], defaultValue: "", mode: "NONE" } }),
    });
    const repository = makeRepository({ properties: [makeProperty({ key: "lang", value: "java" })] });
    renderWithProviders(<CustomPropertiesEditor repository={repository} />, `${BASE}?key=lang`);

    await waitFor(() => expect((screen.getByLabelText(t("scm-custom-properties-plugin.editor.value.label")) as HTMLElement).tagName).toBe("SELECT"));
    expect(screen.getByRole("option", { name: "kotlin" })).toBeInTheDocument();
  });

  it("preselects existing values of a multiple-choice property and submits the joined value", async () => {
    const user = userEvent.setup();
    apiClient.get.mockResolvedValue({
      json: async () => ({ lang: { allowedValues: ["java", "kotlin", "scala"], defaultValue: "", mode: "MULTIPLE_CHOICE" } }),
    });
    const repository = makeRepository({
      properties: [makeProperty({ key: "lang", value: "java\tkotlin", separator: "\t" })],
    });
    renderWithProviders(<CustomPropertiesEditor repository={repository} />, `${BASE}?key=lang`);

    const java = await screen.findByRole("checkbox", { name: "java" });
    await waitFor(() => expect(java).toBeChecked());
    expect(screen.getByRole("checkbox", { name: "kotlin" })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: "scala" })).not.toBeChecked();

    await user.click(screen.getByRole("checkbox", { name: "scala" }));
    submitForm();

    await waitFor(() =>
      expect(apiClient.put).toHaveBeenCalledWith(
        "/api/v2/props/lang",
        expect.objectContaining({ value: "java\tkotlin\tscala" }),
        expect.any(String),
      ),
    );
  });

  it("requires at least one value for a multiple-choice property", async () => {
    const user = userEvent.setup();
    apiClient.get.mockResolvedValue({
      json: async () => ({ lang: { allowedValues: ["java", "kotlin"], defaultValue: "", mode: "MULTIPLE_CHOICE" } }),
    });
    const repository = makeRepository({
      properties: [makeProperty({ key: "lang", value: "java", separator: "\t" })],
    });
    renderWithProviders(<CustomPropertiesEditor repository={repository} />, `${BASE}?key=lang`);

    const java = await screen.findByRole("checkbox", { name: "java" });
    await waitFor(() => expect(java).toBeChecked());

    await user.click(java); // uncheck the only selected value
    expect(screen.getByRole("button", { name: t("scm-custom-properties-plugin.config.general.submit") })).toBeDisabled();
  });

  it("locks the key read-only when editing a mandatory property", async () => {
    apiClient.get.mockResolvedValue({
      json: async () => ({ lang: { allowedValues: [], defaultValue: "", mode: "MANDATORY" } }),
    });
    const repository = makeRepository({ properties: [makeProperty({ key: "lang", value: "java" })] });
    renderWithProviders(<CustomPropertiesEditor repository={repository} />, `${BASE}?key=lang`);

    const keyField = screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label"));
    await waitFor(() => expect(keyField).toHaveValue("lang"));
    await waitFor(() => expect(keyField).toHaveAttribute("readonly"));
  });

  it("suggests predefined keys and the current filter in the key combobox", async () => {
    const user = userEvent.setup();
    apiClient.get.mockResolvedValue({
      json: async () => ({
        lang: { allowedValues: [], defaultValue: "", mode: "NONE" },
        scala: { allowedValues: [], defaultValue: "", mode: "NONE" },
      }),
    });
    renderWithProviders(<CustomPropertiesEditor repository={makeRepository({ properties: [] })} />, `${BASE}?mode=add`);

    const suggestions = screen.getByLabelText("key-suggestions");
    await waitFor(() => expect(within(suggestions).getByText("lang")).toBeInTheDocument());
    expect(within(suggestions).getByText("scala")).toBeInTheDocument();

    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label")), "brandnew");
    expect(within(suggestions).getByText("brandnew")).toBeInTheDocument();
  });

  it("shows an error notification and stays on the form when creation fails", async () => {
    const user = userEvent.setup();
    apiClient.post.mockRejectedValue(new Error("Backend refused the property"));
    renderWithProviders(<CustomPropertiesEditor repository={makeRepository({ properties: [] })} />, `${BASE}?mode=add`);

    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label")), "license");
    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.value.label")), "MIT");
    submitForm();

    expect(await screen.findByRole("alert")).toHaveTextContent("Backend refused the property");
    // no navigation back to the overview happened
    expect(screen.getByTestId("location").textContent).toBe(`${BASE}?mode=add`);
  });

  it("rejects a key with invalid characters", async () => {
    const user = userEvent.setup();
    renderWithProviders(<CustomPropertiesEditor repository={makeRepository({ properties: [] })} />, `${BASE}?mode=add`);

    await user.type(screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label")), "not/allowed!");

    expect(
      screen.getByText(t("scm-custom-properties-plugin.editor.key.invalidChars")),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: t("scm-custom-properties-plugin.config.general.submit") })).toBeDisabled();
  });

  it("prefills key and value from a default property and creates a concrete one", async () => {
    const repository = makeRepository({
      properties: [makeProperty({ key: "lang", value: "java", defaultProperty: true, _links: {} })],
    });
    renderWithProviders(<CustomPropertiesEditor repository={repository} />, `${BASE}?defaultProperty=lang`);

    await waitFor(() => expect(screen.getByLabelText(t("scm-custom-properties-plugin.editor.key.label"))).toHaveValue("lang"));
    expect(screen.getByLabelText(t("scm-custom-properties-plugin.editor.value.label"))).toHaveValue("java");

    submitForm();

    await waitFor(() =>
      expect(apiClient.post).toHaveBeenCalledWith(
        "/api/v2/props",
        expect.objectContaining({ key: "lang", value: "java" }),
        expect.any(String),
      ),
    );
  });
});
