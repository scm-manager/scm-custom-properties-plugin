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

import React, {FC, useEffect, useMemo, useRef, useState} from "react";
import {useFieldArray} from "react-hook-form";
import {useQueryClient} from "@tanstack/react-query";
import {useTranslation} from "react-i18next";
import {Link} from "react-router-dom";
import {Button, Form, Icon, SubpageForm, useScmFormContext, useSubpageContext} from "@scm-manager/ui-core";
import {SmallLoadingSpinner} from "@scm-manager/ui-components";
import {Links} from "@scm-manager/ui-types";
import {
  BaseConfig,
  NamespaceConfig,
  PredefinedKeyEntry,
  PredefinedKeys,
  SinglePredefinedKey,
  ValueMode,
} from "../types";
import PropertyTag from "../component/PropertyTag";
import DeleteAction from "../component/DeleteAction";
import PredefinedKeyFields from "./PredefinedKeyFields";

type Level = "global" | "namespace";

type ConfigFormShape = {
  enabled?: boolean;
  enableNamespaceConfig?: boolean;
  globallyPredefinedKeys?: PredefinedKeys;
  predefinedKeys: PredefinedKeyEntry[];
  _links: Links;
};

type ConfigUpdate<T extends BaseConfig> = (config: T) => Promise<Response> | undefined;

type ConfigFormProps<T extends BaseConfig> = {
  level: Level;
  config: T;
  update: ConfigUpdate<T>;
  isReadOnly: boolean;
  isUpdating: boolean;
  missingProperties: Record<string, string[]>;
  isMissingPropertiesLoading: boolean;
};

const keyOfSubpage = (index: number) => `key-${index}`;
const missingSubpage = (key: string) => `missing-${key}`;

const toEntries = (record: PredefinedKeys): PredefinedKeyEntry[] =>
  Object.entries(record)
    .sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
    .map(([key, definition]) => ({
      key,
      allowedValues: definition.allowedValues ?? [],
      defaultValue: definition.defaultValue,
      mode: definition.mode,
    }));

export const toRecord = (entries: PredefinedKeyEntry[]): PredefinedKeys =>
  entries.reduce<PredefinedKeys>((record, {key, allowedValues, defaultValue, mode}) => {
    if (key) {
      record[key] = {allowedValues: allowedValues ?? [], defaultValue, mode};
    }
    return record;
  }, {});

const ValueModeColumn: FC<Pick<SinglePredefinedKey, "mode" | "defaultValue">> = ({mode, defaultValue}) => {
  const [t] = useTranslation("plugins");

  if (mode === "MANDATORY") {
    return <>{t("scm-custom-properties-plugin.editor.mandatoryValueTag")}</>;
  }
  if (mode === "DEFAULT" && defaultValue) {
    return <>{`${t("scm-custom-properties-plugin.editor.defaultValueTag")}: ${defaultValue}`}</>;
  }
  if (mode === "MULTIPLE_CHOICE") {
    return <>{t("scm-custom-properties-plugin.editor.multipleValueTag")}</>;
  }
  return null;
};

const AllowedValuesColumn: FC<{ allowedValues: string[] }> = ({allowedValues}) => (
  <ul>
    {[...(allowedValues ?? [])]
      .sort((valueA, valueB) => valueA.localeCompare(valueB))
      .map((value) => (
        <li key={value}>{value}</li>
      ))}
  </ul>
);

type MandatoryValueSetProps = {
  propertyKey: string;
  mode: ValueMode;
  isLoading: boolean;
  violatingRepositories: string[] | undefined;
};

const MandatoryValueSet: FC<MandatoryValueSetProps> = ({propertyKey, mode, isLoading, violatingRepositories}) => {
  const [t] = useTranslation("plugins");
  const {openSubpage} = useSubpageContext();
  const {readOnly} = useScmFormContext();

  if (mode !== "MANDATORY" || isLoading) {
    return null;
  }

  if (!violatingRepositories) {
    return (
      <Icon
        className="is-medium"
        aria-label={t("scm-custom-properties-plugin.config.missingMandatoryProperties.ariaLabelNoMissingRepos", {
          key: propertyKey,
        })}
      >
        check-circle
      </Icon>
    );
  }

  const showMissingReposLabel = t(
    "scm-custom-properties-plugin.config.missingMandatoryProperties.ariaLabelShowMissingRepos",
    {key: propertyKey},
  );

  if (readOnly) {
    return (
      <Icon className="has-text-warning is-medium" aria-label={showMissingReposLabel}>
        exclamation
      </Icon>
    );
  }

  return (
    <Button variant="signal" onClick={() => openSubpage(missingSubpage(propertyKey))}
            aria-label={showMissingReposLabel}>
      <Icon>exclamation</Icon>
    </Button>
  );
};

