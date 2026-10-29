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
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import MissingPropertyBanner from "./MissingPropertyBanner";
import { makeRepository, t } from "../../../test/testUtils";

const renderBanner = (repository: ReturnType<typeof makeRepository>, route: string) =>
  render(
    <MemoryRouter initialEntries={[route]}>
      <MissingPropertyBanner repository={repository} />
    </MemoryRouter>,
  );

describe("MissingPropertyBanner", () => {
  it("shows a warning and a deeplink to the missing properties when mandatory values are missing", () => {
    renderBanner(makeRepository({ missing: ["license"] }), "/repo/hitchhiker/heart-of-gold/info");

    expect(screen.getByText(t("scm-custom-properties-plugin.repository.banner.message"))).toBeInTheDocument();
    const link = screen.getByRole("link", { name: t("scm-custom-properties-plugin.repository.banner.link") });
    expect(link).toHaveAttribute("href", expect.stringContaining("/custom-properties"));
    expect(link).toHaveAttribute("href", expect.stringContaining("#missing-properties"));
  });

  it("hides itself while already on the custom-properties page", () => {
    renderBanner(makeRepository({ missing: ["license"] }), "/repo/hitchhiker/heart-of-gold/custom-properties");

    expect(screen.queryByText(t("scm-custom-properties-plugin.repository.banner.message"))).toBeNull();
  });

  it("renders nothing when no mandatory property is missing", () => {
    renderBanner(makeRepository({ missing: [] }), "/repo/hitchhiker/heart-of-gold/info");

    expect(screen.queryByText(t("scm-custom-properties-plugin.repository.banner.message"))).toBeNull();
  });

  it("renders nothing (and does not crash) when the missingMandatoryProperties embedding is absent", () => {
    // e.g. plugin disabled globally / no permission: the enriched data is not embedded.
    const repositoryWithoutEmbedding = { namespace: "hitchhiker", name: "heart-of-gold", _embedded: {} } as any;

    expect(() =>
      renderBanner(repositoryWithoutEmbedding, "/repo/hitchhiker/heart-of-gold/info"),
    ).not.toThrow();
    expect(screen.queryByText(t("scm-custom-properties-plugin.repository.banner.message"))).toBeNull();
  });

  it("renders nothing when the repository has no _embedded section at all", () => {
    const bareRepository = { namespace: "hitchhiker", name: "heart-of-gold" } as any;

    expect(() => renderBanner(bareRepository, "/repo/hitchhiker/heart-of-gold/info")).not.toThrow();
    expect(screen.queryByText(t("scm-custom-properties-plugin.repository.banner.message"))).toBeNull();
  });
});
