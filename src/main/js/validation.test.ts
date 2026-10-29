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
import { validateKey } from "./validation";

const never = () => false;

describe("validateKey", () => {
  it("accepts a valid key", () => {
    expect(validateKey("valid.key-1:2@a/b", undefined, never)).toBeUndefined();
  });

  it("reports an existing key", () => {
    expect(validateKey("taken", undefined, (key) => key === "taken")).toBe("alreadyExists");
  });

  it("allows keeping the original key even if it 'exists'", () => {
    expect(validateKey("taken", "taken", (key) => key === "taken")).toBeUndefined();
  });

  it("reports a key that exceeds 255 characters", () => {
    expect(validateKey("a".repeat(256), undefined, never)).toBe("tooLong");
  });

  it("reports invalid characters", () => {
    expect(validateKey("not/allowed!", undefined, never)).toBe("invalidChars");
  });
});
