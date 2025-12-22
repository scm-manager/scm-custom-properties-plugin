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
import PredefinedKeyEditor from "./PredefinedKeyEditor";

type ConfigEditorProps<T extends BaseConfig> = {
  config: T;
  update: (config: T) => Promise<Response> | undefined;
  isUpdating: boolean;
  redirectUrl: string;
};

type EditRadioProps<T extends BaseConfig> = {
  name: string;
} & Omit<ConfigEditorProps<T>, "isUpdating">;

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

const ConfigEditor = <T extends BaseConfig>({ config, update, redirectUrl, isUpdating }: ConfigEditorProps<T>) => {
  const params = useParams<{ field: string; selector?: string }>();

  if ("enabled" in config && params.field === "enabled") {
    return <EditRadio name={"enabled"} config={config} update={update} redirectUrl={redirectUrl} />;
  }

  if ("enableNamespaceConfig" in config && params.field === "enableNamespaceConfig") {
    return <EditRadio name={"enableNamespaceConfig"} config={config} update={update} redirectUrl={redirectUrl} />;
  }

  const originalKey = decodeURIComponent(params.selector ?? "");
  if (params.field === "predefinedKeys" && originalKey && Object.keys(config.predefinedKeys).includes(originalKey)) {
    const predefinedKey = config.predefinedKeys[originalKey];
    return (
      <PredefinedKeyEditor
        initial={{
          key: originalKey,
          allowedValues: predefinedKey.allowedValues,
          defaultValue: predefinedKey.defaultValue,
          mode: predefinedKey.mode,
        }}
        existingPredefinedKeys={config.predefinedKeys}
        redirectUrl={redirectUrl}
        submit={async (updatedPredefinedKey) => {
          const predefinedKeys = { ...config.predefinedKeys };
          delete predefinedKeys[originalKey];
          predefinedKeys[updatedPredefinedKey.key] = {
            allowedValues: updatedPredefinedKey.allowedValues,
            defaultValue: updatedPredefinedKey.defaultValue,
            mode: updatedPredefinedKey.mode,
          };

          return update({ ...config, predefinedKeys });
        }}
        isSubmitting={isUpdating}
      />
    );
  }

  if (params.field === "predefinedKeys" && !originalKey) {
    return (
      <PredefinedKeyEditor
        initial={{ key: "", allowedValues: [], defaultValue: "", mode: "NONE" }}
        existingPredefinedKeys={config.predefinedKeys}
        redirectUrl={redirectUrl}
        submit={async (addedPredefinedKey) => {
          const predefinedKeys = { ...config.predefinedKeys };
          predefinedKeys[addedPredefinedKey.key] = {
            allowedValues: addedPredefinedKey.allowedValues,
            defaultValue: addedPredefinedKey.defaultValue,
            mode: addedPredefinedKey.mode,
          };

          return update({ ...config, predefinedKeys });
        }}
        isSubmitting={isUpdating}
      />
    );
  }

  // @ts-expect-error Will be irrelevant after react 19 upgrade
  return <Redirect to={redirectUrl} />;
};

export default ConfigEditor;
