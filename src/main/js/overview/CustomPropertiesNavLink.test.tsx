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
import CustomPropertiesNavLink from "./CustomPropertiesNavLink";
import { CustomPropertiesPredicate } from "../index";
import { makeRepository, renderInProviders, t } from "../../../test/testUtils";

describe("CustomPropertiesNavLink", () => {
  it("links to the repository custom-properties page", () => {
    renderInProviders(<CustomPropertiesNavLink repository={makeRepository()} />);

    const link = screen.getByRole("link", { name: t("scm-custom-properties-plugin.navLink") });
    expect(link).toHaveAttribute("href", "/repo/hitchhiker/heart-of-gold/custom-properties");
  });

  it("is not contributed to the navigation when the plugin is globally disabled", () => {
    const disabledRepository = { ...makeRepository(), _embedded: {} };
    expect(CustomPropertiesPredicate({ repository: disabledRepository })).toBe(false);

  if (CustomPropertiesPredicate({ repository: disabledRepository })) {
      renderInProviders(<CustomPropertiesNavLink repository={disabledRepository} />);
    }
    expect(screen.queryByRole("link", { name: t("scm-custom-properties-plugin.navLink") })).toBeNull();
  });
});
