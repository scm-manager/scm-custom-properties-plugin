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

import { defineConfig } from "vitest/config";
import path from "node:path";

const mock = (file: string) => path.resolve(process.cwd(), "src/test/mocks", file);
const isCI = !!process.env.CI;

export default defineConfig({
  // The @scm-manager/ui-* packages are provided by the host at runtime (webpack "externals"),
  // so their transitive deps (@headlessui/react, ...) are not installed and they cannot be
  // imported in jsdom. For tests we alias them to lightweight mocks. react-router-dom and
  // @tanstack/react-query stay real so routing/navigation and query/mutation behaviour is exercised.
  resolve: {
    alias: {
      "@scm-manager/ui-core": mock("ui-core.tsx"),
      "@scm-manager/ui-components": mock("ui-components.tsx"),
      "@scm-manager/ui-api": mock("ui-api.ts"),
      "@scm-manager/ui-extensions": mock("ui-extensions.ts"),
      "@scm-manager/ui-types": mock("empty.ts"),
      "react-i18next": mock("react-i18next.tsx"),
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: [path.resolve(process.cwd(), "src/test/setup.ts")],
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
    exclude: ["**/node_modules/**", "**/dist/**", "**/.cache/**", "**/cypress/**"],
    reporters: ["default", "junit"],
    outputFile: {
      junit: "./build/jest-reports/TEST-vitest-report.xml",
    },
    coverage: {
      enabled: isCI,
      provider: "v8",
      reporter: ["text", "lcov", "html"],
      reportsDirectory: "./build/coverage",
      include: ["src/main/js/**/*.{ts,tsx}"],
      exclude: ["**/*.{test,spec}.{ts,tsx}"],
    },
  },
});
