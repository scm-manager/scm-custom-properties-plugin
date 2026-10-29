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

import React, { FC } from "react";
import { Controller } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { ChipInputField, Input, Label, RadioGroup, SelectField, Subtitle, useScmFormContext } from "@scm-manager/ui-core";
import { Option } from "@scm-manager/ui-types";
import { PredefinedKeyEntry, ValueMode, valueModes } from "../types";
import { validateKey } from "../validation";
import { Field, Row } from "../component/FormUtils";

type Props = {
  index: number;
  originalKey: string;
};

const fieldName = (index: number, field: keyof PredefinedKeyEntry) => `predefinedKeys.${index}.${field}` as const;

const otherKeys = (entries: PredefinedKeyEntry[], index: number) =>
  entries.filter((_, i) => i !== index).map((entry) => entry.key);

const PredefinedKeyFields: FC<Props> = ({ index, originalKey }) => {
  const [t] = useTranslation("plugins");
  const { control, watch, setValue, trigger, readOnly } = useScmFormContext();

  const mode = watch(fieldName(index, "mode")) as ValueMode;
  const allowedValues = (watch(fieldName(index, "allowedValues")) as string[]) ?? [];

  const asOptions = (values: string[]): Option<string>[] => values.map((value) => ({ label: value, value }));

  const ensureDefaultWithinAllowedValues = (nextAllowedValues: string[]) => {
    const currentDefault = watch(fieldName(index, "defaultValue")) as string;
    if (nextAllowedValues.length !== 0 && !nextAllowedValues.includes(currentDefault)) {
      setValue(fieldName(index, "defaultValue"), nextAllowedValues[0], { shouldDirty: true, shouldValidate: true });
    }
  };

  return (
    <>
      <Subtitle>{t("scm-custom-properties-plugin.config.general.predefinedKeys.subtitle")}</Subtitle>
      <Row>
        <Field>
          <Label htmlFor="predefinedKey-key">{t("scm-custom-properties-plugin.config.general.addedKey.label")}</Label>
          <Controller
            control={control}
            name={fieldName(index, "key")}
            rules={{
              validate: (value: string, formValues) => {
                if (!value) {
                  return false;
                }
                const others = otherKeys(formValues.predefinedKeys as PredefinedKeyEntry[], index);
                const validationResult = validateKey(value, originalKey, (key) => others.includes(key));
                return validationResult ? t(`scm-custom-properties-plugin.editor.key.${validationResult}`) : true;
              },
            }}
            render={({ field, fieldState }) => (
              <>
                <div className="control">
                  <Input
                    id="predefinedKey-key"
                    value={field.value ?? ""}
                    onChange={field.onChange}
                    onBlur={field.onBlur}
                    autoFocus
                    readOnly={readOnly}
                  />
                </div>
                {fieldState.error?.message ? <p className="help is-danger">{fieldState.error.message}</p> : null}
              </>
            )}
          />
        </Field>
      </Row>
      <Row style={{ paddingTop: "8px" }}>
        <Field>
          <Label as="legend" htmlFor="predefinedKey-mode" style={{ marginBottom: 0 }}>
            {t("scm-custom-properties-plugin.config.general.mandatoryOrDefault.label")}
          </Label>
          <div>
            <Controller
              control={control}
              name={fieldName(index, "mode")}
              render={({ field }) => (
                <RadioGroup
                  className="column"
                  id="predefinedKey-mode"
                  value={field.value}
                  onValueChange={(newMode: ValueMode) => {
                    field.onChange(newMode);
                    if (newMode === "DEFAULT") {
                      ensureDefaultWithinAllowedValues(allowedValues);
                    }
                    trigger([fieldName(index, "allowedValues"), fieldName(index, "defaultValue")]);
                  }}
                >
                  {valueModes.map((value: ValueMode) => (
                    <RadioGroup.Option
                      id={`predefinedKey-mode-${value}`}
                      key={value}
                      value={value}
                      label={t(`scm-custom-properties-plugin.config.general.mandatoryOrDefault.${value}`)}
                      disabled={readOnly}
                    />
                  ))}
                </RadioGroup>
              )}
            />
          </div>
        </Field>
      </Row>
      <Row>
        <Controller
          control={control}
          name={fieldName(index, "allowedValues")}
          rules={{
            validate: (value: string[], formValues) => {
              const currentMode = (formValues.predefinedKeys as PredefinedKeyEntry[])[index].mode;
              if (currentMode === "MULTIPLE_CHOICE" && (!value || value.length === 0)) {
                return t("scm-custom-properties-plugin.config.general.allowedValues.isRequiredError");
              }
              return true;
            },
          }}
          render={({ field, fieldState }) => (
            <ChipInputField<string>
              className="column"
              id="predefinedKey-allowedValues"
              label={t(
                mode === "MULTIPLE_CHOICE"
                  ? "scm-custom-properties-plugin.config.general.allowedValues.labelRequired"
                  : "scm-custom-properties-plugin.config.general.allowedValues.label",
              )}
              information={t("scm-custom-properties-plugin.config.general.allowedValues.note")}
              value={asOptions(field.value ?? [])}
              onChange={(options: Option<string>[]) => {
                const nextValues = options.map((option) => option.value);
                field.onChange(nextValues);
                ensureDefaultWithinAllowedValues(nextValues);
              }}
              readOnly={readOnly}
              error={fieldState.error?.message}
            />
          )}
        />
      </Row>
      {mode === "DEFAULT" ? (
        <Row>
          <Controller
            control={control}
            name={fieldName(index, "defaultValue")}
            rules={{
              validate: (value: string, formValues) => {
                const currentMode = (formValues.predefinedKeys as PredefinedKeyEntry[])[index].mode;
                return currentMode !== "DEFAULT" || value !== "" || false;
              },
            }}
            render={({ field }) =>
              allowedValues.length === 0 ? (
                <Field>
                  <Label htmlFor="predefinedKey-defaultValue">
                    {t("scm-custom-properties-plugin.config.general.defaultValue.label")}
                  </Label>
                  <div className="control">
                    <Input
                      id="predefinedKey-defaultValue"
                      value={field.value ?? ""}
                      onChange={field.onChange}
                      readOnly={readOnly}
                    />
                  </div>
                </Field>
              ) : (
                <SelectField
                  className="column"
                  label={t("scm-custom-properties-plugin.config.general.defaultValue.label")}
                  id="predefinedKey-defaultValue"
                  value={field.value ?? ""}
                  onChange={field.onChange}
                  disabled={readOnly}
                >
                  {allowedValues.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </SelectField>
              )
            }
          />
        </Row>
      ) : null}
    </>
  );
};

export default PredefinedKeyFields;
