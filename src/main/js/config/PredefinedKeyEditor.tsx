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
import { Subtitle, Title, Input, Label, ChipInputField, Level, Button, SelectField } from "@scm-manager/ui-core";
import { Option } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { PredefinedKeys, SinglePredefinedKey } from "../types";
import { useHistory } from "react-router";
import { validateKey } from "../validation";

const FORM_IDS = {
  form: "form-predefinedKey",
  keyInput: "input-Key",
  allowedValuesInput: "input-allowedValues",
  defaultValueInput: "input-defaultValue",
};

type WithChildren = { children: React.ReactNode };

type PredefinedKeyEntry = { key: string } & SinglePredefinedKey;

type EditorProps = {
  initial: PredefinedKeyEntry;
  existingPredefinedKeys: PredefinedKeys;
  redirectUrl: string;
  submit: (entry: PredefinedKeyEntry) => Promise<Response | undefined>;
  isSubmitting: boolean;
};

const ButtonsContainer = styled.div`
  display: flex;
  gap: 0.75rem;
`;

const Row = ({ children }: WithChildren) => {
  return <div className="columns">{children}</div>;
};

const Field = ({ children }: WithChildren) => {
  return <div className="field column">{children}</div>;
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
  const [defaultValue, setDefaultValue] = useState(initialState.defaultValue);

  const onKeyChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newKey = event.target.value;
    setKey(newKey);

    const validationResult = validateKey(newKey, initialState.key, (key) =>
      Object.keys(existingPredefinedKeys).includes(key),
    );
    setKeyError(validationResult ? t(`scm-custom-properties-plugin.editor.key.${validationResult}`) : "");
  };

  const onAllowedValueChange = (newAllowedValues: Option<string>[]) => {
    if (newAllowedValues.length !== 0 && !newAllowedValues.some((option) => option.value === defaultValue)) {
      setDefaultValue("");
    }

    setAllowedValues(newAllowedValues);
  };

  const onDefaultValueChange = (event: React.ChangeEvent<HTMLInputElement> | React.ChangeEvent<HTMLSelectElement>) => {
    setDefaultValue(event.target.value);
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
    key !== initialState.key || defaultValue !== initialState.defaultValue || isAllowedValuesDirty();

  const isValid = () => keyError === "" && key !== "";

  const redirectBackToConfig = () => {
    history.push(redirectUrl);
  };

  const onSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await submit({ key, allowedValues: allowedValues.map((option) => option.value), defaultValue });
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
        <ChipInputField<string>
          className="column"
          id={FORM_IDS.allowedValuesInput}
          label={t("scm-custom-properties-plugin.config.general.allowedValues.label")}
          information={t("scm-custom-properties-plugin.config.general.allowedValues.note")}
          onChange={onAllowedValueChange}
          value={allowedValues}
          readOnly={isSubmitting}
        />
      </Row>
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
            <option value=""></option>
            {allowedValues.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </SelectField>
        )}
      </Row>
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
