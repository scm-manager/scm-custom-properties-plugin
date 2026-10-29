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
import { screen } from "@testing-library/react";
import CustomPropertyHitRenderer from "./CustomPropertyHitRenderer";
import { renderInProviders, t } from "../../../test/testUtils";

const hit = (overrides: Record<string, any> = {}) => ({
  _embedded: { repository: { namespace: "hitchhiker", name: "heart-of-gold" } },
  fields: { key: { value: "lang" }, value: { value: "java" } },
  ...overrides,
});

describe("CustomPropertyHitRenderer", () => {
  it("renders the repository link, key and value of a search hit", () => {
    renderInProviders(<CustomPropertyHitRenderer hit={hit() as any} />);

    const link = screen.getByRole("link", { name: "hitchhiker/heart-of-gold" });
    expect(link).toHaveAttribute("href", "/repo/hitchhiker/heart-of-gold");
    expect(
      screen.getByText(t("scm-custom-properties-plugin.search.customProperty.key", { key: "lang" })),
    ).toBeInTheDocument();
    expect(
      screen.getByText(t("scm-custom-properties-plugin.search.customProperty.value", { value: "java" })),
    ).toBeInTheDocument();
  });

  it("shows an error notification when the hit has no repository", () => {
    renderInProviders(<CustomPropertyHitRenderer hit={{ _embedded: {}, fields: {} } as any} />);

    expect(screen.getByText(t("scm-custom-properties-plugin.search.invalidResult"))).toBeInTheDocument();
  });
});
