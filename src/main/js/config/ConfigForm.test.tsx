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
import { screen, waitFor, fireEvent, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ConfigForm, { toRecord } from "./ConfigForm";
import { makeGlobalConfig, makePredefinedKey, renderInProviders, t } from "../../../test/testUtils";

const renderConfigForm = (props: Record<string, unknown> = {}) => {
  const update = vi.fn().mockResolvedValue(undefined);
  const config = makeGlobalConfig({
    predefinedKeys: {
      alpha: makePredefinedKey({ mode: "NONE" }),
      ...(props.predefinedKeys as Record<string, unknown>),
    },
  });
  const view = renderInProviders(
    <ConfigForm
      level="global"
      config={config}
      update={update}
      isReadOnly={(props.isReadOnly as boolean) ?? false}
      isUpdating={false}
      missingProperties={{}}
      isMissingPropertiesLoading={false}
    />,
  );
  return { update, config, ...view };
};

describe("ConfigForm (global)", () => {
  it("lists predefined keys with their value-mode column", () => {
    renderConfigForm({
      predefinedKeys: {
        mand: makePredefinedKey({ mode: "MANDATORY" }),
        def: makePredefinedKey({ mode: "DEFAULT", defaultValue: "x" }),
        multi: makePredefinedKey({ mode: "MULTIPLE_CHOICE", allowedValues: ["red", "blue"] }),
      },
    });

    expect(screen.getByText("mand")).toBeInTheDocument();
    expect(screen.getByText(t("scm-custom-properties-plugin.editor.mandatoryValueTag"))).toBeInTheDocument();
    expect(screen.getByText(`${t("scm-custom-properties-plugin.editor.defaultValueTag")}: x`)).toBeInTheDocument();
    expect(screen.getByText(t("scm-custom-properties-plugin.editor.multipleValueTag"))).toBeInTheDocument();
    // the allowed-values column lists each configured value
    expect(screen.getByText("red")).toBeInTheDocument();
    expect(screen.getByText("blue")).toBeInTheDocument();
  });

  it("shows an empty-state message when no keys are predefined", () => {
    const config = makeGlobalConfig({ predefinedKeys: {} });
    renderInProviders(
      <ConfigForm
        level="global"
        config={config}
        update={vi.fn().mockResolvedValue(undefined)}
        isReadOnly={false}
        isUpdating={false}
        missingProperties={{}}
        isMissingPropertiesLoading={false}
      />,
    );

    expect(screen.getByText(t("scm-custom-properties-plugin.config.predefinedKeys.empty"))).toBeInTheDocument();
  });

  it("hides the empty-state message once a key is predefined", () => {
    renderConfigForm(); // seeds the key "alpha"

    expect(screen.queryByText(t("scm-custom-properties-plugin.config.predefinedKeys.empty"))).toBeNull();
  });

  it("opens the add-key subpage", async () => {
    const user = userEvent.setup();
    renderConfigForm();

    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.table.footer.addKey") }));

    expect(await screen.findByLabelText(t("scm-custom-properties-plugin.config.general.addedKey.label"))).toBeInTheDocument();
    expect(
      screen.getByRole("radio", { name: t("scm-custom-properties-plugin.config.general.mandatoryOrDefault.NONE") }),
    ).toBeInTheDocument();
  });

  it("shows a validation error for a duplicate key in the add-key subpage", async () => {
    const user = userEvent.setup();
    renderConfigForm(); // already contains the key "alpha"

    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.table.footer.addKey") }));
    await user.type(await screen.findByLabelText(t("scm-custom-properties-plugin.config.general.addedKey.label")), "alpha");

    expect(await screen.findByText(t("scm-custom-properties-plugin.editor.key.alreadyExists"))).toBeInTheDocument();
  });

  it("reveals the default-value field when switching a key to DEFAULT mode", async () => {
    renderConfigForm();

    fireEvent.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.table.body.edit", { key: "alpha" }) }));
    expect(await screen.findByLabelText(t("scm-custom-properties-plugin.config.general.addedKey.label"))).toHaveValue("alpha");

    fireEvent.click(screen.getByRole("radio", { name: t("scm-custom-properties-plugin.config.general.mandatoryOrDefault.DEFAULT") }));

    expect(await screen.findByLabelText(t("scm-custom-properties-plugin.config.general.defaultValue.label"))).toBeInTheDocument();
  });

  it("switches the allowed-values label to the required variant for MULTIPLE_CHOICE", async () => {
    renderConfigForm();

    fireEvent.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.table.body.edit", { key: "alpha" }) }));
    await screen.findByLabelText(t("scm-custom-properties-plugin.config.general.addedKey.label"));

    fireEvent.click(
      screen.getByRole("radio", {
        name: t("scm-custom-properties-plugin.config.general.mandatoryOrDefault.MULTIPLE_CHOICE"),
      }),
    );

    expect(
      await screen.findByText(t("scm-custom-properties-plugin.config.general.allowedValues.labelRequired")),
    ).toBeInTheDocument();
  });

  it("shows a check indicator for a mandatory key that no repository is missing", () => {
    renderConfigForm({ predefinedKeys: { license: makePredefinedKey({ mode: "MANDATORY" }) } });

    expect(
      screen.getByLabelText(
        t("scm-custom-properties-plugin.config.missingMandatoryProperties.ariaLabelNoMissingRepos", { key: "license" }),
      ),
    ).toBeInTheDocument();
  });

  it("offers a default-value select once allowed values exist and mode is DEFAULT", async () => {
    const user = userEvent.setup();
    renderConfigForm();

    fireEvent.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.table.body.edit", { key: "alpha" }) }));
    await screen.findByLabelText(t("scm-custom-properties-plugin.config.general.addedKey.label"));

    const allowedValues = screen.getByLabelText(t("scm-custom-properties-plugin.config.general.allowedValues.label"));
    await user.type(allowedValues, "red{Enter}");

    fireEvent.click(screen.getByRole("radio", { name: t("scm-custom-properties-plugin.config.general.mandatoryOrDefault.DEFAULT") }));

    const defaultValue = await screen.findByLabelText(t("scm-custom-properties-plugin.config.general.defaultValue.label"));
    expect(defaultValue.tagName).toBe("SELECT");
    expect(within(defaultValue as HTMLElement).getByRole("option", { name: "red" })).toBeInTheDocument();
  });

  it("does not delete a predefined key when the deletion is cancelled", async () => {
    const user = userEvent.setup();
    const { update } = renderConfigForm();

    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.table.body.delete", { key: "alpha" }) }));
    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.modal.deletePredefinedKey.cancel") }));

    expect(update).not.toHaveBeenCalled();
  });

  it("deletes a predefined key", async () => {
    const user = userEvent.setup();
    const { update } = renderConfigForm();

    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.table.body.delete", { key: "alpha" }) }));
    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.modal.deletePredefinedKey.submit") }));

    await waitFor(() => expect(update).toHaveBeenCalledWith(expect.objectContaining({ predefinedKeys: {} })));
  });

  it("hides edit and add controls in read-only mode", () => {
    renderConfigForm({ isReadOnly: true });

    expect(screen.getByText("alpha")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: t("scm-custom-properties-plugin.table.body.edit", { key: "alpha" }) })).toBeNull();
    expect(screen.queryByRole("button", { name: t("scm-custom-properties-plugin.table.footer.addKey") })).toBeNull();
  });

  it("saves a mandatory key without allowed values as an empty array, not null", async () => {
    const user = userEvent.setup();
    const { update } = renderConfigForm();

    await user.click(screen.getByRole("button", { name: t("scm-custom-properties-plugin.table.footer.addKey") }));
    await user.type(await screen.findByLabelText(t("scm-custom-properties-plugin.config.general.addedKey.label")), "jep2");
    await user.click(
      screen.getByRole("radio", {
        name: t("scm-custom-properties-plugin.config.general.mandatoryOrDefault.MANDATORY"),
      }),
    );
    await user.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith(
        expect.objectContaining({
          predefinedKeys: expect.objectContaining({ jep2: expect.objectContaining({ allowedValues: [] }) }),
        }),
      ),
    );
  });

  it("does not echo back null allowed values for a key that was loaded without them", async () => {
    const user = userEvent.setup();
    // A key persisted without allowed values may reach the client with allowedValues === null.
    const { update } = renderConfigForm({
      predefinedKeys: { jep2: { allowedValues: null, defaultValue: "", mode: "MANDATORY" } },
    });

    // The overview table must render such a key without crashing.
    expect(screen.getByText("jep2")).toBeInTheDocument();

    // Saving any part of the config re-sends every key, including the untouched one.
    await user.click(
      screen.getByRole("button", { name: t("scm-custom-properties-plugin.config.general.enabled.edit.ariaLabel") }),
    );
    await user.click(
      await screen.findByRole("radio", { name: t("scm-custom-properties-plugin.config.general.enabled.radio.false") }),
    );
    await user.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith(
        expect.objectContaining({
          predefinedKeys: expect.objectContaining({ jep2: expect.objectContaining({ allowedValues: [] }) }),
        }),
      ),
    );
  });

  it("drops an orphaned empty-key entry so no phantom row is shown on the overview", async () => {
    // An empty-key entry can be left behind by leaving the add-key subpage via the browser Back
    // button (which skips closeSubpage's reset). It must not survive on the overview.
    renderConfigForm({ predefinedKeys: { "": makePredefinedKey(), alpha: makePredefinedKey() } });

    expect(screen.getByText("alpha")).toBeInTheDocument();
    // Only the "alpha" row remains once the empty placeholder has been pruned.
    await waitFor(() =>
      expect(
        screen.getAllByRole("button", { name: /^Edit entry with the key/ }),
      ).toHaveLength(1),
    );
    expect(
      screen.queryByRole("button", { name: t("scm-custom-properties-plugin.table.body.edit", { key: "" }) }),
    ).toBeNull();
  });
});

describe("toRecord", () => {
  it("omits entries with an empty key so no empty map key is sent to the backend", () => {
    const record = toRecord([
      { key: "", allowedValues: [], defaultValue: "", mode: "NONE" },
      { key: "alpha", allowedValues: ["x"], defaultValue: "", mode: "NONE" },
    ]);

    expect(Object.keys(record)).toEqual(["alpha"]);
  });
});
