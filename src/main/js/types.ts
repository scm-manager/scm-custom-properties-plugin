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

import { HalRepresentation } from "@scm-manager/ui-types";

export type CustomProperty = {
  key: string;
  value: string;
  defaultProperty?: boolean;
  mandatory?: boolean;
  separator: string;
} & HalRepresentation;

export const valueModes = ["NONE", "DEFAULT", "MANDATORY", "MULTIPLE_CHOICE"] as const;
export type ValueMode = (typeof valueModes)[number];

export type SinglePredefinedKey = {
  allowedValues: string[];
  defaultValue: string;
  mode: ValueMode;
};

export type PredefinedKeys = Record<string, SinglePredefinedKey>;

export type BaseConfig = {
  predefinedKeys: PredefinedKeys;
} & HalRepresentation;

export type GlobalConfig = {
  enabled: boolean;
  enableNamespaceConfig: boolean;
} & BaseConfig;

export type NamespaceConfig = {
  globallyPredefinedKeys: PredefinedKeys;
} & BaseConfig;
