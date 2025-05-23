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

import React, { FC, useEffect, useState } from "react";
import { Repository } from "@scm-manager/ui-types";
import { Form, Subtitle } from "@scm-manager/ui-core";
import { CustomProperty } from "./types";
import { useTranslation } from "react-i18next";
import { useHistory, useLocation } from "react-router";
import { urls } from "@scm-manager/ui-api";
import { useCreateCustomProperty, useEditCustomProperty } from "./hooks";

type Props = {
  repository: Repository;
  url: string;
};

const keyRegex = /^[a-zA-Z_ 0-9.]*$/;

const CustomPropertiesEditor: FC<Props> = ({ repository, url }) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();
  const [initialState, setInitialState] = useState<CustomProperty>({ key: "", value: "" });

  const { createCustomProperty } = useCreateCustomProperty(repository);
  const { editCustomProperty } = useEditCustomProperty(repository);

  const location = useLocation();
  const queryKey = urls.getValueStringFromLocationByKey(location, "key");

  useEffect(() => setInitialState({ key: queryKey ?? "", value: findValue() }), [queryKey]);

  const isEditMode = () => {
    return queryKey !== undefined && queryKey.length > 0;
  };

  const validateKey = (key: string) => {
    if (!isEditMode() && getCustomPropertyByKey(key)) {
      return t("scm-custom-properties-plugin.editor.key.alreadyExists", { key: key });
    }

    if (key.length > 255) {
      return t("scm-custom-properties-plugin.editor.key.tooLong");
    }

    if (!keyRegex.test(key)) {
      return t("scm-custom-properties-plugin.editor.key.invalidChars");
    }
  };

  const getCustomPropertyByKey = (key: string) => {
    return (repository._embedded?.customProperties as { properties: CustomProperty[] }).properties.find(
      (customProperty) => customProperty.key === key,
    );
  };

  const findValue = () => {
    if (!isEditMode()) {
      return "";
    }

    //At this point the key should always be defined.
    //The nullish coalescing operator is only there to please the compiler.
    const customProperty = getCustomPropertyByKey(queryKey ?? "");

    if (!customProperty) {
      //The translation hook, does not seem to work during initial rendering
      throw new Error(`The provided key '${queryKey}' does not exists for this repository`);
    }

    return customProperty.value;
  };

  const onSubmit = async (customProperty: CustomProperty) => {
    if (isEditMode()) {
      await editCustomProperty({
        newKey: customProperty.key,
        newValue: customProperty.value,
        //At this point the key should always be defined.
        //The nullish coalescing operator is only there to please the compiler.
        oldKey: queryKey ?? "",
        oldValue: findValue(),
      });
    } else {
      await createCustomProperty(customProperty);
    }

    history.push(`${url}/custom-properties`);
  };

  return (
    <Form<CustomProperty>
      translationPath={["plugins", "scm-custom-properties-plugin.editor"]}
      onSubmit={onSubmit}
      defaultValues={initialState}
    >
      <Subtitle>
        {isEditMode()
          ? t("scm-custom-properties-plugin.editor.subtitle_edit", { key: queryKey })
          : t("scm-custom-properties-plugin.editor.subtitle_create")}
      </Subtitle>
      <Form.Row>
        <Form.Input name="key" rules={{ required: true, validate: validateKey }} />
      </Form.Row>
      <Form.Row>
        <Form.Input name="value" rules={{ required: true }} />
      </Form.Row>
    </Form>
  );
};

export default CustomPropertiesEditor;
