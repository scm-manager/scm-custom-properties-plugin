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
import { Repository } from "@scm-manager/ui-types";
import { Form, Subtitle } from "@scm-manager/ui-core";
import { CustomProperty } from "../types";
import { useTranslation } from "react-i18next";
import { useHistory, useLocation } from "react-router";
import { urls } from "@scm-manager/ui-api";
import { useCreateCustomProperty, useEditCustomProperty, useQueryPredefinedKeys } from "../hooks";
import { validateKey } from "../validation";
import FieldErrorMessage from "../component/FieldErrorMessage";
import styled from "styled-components";

const StyledCombobox = styled(Form.Combobox)<React.ComponentProps<typeof Form.Combobox> & { isError: boolean }>`
  & * input {
    ${({ isError }) => isError && "border-color: var(--scm-danger-color);"}
  }
  & * input:focus {
    ${({ isError }) => isError && "box-shadow: 0 0 0 .125em rgba(255,56,96,.25)"}
  }
  & * input:hover {
    ${({ isError }) => isError && "border-color: var(--scm-danger-color);"}
  }
  & * ul {
    overflow-y: scroll;
    max-height: 25rem;
  }
`;

type Props = {
  repository: Repository;
};

const CustomPropertiesEditor: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();
  const [initialState, setInitialState] = useState<CustomProperty>({ key: "", value: "", _links: {} });

  const location = useLocation();
  const queryKey = urls.getValueStringFromLocationByKey(location, "key");

  const { createCustomProperty } = useCreateCustomProperty(repository);
  const { editCustomProperty } = useEditCustomProperty(repository);
  const [filter, setFilter] = useState("");
  const { data } = useQueryPredefinedKeys(repository, filter);

  const isEditMode = () => {
    return queryKey !== undefined && queryKey.length > 0;
  };

  const keyValidator = (key: string) => {
    const result = validateKey(key, queryKey, (key) => !!getCustomPropertyByKey(key));

    if (result) {
      return t(`scm-custom-properties-plugin.editor.key.${result}`);
    }
  };

  const getCustomPropertyByKey = useCallback(
    (key: string) => {
      return (repository._embedded?.customProperties as { properties: CustomProperty[] }).properties.find(
        (customProperty) => customProperty.key === key,
      );
    },
    [repository._embedded?.customProperties],
  );

  const onSubmit = async (customProperty: CustomProperty) => {
    if (isEditMode()) {
      await editCustomProperty(customProperty);
    } else {
      await createCustomProperty(customProperty);
    }

    history.push(`/repo/${repository.namespace}/${repository.name}/custom-properties`);
  };

  useEffect(() => {
    const customProperty = getCustomPropertyByKey(queryKey ?? "");

    if (customProperty) {
      setInitialState({ key: queryKey ?? "", value: customProperty.value, _links: customProperty._links });
    }
  }, [getCustomPropertyByKey, queryKey]);

  return (
    <Form<CustomProperty>
      translationPath={["plugins", "scm-custom-properties-plugin.editor"]}
      onSubmit={onSubmit}
      defaultValues={initialState}
    >
      {({ getFieldState }) => (
        <>
          <Subtitle>
            {isEditMode()
              ? t("scm-custom-properties-plugin.editor.subtitle_edit", { key: queryKey })
              : t("scm-custom-properties-plugin.editor.subtitle_create")}
          </Subtitle>
          <Form.Row>
            <StyledCombobox
              name="key"
              options={data}
              onQueryChange={setFilter}
              rules={{ required: true, validate: keyValidator }}
              className={"is-info"}
              isError={!!getFieldState("key").error}
            />
          </Form.Row>
          <Form.Row>
            {getFieldState("key").error ? (
              <FieldErrorMessage>{getFieldState("key").error?.message}</FieldErrorMessage>
            ) : null}
          </Form.Row>
          <Form.Row>
            <Form.Input name="value" rules={{ required: true }} />
          </Form.Row>
        </>
      )}
    </Form>
  );
};

export default CustomPropertiesEditor;
