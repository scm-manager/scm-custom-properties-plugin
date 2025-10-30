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
import { Redirect, useHistory, useParams } from "react-router";
import { BaseConfig } from "../types";
import { useTranslation } from "react-i18next";
import { Form, Subtitle, Title } from "@scm-manager/ui-core";
import { validateKey } from "../validation";

type ConfigEditorProps<T extends BaseConfig> = {
  config: T;
  update: (config: T) => Promise<Response> | undefined;
  redirectUrl: string;
};

type EditRadioProps<T extends BaseConfig> = {
  name: string;
} & ConfigEditorProps<T>;

type EditKeyProps<T extends BaseConfig> = {
  originalKey: string;
} & ConfigEditorProps<T>;

type EditKeyConfig<T extends BaseConfig> = T & { editedKey: string };

type AddKeyConfig<T extends BaseConfig> = T & { addedKey: string };

const AddKey = <T extends BaseConfig>({ config, update, redirectUrl }: ConfigEditorProps<T>) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();

  const onSubmit = async (updatedConfig: AddKeyConfig<T>) => {
    await update({ ...updatedConfig, predefinedKeys: [...updatedConfig.predefinedKeys, updatedConfig.addedKey] });
    history.push(redirectUrl);
  };

  const keyValidator = (key: string) => {
    const result = validateKey(key, undefined, (key) => config.predefinedKeys.includes(key));

    if (result) {
      return t(`scm-custom-properties-plugin.editor.key.${result}`);
    }
  };

  return (
    <Form<AddKeyConfig<T>>
      translationPath={["plugins", "scm-custom-properties-plugin.config.general"]}
      onSubmit={onSubmit}
      defaultValues={{ addedKey: "", ...config }}
    >
      <Title>{t("scm-custom-properties-plugin.config.general.predefinedKeys.title")}</Title>
      <Subtitle>{t("scm-custom-properties-plugin.config.general.predefinedKeys.subtitle")}</Subtitle>
      <Form.Row>
        <Form.Input name="addedKey" rules={{ required: true, validate: keyValidator }} autoFocus />
      </Form.Row>
    </Form>
  );
};

const EditKey = <T extends BaseConfig>({ config, update, originalKey, redirectUrl }: EditKeyProps<T>) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();

  const onSubmit = async (updatedConfig: EditKeyConfig<T>) => {
    const predefinedKeys = updatedConfig.predefinedKeys.filter((key) => key !== originalKey);
    predefinedKeys.push(updatedConfig.editedKey);

    await update({ ...updatedConfig, predefinedKeys });

    history.push(redirectUrl);
  };

  const keyValidator = (key: string) => {
    const result = validateKey(key, originalKey, (key) => config.predefinedKeys.includes(key));

    if (result) {
      return t(`scm-custom-properties-plugin.editor.key.${result}`);
    }
  };

  return (
    <Form<EditKeyConfig<T>>
      translationPath={["plugins", "scm-custom-properties-plugin.config.general"]}
      onSubmit={onSubmit}
      defaultValues={{ editedKey: originalKey, ...config }}
    >
      <Title>{t("scm-custom-properties-plugin.config.general.predefinedKeys.title")}</Title>
      <Subtitle>{t("scm-custom-properties-plugin.config.general.predefinedKeys.subtitle")}</Subtitle>
      <Form.Row>
        <Form.Input name="editedKey" rules={{ required: true, validate: keyValidator }} autoFocus />
      </Form.Row>
    </Form>
  );
};

const EditRadio = <T extends BaseConfig>({ config, update, name, redirectUrl }: EditRadioProps<T>) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();

  const onSubmit = async (config: T) => {
    await update(config);
    history.push(redirectUrl);
  };

  return (
    <Form<T>
      translationPath={["plugins", "scm-custom-properties-plugin.config.general"]}
      onSubmit={onSubmit}
      defaultValues={config}
    >
      {({ watch }) => (
        <>
          <Title>{t(`scm-custom-properties-plugin.config.general.${name}.title`)}</Title>
          <Subtitle>{t(`scm-custom-properties-plugin.config.general.${name}.subtitle`)}</Subtitle>
          <Form.Row>
            <Form.RadioGroup name={name}>
              {/* @ts-expect-error RadioGroup usually expects string values, but in this case it can also be used as boolean */}
              <Form.RadioGroup.Option value={true} autoFocus={watch(name)} />
              {/* @ts-expect-error RadioGroup usually expects string values, but in this case it can also be used as boolean */}
              <Form.RadioGroup.Option value={false} autoFocus={!watch(name)} />
            </Form.RadioGroup>
          </Form.Row>
        </>
      )}
    </Form>
  );
};

const ConfigEditor = <T extends BaseConfig>({ config, update, redirectUrl }: ConfigEditorProps<T>) => {
  const params = useParams<{ field: string; selector?: string }>();

  if ("enabled" in config && params.field === "enabled") {
    return <EditRadio name={"enabled"} config={config} update={update} redirectUrl={redirectUrl} />;
  }

  if ("enableNamespaceConfig" in config && params.field === "enableNamespaceConfig") {
    return <EditRadio name={"enableNamespaceConfig"} config={config} update={update} redirectUrl={redirectUrl} />;
  }

  const originalKey = decodeURIComponent(params.selector ?? "");
  if (params.field === "predefinedKeys" && originalKey && config.predefinedKeys.includes(originalKey)) {
    return <EditKey originalKey={originalKey} config={config} update={update} redirectUrl={redirectUrl} />;
  }

  if (params.field === "predefinedKeys" && !originalKey) {
    return <AddKey config={config} update={update} redirectUrl={redirectUrl} />;
  }

  // @ts-expect-error Will be irrelevant after react 19 upgrade
  return <Redirect to={redirectUrl} />;
};

export default ConfigEditor;
