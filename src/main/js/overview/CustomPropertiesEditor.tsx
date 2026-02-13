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

import React, { FC, useCallback, useEffect, useState } from "react";
import { Option, Repository } from "@scm-manager/ui-types";
import {
  Combobox,
  Subtitle,
  Title,
  useDocumentTitleForRepository,
  Input,
  Label,
  Checkbox,
  SelectField,
  Level,
  Button,
  ErrorNotification,
} from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";
import { useCreateCustomProperty, useEditCustomProperty, useQueryPredefinedKeys } from "../hooks";
import { Row, Field, ButtonsContainer } from "../component/FormUtils";
import { CustomProperty } from "../types";
import { useHistory, useLocation } from "react-router";
import { MULTIPLE_CHOICE_SEPARATOR } from "../utils";
import { validateKey } from "../validation";
import { urls } from "@scm-manager/ui-api";

/*
 * TODOS:
 * - isDirty
 */

type Props = {
  repository: Repository;
};

type CheckedValue = {
  value: string;
  isChecked: boolean;
};

const FORM_IDS = {
  form: "form-custom-property",
  keyInput: "input-Key",
  valueInput: "input-value",
};

const CustomPropertiesEditor: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();
  useDocumentTitleForRepository(
    repository,
    t("scm-custom-properties-plugin.repository.subtitle"),
    t("scm-custom-properties-plugin.config.edit"),
  );

  const location = useLocation();
  const queryExistingPropertyKey = urls.getValueStringFromLocationByKey(location, "key");
  const queryDefaultProperty = urls.getValueStringFromLocationByKey(location, "defaultProperty");
  const queryMissingProperty = urls.getValueStringFromLocationByKey(location, "missingProperty");

  const {
    createCustomProperty,
    isLoading: isCreationLoading,
    error: creationError,
  } = useCreateCustomProperty(repository);
  const { editCustomProperty, isLoading: isEditLoading, error: editingError } = useEditCustomProperty(repository);
  const isSubmitting = isCreationLoading || isEditLoading;

  const [initialState, setInitialState] = useState<CustomProperty & { checkedValues: CheckedValue[] }>({
    key: "",
    value: "",
    _links: {},
    checkedValues: [],
  });

  const [key, setKey] = useState("");
  const [keyError, setKeyError] = useState<string | undefined>("");
  const [keyFilter, setKeyFilter] = useState("");
  const { data } = useQueryPredefinedKeys(repository, keyFilter);

  const [value, setValue] = useState("");
  const [checkedValues, setCheckedValues] = useState<CheckedValue[]>([]);

  const isEditMode = () => queryExistingPropertyKey !== undefined && queryExistingPropertyKey !== "";
  const isMandatoryPropertyMode = () =>
    (queryMissingProperty !== undefined && queryMissingProperty !== "") ||
    (isEditMode() && queryExistingPropertyKey === key && data?.[key]?.mode === "MANDATORY");

  const getCustomPropertyByKey = useCallback(
    (key: string) => {
      return (repository._embedded?.customProperties as { properties: CustomProperty[] }).properties.find(
        (customProperty) => customProperty.key === key,
      );
    },
    [repository._embedded?.customProperties],
  );

  useEffect(() => {
    if (!queryDefaultProperty) {
      return;
    }

    const customProperty = getCustomPropertyByKey(queryDefaultProperty);
    if (customProperty) {
      setKey(customProperty.key);
      setValue(customProperty.value);
    }
  }, [getCustomPropertyByKey, queryDefaultProperty]);

  useEffect(() => {
    if (!queryMissingProperty) {
      return;
    }

    setInitialState((initialState) => {
      return { ...initialState, key: queryMissingProperty };
    });
    setKey(queryMissingProperty);
  }, [queryMissingProperty]);

  useEffect(() => {
    if (!queryExistingPropertyKey || initialState.key !== "") {
      return;
    }

    const customProperty = getCustomPropertyByKey(queryExistingPropertyKey);
    if (customProperty && data) {
      const checkedValues: CheckedValue[] =
        data?.[customProperty.key]?.mode === "MULTIPLE_CHOICE"
          ? customProperty.value.split(MULTIPLE_CHOICE_SEPARATOR).map((value) => {
              return { value, isChecked: true };
            })
          : [];

      setInitialState({
        key: customProperty.key,
        value: customProperty.value,
        _links: customProperty._links,
        checkedValues,
      });
      setKey(customProperty.key);
      setValue(customProperty.value);
      setCheckedValues(checkedValues);
    }
  }, [data, getCustomPropertyByKey, initialState, initialState.key, queryExistingPropertyKey]);

  const onKeyChange = (option?: Option<string>) => {
    if (!option) {
      return;
    }

    setKey(option.value);

    const validationError = validateKey(option.value, queryExistingPropertyKey, (key) => {
      const customProperty = getCustomPropertyByKey(key);
      return customProperty !== undefined && !customProperty.defaultProperty;
    });
    setKeyError(validationError);
  };

  const onValueChange = (event: React.ChangeEvent<HTMLInputElement> | React.ChangeEvent<HTMLSelectElement>) => {
    setValue(event.target.value);
  };

  const onValueChecked = (newCheckedValue: CheckedValue) => {
    const unchangedCheckedValues = checkedValues.filter((checkedValue) => checkedValue.value !== newCheckedValue.value);
    unchangedCheckedValues.push(newCheckedValue);
    setCheckedValues(unchangedCheckedValues);
  };

  const isValueChecked = (value: string) => {
    const checkedValue = checkedValues.find((checkedValue) => checkedValue.value === value);
    return checkedValue ? checkedValue.isChecked : false;
  };

  const transformPredefinedKeysToOptions = useCallback(() => {
    const result = Object.keys(data || {}).map((key) => {
      return { label: key, value: key };
    });

    if (keyFilter !== "" && !result.some((option) => option.value === keyFilter)) {
      result.unshift({ label: keyFilter, value: keyFilter });
    }

    return result;
  }, [data, keyFilter]);

  const transformInputsToCustomProperty = (): CustomProperty => {
    const customProperty = { key, value, _links: initialState._links };

    if (data?.[key]?.mode === "MULTIPLE_CHOICE") {
      customProperty.value = checkedValues
        .filter((checkedValue) => checkedValue.isChecked)
        .map((checkedValue) => checkedValue.value)
        .join(MULTIPLE_CHOICE_SEPARATOR);
    }

    return customProperty;
  };

  const redirectBackToOverview = () => {
    history.push(`/repo/${repository.namespace}/${repository.name}/custom-properties`);
  };

  const isValueDirty = () => {
    if (data?.[key]?.mode === "MULTIPLE_CHOICE") {
      //It is dirty, if at least one currentlyCheckedValue can be found, that does not exist in the initial state, but is currently checked.
      //Or if it exists in the initial state, but the isChecked value differs from the initial state
      return checkedValues.some((currentlyChecked) => {
        const initiallyChecked = initialState.checkedValues.find(
          (initiallyChecked) => currentlyChecked.value === initiallyChecked.value,
        );

        return (
          (!initiallyChecked && currentlyChecked.isChecked) ||
          (initiallyChecked && currentlyChecked.isChecked !== initiallyChecked.isChecked)
        );
      });
    }

    return initialState.value !== value;
  };

  const isDirty = () => initialState.key !== key || isValueDirty();

  const isValueValid = () => {
    if (data?.[key]?.mode === "MULTIPLE_CHOICE") {
      return checkedValues.some((checkedValue) => checkedValue.isChecked);
    }

    return value !== "";
  };

  const isValid = () => !keyError && key !== "" && isValueValid();

  const onSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const customProperty = transformInputsToCustomProperty();

    if (isEditMode()) {
      await editCustomProperty(customProperty);
    } else {
      await createCustomProperty(customProperty);
    }

    redirectBackToOverview();
  };

  return (
    <form id={FORM_IDS.form} onSubmit={onSubmit} noValidate>
      <Title>{t("scm-custom-properties-plugin.config.general.predefinedKeys.title")}</Title>
      <Subtitle>{t("scm-custom-properties-plugin.config.general.predefinedKeys.subtitle")}</Subtitle>
      {creationError ? <ErrorNotification error={creationError} /> : null}
      {editingError ? <ErrorNotification error={editingError} /> : null}
      <Row>
        <Field>
          <Label htmlFor={FORM_IDS.keyInput}>{t("scm-custom-properties-plugin.editor.key.label")}</Label>
          <Combobox
            className="column"
            id={FORM_IDS.keyInput}
            onQueryChange={setKeyFilter}
            options={transformPredefinedKeysToOptions}
            value={{ label: key, value: key }}
            onChange={onKeyChange}
            disabled={isSubmitting}
            readOnly={isMandatoryPropertyMode()}
          />
          {keyError ? (
            <p className="help is-danger">{t(`scm-custom-properties-plugin.editor.key.${keyError}`)}</p>
          ) : null}
        </Field>
      </Row>
      {data?.[key] && data?.[key]?.allowedValues.length > 0 && data?.[key]?.mode === "MULTIPLE_CHOICE" ? (
        <fieldset>
          <Label as="legend">{t("scm-custom-properties-plugin.editor.value.multipleChoiceLabel")}</Label>
          <p>{t("scm-custom-properties-plugin.editor.value.oneChoiceRequired")}</p>
          {data[key].allowedValues.map((value) => (
            <Row key={`checkbox-${value}`}>
              <div className="column">
                <Checkbox
                  label={value}
                  checked={isValueChecked(value)}
                  onChange={(e) => onValueChecked({ value, isChecked: e.target.checked })}
                  disabled={isSubmitting}
                />
              </div>
            </Row>
          ))}
        </fieldset>
      ) : null}
      {data?.[key] && data?.[key]?.allowedValues.length > 0 && data?.[key]?.mode !== "MULTIPLE_CHOICE" ? (
        <Row>
          <SelectField
            className="column"
            label={t("scm-custom-properties-plugin.editor.value.label")}
            id={FORM_IDS.valueInput}
            form={FORM_IDS.form}
            value={value}
            onChange={onValueChange}
            disabled={isSubmitting}
          >
            <option value=""></option>
            {data[key].allowedValues.map((value) => (
              <option key={`option-${value}`} value={value}>
                {value}
              </option>
            ))}
          </SelectField>
        </Row>
      ) : null}
      {!data?.[key] || data?.[key]?.allowedValues.length === 0 ? (
        <Row>
          <Field>
            <Label htmlFor={FORM_IDS.valueInput}>{t("scm-custom-properties-plugin.editor.value.label")}</Label>
            <div className="control">
              <Input
                id={FORM_IDS.valueInput}
                form={FORM_IDS.form}
                value={value}
                onChange={onValueChange}
                disabled={isSubmitting}
              />
            </div>
          </Field>
        </Row>
      ) : null}
      <Level
        right={
          <ButtonsContainer>
            <Button onClick={redirectBackToOverview} disabled={isSubmitting}>
              {t("scm-custom-properties-plugin.config.general.cancel")}
            </Button>
            <Button
              variant="primary"
              type="submit"
              form={FORM_IDS.form}
              disabled={isCreationLoading || isEditLoading || !isValid() || !isDirty()}
              isLoading={isCreationLoading || isEditLoading}
            >
              {t("scm-custom-properties-plugin.config.general.submit")}
            </Button>
          </ButtonsContainer>
        }
      />
    </form>
  );
};

export default CustomPropertiesEditor;