const GeneralSettingsTable: FC = () => {
  const [t] = useTranslation("plugins");
  const {watch} = useScmFormContext();

  const settings: { name: "enabled" | "enableNamespaceConfig" }[] = [
    {name: "enabled"},
    {name: "enableNamespaceConfig"},
  ];

  return (
    <SubpageForm.SubpageTable>
      <thead>
      <tr>
        <th>{t("scm-custom-properties-plugin.table.header.setting")}</th>
        <th>{t("scm-custom-properties-plugin.table.header.action")}</th>
      </tr>
      </thead>
      <tbody>
      {settings.map(({name}) => (
        <tr key={name}>
          <td data-label={t("scm-custom-properties-plugin.table.header.setting")}>
            {t(`scm-custom-properties-plugin.config.general.${name}.radio.${!!watch(name)}`)}
          </td>
          <td data-label={t("scm-custom-properties-plugin.table.header.action")} className="has-text-right">
            <SubpageForm.Subpage.OpenIconButton
              subpage={name}
              aria-label={t(`scm-custom-properties-plugin.config.general.${name}.edit.ariaLabel`)}
            >
              <Icon>edit</Icon>
            </SubpageForm.Subpage.OpenIconButton>
          </td>
        </tr>
      ))}
      </tbody>
    </SubpageForm.SubpageTable>
  );
};

type PredefinedKeysTableProps<T extends BaseConfig> = {
  entries: (PredefinedKeyEntry & { id: string })[];
  globallyPredefinedKeys: PredefinedKeys;
  config: T;
  update: ConfigUpdate<T>;
  isUpdating: boolean;
  missingProperties: Record<string, string[]>;
  isMissingPropertiesLoading: boolean;
};

const PredefinedKeysTable = <T extends BaseConfig>({
                                                     entries,
                                                     globallyPredefinedKeys,
                                                     config,
                                                     update,
                                                     isUpdating,
                                                     missingProperties,
                                                     isMissingPropertiesLoading,
                                                   }: PredefinedKeysTableProps<T>) => {
  const [t] = useTranslation("plugins");
  const globallyPredefinedTag = t("scm-custom-properties-plugin.config.template.global");
  const header = {
    key: t("scm-custom-properties-plugin.table.header.key"),
    allowedValues: t("scm-custom-properties-plugin.table.header.allowedValues"),
    defaultValue: t("scm-custom-properties-plugin.table.header.defaultValue"),
    mandatoryValue: t("scm-custom-properties-plugin.table.header.mandatoryValue"),
    action: t("scm-custom-properties-plugin.table.header.action"),
  };

  return (
    <SubpageForm.SubpageTable>
      <thead>
      <tr>
        <th>{header.key}</th>
        <th>{header.allowedValues}</th>
        <th>{header.defaultValue}</th>
        <th>{isMissingPropertiesLoading ? <SmallLoadingSpinner/> : header.mandatoryValue}</th>
        <th>{header.action}</th>
      </tr>
      </thead>
      <tbody>
      {Object.keys(globallyPredefinedKeys).length === 0 && entries.length === 0 ? (
        <SubpageForm.SubpageTable.EmptyRow colSpan={5}>
          {t("scm-custom-properties-plugin.config.predefinedKeys.empty")}
        </SubpageForm.SubpageTable.EmptyRow>
      ) : null}
      {Object.entries(globallyPredefinedKeys)
        .sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
        .map(([key, definition]) => (
          <tr key={`global-${key}`}>
            <td data-label={header.key}>
              {key}
              <PropertyTag>{globallyPredefinedTag}</PropertyTag>
            </td>
            <td data-label={header.allowedValues}>
              <AllowedValuesColumn allowedValues={definition.allowedValues}/>
            </td>
            <td data-label={header.defaultValue}>
              <ValueModeColumn defaultValue={definition.defaultValue} mode={definition.mode}/>
            </td>
            <td data-label={header.mandatoryValue} colSpan={2}>
              <MandatoryValueSet
                propertyKey={key}
                mode={definition.mode}
                violatingRepositories={missingProperties[key]}
                isLoading={isMissingPropertiesLoading}
              />
            </td>
          </tr>
        ))}
      {entries.map((entry, index) => (
        <tr key={entry.id}>
          <td data-label={header.key}>{entry.key}</td>
          <td data-label={header.allowedValues}>
            <AllowedValuesColumn allowedValues={entry.allowedValues}/>
          </td>
          <td data-label={header.defaultValue}>
            <ValueModeColumn defaultValue={entry.defaultValue} mode={entry.mode}/>
          </td>
          <td data-label={header.mandatoryValue}>
            <MandatoryValueSet
              propertyKey={entry.key}
              mode={entry.mode}
              violatingRepositories={missingProperties[entry.key]}
              isLoading={isMissingPropertiesLoading}
            />
          </td>
          <td data-label={header.action} className="has-text-right">
            <div className="is-flex is-justify-content-flex-end is-align-items-center" style={{gap: "0.5rem"}}>
              <SubpageForm.Subpage.OpenIconButton
                subpage={keyOfSubpage(index)}
                aria-label={t("scm-custom-properties-plugin.table.body.edit", {key: entry.key})}
              >
                <Icon>edit</Icon>
              </SubpageForm.Subpage.OpenIconButton>
              <DeleteAction originalKey={entry.key} update={update} config={config} isLoading={isUpdating}/>
            </div>
          </td>
        </tr>
      ))}
      </tbody>
    </SubpageForm.SubpageTable>
  );
};

