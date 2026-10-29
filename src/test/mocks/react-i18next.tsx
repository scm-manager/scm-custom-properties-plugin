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
import en from "../../main/resources/locales/en/plugins.json";

const resources: Record<string, unknown> = { plugins: en };

function lookup(ns: string, key: string): string | undefined {
  let current: any = resources[ns];
  for (const part of key.split(".")) {
    if (current == null) {
      return undefined;
    }
    current = current[part];
  }
  return typeof current === "string" ? current : undefined;
}

function interpolate(template: string, options?: Record<string, unknown>): string {
  if (!options) {
    return template;
  }
  return template.replace(/\{\{(\w+)\}\}/g, (_match, name) =>
    options[name] !== undefined ? String(options[name]) : `{{${name}}}`,
  );
}

export function useTranslation(ns: string | string[] = "plugins") {
  const namespace = Array.isArray(ns) ? ns[0] : ns;
  const t = (key: string, options?: Record<string, unknown>) => {
    const found = lookup(namespace, key) ?? lookup("plugins", key);
    return found !== undefined ? interpolate(found, options) : key;
  };
  return [t, { language: "en", changeLanguage: () => Promise.resolve() }] as const;
}

export const Trans: React.FC<{ children?: React.ReactNode }> = ({ children }) => <>{children}</>;

export const initReactI18next = { type: "3rdParty", init: () => undefined };
