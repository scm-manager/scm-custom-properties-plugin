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

import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/react";
import { binder } from "@scm-manager/ui-extensions";
import { ConfigurationBinder } from "@scm-manager/ui-components";
import { useRepositoryFromParams } from "@scm-manager/ui-core";
import { renderInProviders, BASE, t } from "../../test/testUtils";
// Importing the entry point runs its extension-point registrations against the (mocked) binder.
import "./index";

const bindCall = (extensionPoint: string) =>
  (binder.bind as any).mock.calls.find((call: unknown[]) => call[0] === extensionPoint);

describe("plugin registration (index)", () => {
  it("registers all repository extension points and the config bindings", () => {
    const points = (binder.bind as any).mock.calls.map((call: unknown[]) => call[0]);
    expect(points).toEqual(
      expect.arrayContaining([
        "repository.navigation",
        "repository.route",
        "repository.banner",
        "search.hit.indexedCustomProperty.renderer",
      ]),
    );
    expect(ConfigurationBinder.bindAdminSetting).toHaveBeenCalled();
    expect(ConfigurationBinder.bindNamespaceSetting).toHaveBeenCalled();
  });

  it("gates navigation and banner on the customProperties embedding via the same predicate", () => {
    const predicate = bindCall("repository.navigation")[2];
    expect(predicate({ repository: { _embedded: { customProperties: {} } } })).toBe(true);
    expect(predicate({ repository: { _embedded: {} } })).toBe(false);
    expect(predicate({ repository: {} })).toBe(false);
    // the banner uses the very same predicate
    expect(bindCall("repository.banner")[2]).toBe(predicate);
  });

  it("exposes the custom-properties route and renders the overview when embedded", () => {
    const routes = bindCall("repository.route")[1]();
    expect(routes).toHaveLength(1);
    expect(routes[0].path).toBe("custom-properties");

    (useRepositoryFromParams as any).mockReturnValue({
      namespace: "hitchhiker",
      name: "heart-of-gold",
      _embedded: { customProperties: { properties: [] }, missingMandatoryProperties: { missing: [] } },
    });
    renderInProviders(routes[0].element, BASE);

    expect(screen.getByText(t("scm-custom-properties-plugin.navLink"))).toBeInTheDocument();
  });

  it("redirects away from the route when customProperties are not embedded", () => {
    const routes = bindCall("repository.route")[1]();
    (useRepositoryFromParams as any).mockReturnValue({
      namespace: "hitchhiker",
      name: "heart-of-gold",
      _embedded: {},
    });
    renderInProviders(routes[0].element, BASE);

    // redirected -> the overview header is not rendered
    expect(screen.queryByText(t("scm-custom-properties-plugin.navLink"))).toBeNull();
  });
});