const AddKeyEditor: FC = () => {
  const {control} = useScmFormContext();
  const {fields, append} = useFieldArray({control, name: "predefinedKeys"});
  const [index, setIndex] = useState<number | null>(null);
  const appended = useRef(false);

  useEffect(() => {
    if (!appended.current) {
      appended.current = true;
      setIndex(fields.length);
      append({key: "", allowedValues: [], defaultValue: "", mode: "NONE"});
    }
  }, [append, fields.length]);

  if (index === null) {
    return null;
  }

  return <PredefinedKeyFields index={index} originalKey=""/>;
};

const MissingPropertiesList: FC<{
  propertyKey: string;
  missingProperties: Record<string, string[]>;
}> = ({propertyKey, missingProperties}) => {
  const [t] = useTranslation("plugins");
  const violatingRepositories = missingProperties[propertyKey];

  if (!violatingRepositories) {
    return <p>{t("scm-custom-properties-plugin.config.missingMandatoryProperties.notMissing", {key: propertyKey})}</p>;
  }

  return (
    <div className="content">
      <p>{t("scm-custom-properties-plugin.config.missingMandatoryProperties.paragraph", {key: propertyKey})}</p>
      <ul>
        {[...violatingRepositories]
          .sort((repoA, repoB) => repoA.localeCompare(repoB))
          .map((repo) => (
            <li key={repo}>
              <Link to={{pathname: `/repo/${repo}/custom-properties`, hash: "missing-properties"}}>{repo}</Link>
            </li>
          ))}
      </ul>
    </div>
  );
};

