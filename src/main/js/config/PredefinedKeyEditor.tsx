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

import React, { useState } from "react";
import {
  Subtitle,
  Title,
  Input,
  Label,
  ChipInputField,
  Level,
  Button,
  SelectField,
  RadioGroup,
} from "@scm-manager/ui-core";
import { Option } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import { PredefinedKeys, SinglePredefinedKey, ValueMode, valueModes } from "../types";
import { useHistory } from "react-router";
import { validateKey } from "../validation";
import { ButtonsContainer, Field, Row } from "../component/FormUtils";

const FORM_IDS = {
  form: "form-predefinedKey",
  keyInput: "input-Key",
  allowedValuesInput: "input-allowedValues",
  mandatoryOrDefaultInput: "input-mandatoryOrDefault",
  defaultValueInput: "input-defaultValue",
};

type PredefinedKeyEntry = { key: string } & SinglePredefinedKey;

type EditorProps = {
  initial: PredefinedKeyEntry;
  existingPredefinedKeys: PredefinedKeys;
  redirectUrl: string;
  submit: (entry: PredefinedKeyEntry) => Promise<Response | undefined>;
  isSubmitting: boolean;
};

const PredefinedKeyEditor = ({ initial, existingPredefinedKeys, redirectUrl, submit, isSubmitting }: EditorProps) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();
  const [initialState] = useState(initial);

  const [key, setKey] = useState(initialState.key);
  const [keyError, setKeyError] = useState("");

  const [allowedValues, setAllowedValues] = useState<Option<string>[]>(
    initialState.allowedValues.map((value) => {
      return { label: value, value };
    }),
  );
  const [allowedValuesRequiredError, setAllowedValuesRequiredError] = useState("");

  const [valueMode, setValueMode] = useState<ValueMode>(initialState.mode);

  const [defaultValue, setDefaultValue] = useState(initialState.defaultValue);

  const onKeyChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newKey = event.target.value;
    setKey(newKey);

    const validationResult = validateKey(newKey, initialState.key, (key) =>
      Object.keys(existingPredefinedKeys).includes(key),
    );
    setKeyError(validationResult ? t(`scm-custom-properties-plugin.editor.key.${validationResult}`) : "");
  };

  const checkIsAllowedValuesRequired = (valueMode: ValueMode, allowedValues: Option<string>[]) => {
    if (valueMode === "MULTIPLE_CHOICE" && allowedValues.length === 0) {
      setAllowedValuesRequiredError(t("scm-custom-properties-plugin.config.general.allowedValues.isRequiredError"));
    } else {
      setAllowedValuesRequiredError("");
    }
  };

  const onAllowedValueChange = (newAllowedValues: Option<string>[]) => {
    if (newAllowedValues.length !== 0 && !newAllowedValues.some((option) => option.value === defaultValue)) {
      setDefaultValue(newAllowedValues[0].value);
    }

    checkIsAllowedValuesRequired(valueMode, newAllowedValues);

    setAllowedValues(newAllowedValues);
  };

  const onDefaultValueChange = (event: React.ChangeEvent<HTMLInputElement> | React.ChangeEvent<HTMLSelectElement>) => {
    setDefaultValue(event.target.value);
  };

  const onValueModeChange = (newValue: ValueMode) => {
    setValueMode(newValue);

    checkIsAllowedValuesRequired(newValue, allowedValues);

    if (
      newValue === "DEFAULT" &&
      allowedValues.length !== 0 &&
      !allowedValues.some((option) => option.value === defaultValue)
    ) {
      setDefaultValue(allowedValues[0].value);
    }
  };

  const isAllowedValuesDirty = () => {
    if (allowedValues.length !== initialState.allowedValues.length) {
      return true;
    }

    for (let i = 0; i < allowedValues.length; i++) {
      if (allowedValues[i].value !== initialState.allowedValues[i]) {
        return true;
      }
    }

    return false;
  };

  const isDirty = () =>
    key !== initialState.key ||
    defaultValue !== initialState.defaultValue ||
    valueMode !== initialState.mode ||
    isAllowedValuesDirty();

  const isValid = () =>
    keyError === "" &&
    key !== "" &&
    (valueMode !== "DEFAULT" || defaultValue !== "") &&
    (valueMode !== "MULTIPLE_CHOICE" || allowedValues.length > 0);

  const redirectBackToConfig = () => {
    history.push(redirectUrl);
  };

  const onSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await submit({
      key,
      allowedValues: allowedValues.map((option) => option.value),
      defaultValue,
      mode: valueMode,
    });
    redirectBackToConfig();
  };

  return (
    <form onSubmit={onSubmit} id={FORM_IDS.form} noValidate>
      <Title>{t("scm-custom-properties-plugin.config.general.predefinedKeys.title")}</Title>
      <Subtitle>{t("scm-custom-properties-plugin.config.general.predefinedKeys.subtitle")}</Subtitle>
      <Row>
        <Field>
          <Label htmlFor={FORM_IDS.keyInput}>{t("scm-custom-properties-plugin.config.general.addedKey.label")}</Label>
          <div className="control">
            <Input
              id={FORM_IDS.keyInput}
              form={FORM_IDS.form}
              value={key}
              onChange={onKeyChange}
              autoFocus
              readOnly={isSubmitting}
            />
          </div>
          {keyError && <p className="help is-danger">{keyError}</p>}
        </Field>
      </Row>
      <Row>
        <fieldset className="field column">
          <Label as="legend" htmlFor={FORM_IDS.mandatoryOrDefaultInput}>
            {t("scm-custom-properties-plugin.config.general.mandatoryOrDefault.label")}
          </Label>
          <div>
            <RadioGroup
              className="column"
              id={FORM_IDS.mandatoryOrDefaultInput}
              onValueChange={onValueModeChange}
              value={valueMode}
            >
              {valueModes.map((value: ValueMode) => (
                <RadioGroup.Option
                  id={`${FORM_IDS.mandatoryOrDefaultInput}-${value}`}
                  form={FORM_IDS.form}
                  key={value}
                  value={value}
                  label={t(`scm-custom-properties-plugin.config.general.mandatoryOrDefault.${value}`)}
                  disabled={isSubmitting}
                />
              ))}
            </RadioGroup>
          </div>
        </fieldset>
      </Row>
      <Row>
        <ChipInputField<string>
          className="column"
          id={FORM_IDS.allowedValuesInput}
          label={t(
            valueMode === "MULTIPLE_CHOICE"
              ? "scm-custom-properties-plugin.config.general.allowedValues.labelRequired"
              : "scm-custom-properties-plugin.config.general.allowedValues.label",
          )}
          information={t("scm-custom-properties-plugin.config.general.allowedValues.note")}
          onChange={onAllowedValueChange}
          value={allowedValues}
          readOnly={isSubmitting}
          error={allowedValuesRequiredError}
        />
      </Row>
      {valueMode === "DEFAULT" ? (
        <Row>
          {allowedValues.length === 0 ? (
            <Field>
              <Label htmlFor={FORM_IDS.defaultValueInput}>
                {t("scm-custom-properties-plugin.config.general.defaultValue.label")}
              </Label>
              <div className="control">
                <Input
                  id={FORM_IDS.defaultValueInput}
                  form={FORM_IDS.form}
                  onChange={onDefaultValueChange}
                  value={defaultValue}
                  readOnly={isSubmitting}
                />
              </div>
            </Field>
          ) : (
            <SelectField
              className="column"
              label={t("scm-custom-properties-plugin.config.general.defaultValue.label")}
              id={FORM_IDS.defaultValueInput}
              form={FORM_IDS.form}
              value={defaultValue}
              onChange={onDefaultValueChange}
            >
              {allowedValues.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </SelectField>
          )}
        </Row>
      ) : null}
      <Level
        right={
          <ButtonsContainer>
            <Button onClick={redirectBackToConfig}>{t("scm-custom-properties-plugin.config.general.cancel")}</Button>
            <Button
              variant="primary"
              type="submit"
              form={FORM_IDS.form}
              disabled={!isDirty() || !isValid()}
              isLoading={isSubmitting}
            >
              {t("scm-custom-properties-plugin.config.general.submit")}
            </Button>
          </ButtonsContainer>
        }
      />
    </form>
  );
};

export default PredefinedKeyEditor;