const ConfigFormBody = <T extends BaseConfig>({
                                                level,
                                                config,
                                                update,
                                                isUpdating,
                                                missingProperties,
                                                isMissingPropertiesLoading,
                                              }: Omit<ConfigFormProps<T>, "isReadOnly">) => {
  const [t] = useTranslation("plugins");
  const {control, getValues} = useScmFormContext();
  const {fields, remove} = useFieldArray({control, name: "predefinedKeys"});
  const {activeSubpage} = useSubpageContext();
  const entries = fields as unknown as (PredefinedKeyEntry & { id: string })[];

  useEffect(() => {
    if (activeSubpage !== null) {


      return;
    }
    const currentEntries = (getValues("predefinedKeys") as PredefinedKeyEntry[]) ?? [];
    const emptyIndices = currentEntries.reduce<number[]>((acc, entry, index) => {
      if (!entry?.key) {
        acc.push(index);
      }
      return acc;
    }, []);
    if (emptyIndices.length > 0) {
      remove(emptyIndices);
    }
  }, [activeSubpage, getValues, remove]);

  const globallyPredefinedKeys =
    level === "namespace" ? (config as unknown as NamespaceConfig).globallyPredefinedKeys : ({} as PredefinedKeys);

  const mandatoryKeys = Array.from(
    new Set(
      [
        ...Object.entries(globallyPredefinedKeys).map(([key, definition]) => ({key, mode: definition.mode})),
        ...entries.map((entry) => ({key: entry.key, mode: entry.mode})),
      ]
        .filter((item) => item.mode === "MANDATORY")
        .map((item) => item.key),
    ),
  );

  return (
    <>
      <SubpageForm.Overview>
        {level === "global" ? (
          <>
            <GeneralSettingsTable/>
            <SubpageForm.Divider/>
          </>
        ) : null}

        <SubpageForm.Subpage.Header
          subpage="add-key"
          label={t("scm-custom-properties-plugin.config.predefinedKeys.subtitle")}
        >
          {t("scm-custom-properties-plugin.table.footer.addKey")}
        </SubpageForm.Subpage.Header>
        <PredefinedKeysTable
          entries={entries}
          globallyPredefinedKeys={globallyPredefinedKeys}
          config={config}
          update={update}
          isUpdating={isUpdating}
          missingProperties={missingProperties}
          isMissingPropertiesLoading={isMissingPropertiesLoading}
        />
      </SubpageForm.Overview>

      {level === "global" ? (
        <>
          <SubpageForm.Subpage name="enabled" title={t("scm-custom-properties-plugin.config.general.enabled.title")}>
            <Form.BooleanRadio
              name="enabled"
              label={t("scm-custom-properties-plugin.config.general.enabled.subtitle")}
              trueLabel={t("scm-custom-properties-plugin.config.general.enabled.radio.true")}
              falseLabel={t("scm-custom-properties-plugin.config.general.enabled.radio.false")}
            />
          </SubpageForm.Subpage>
          <SubpageForm.Subpage
            name="enableNamespaceConfig"
            title={t("scm-custom-properties-plugin.config.general.enableNamespaceConfig.title")}
          >
            <Form.BooleanRadio
              name="enableNamespaceConfig"
              label={t("scm-custom-properties-plugin.config.general.enableNamespaceConfig.subtitle")}
              trueLabel={t("scm-custom-properties-plugin.config.general.enableNamespaceConfig.radio.true")}
              falseLabel={t("scm-custom-properties-plugin.config.general.enableNamespaceConfig.radio.false")}
            />
          </SubpageForm.Subpage>
        </>
      ) : null}

      {entries.map((entry, index) => (
        <SubpageForm.Subpage
          key={entry.id}
          name={keyOfSubpage(index)}
          title={entry.key || t("scm-custom-properties-plugin.config.general.predefinedKeys.title")}
        >
          <PredefinedKeyFields index={index} originalKey={entry.key}/>
        </SubpageForm.Subpage>
      ))}

      <SubpageForm.Subpage name="add-key" title={t("scm-custom-properties-plugin.table.footer.addKey")}>
        <AddKeyEditor/>
      </SubpageForm.Subpage>

      {mandatoryKeys.map((key) => (
        <SubpageForm.Subpage
          key={`missing-${key}`}
          name={missingSubpage(key)}
          title={t("scm-custom-properties-plugin.config.missingMandatoryProperties.title")}
        >
          <MissingPropertiesList propertyKey={key} missingProperties={missingProperties}/>
        </SubpageForm.Subpage>
      ))}
    </>
  );
};

const ConfigForm = <T extends BaseConfig>({
                                            level,
                                            config,
                                            update,
                                            isReadOnly,
                                            isUpdating,
                                            missingProperties,
                                            isMissingPropertiesLoading,
                                          }: ConfigFormProps<T>) => {
  const [t] = useTranslation("plugins");
  const queryClient = useQueryClient();

  const defaultValues = useMemo<ConfigFormShape>(
    () => ({...(config as BaseConfig), predefinedKeys: toEntries(config.predefinedKeys)}) as ConfigFormShape,
    [config],
  );

  const invalidateMissingProperties = () =>
    queryClient.invalidateQueries({queryKey: ["missingMandatoryProperties"]});

  const onSubmit = async (data: ConfigFormShape) => {
    await update({...data, predefinedKeys: toRecord(data.predefinedKeys)} as unknown as T);
    await invalidateMissingProperties();
  };

  const updateAndRefresh: ConfigUpdate<T> = (cfg) => {
    const result = update(cfg);
    void Promise.resolve(result).then(invalidateMissingProperties);
    return result;
  };

  return (
    <SubpageForm<ConfigFormShape>
      title={t(
        level === "global"
          ? "scm-custom-properties-plugin.config.general.subtitle"
          : "scm-custom-properties-plugin.config.title",
      )}
      translationPath={["plugins", "scm-custom-properties-plugin.config"]}
      defaultValues={defaultValues}
      readOnly={isReadOnly}
      onSubmit={onSubmit}
      successMessageFallback={t("scm-custom-properties-plugin.config.submitSuccess")}
    >
      <ConfigFormBody
        level={level}
        config={config}
        update={updateAndRefresh}
        isUpdating={isUpdating}
        missingProperties={missingProperties}
        isMissingPropertiesLoading={isMissingPropertiesLoading}
      />
    </SubpageForm>
  );
};

export default ConfigForm;
