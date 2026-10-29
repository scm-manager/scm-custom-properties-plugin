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
import { HalRepresentation, Repository } from "@scm-manager/ui-types";
import {
  LinkButton,
  IconButton,
  Icon,
  Dialog,
  Button,
  Level,
  SubpageForm,
  useDocumentTitleForRepository,
} from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";
import { SmallLoadingSpinner } from "@scm-manager/ui-components";
import { CustomProperty } from "../types";
import { useDeleteCustomProperty } from "../hooks";
import MinWidthTableCell from "../component/MinWidthTableCell";
import PropertyTag from "../component/PropertyTag";
import { useLocation, useNavigate } from "react-router-dom";
import { urls } from "@scm-manager/ui-api";
import CustomPropertiesBreadcrumb from "./CustomPropertiesBreadcrumb";
import CustomPropertiesEditor from "./CustomPropertiesEditor";

type CustomPropertyActionProps = {
  repository: Repository;
  customProperty: CustomProperty;
  modifyUrl: string;
  isCreateAllowed: boolean;
};

type MissingPropertyActionProps = {
  missingProperty: string;
  modifyUrl: string;
};

const MissingPropertyAction: FC<MissingPropertyActionProps> = ({ missingProperty, modifyUrl }) => {
  const [t] = useTranslation("plugins");

  return (
    <span>
      <LinkButton
        className="px-2"
        to={`${modifyUrl}?missingProperty=${encodeURIComponent(missingProperty)}`}
        aria-label={t("scm-custom-properties-plugin.table.body.edit", { key: missingProperty })}
      >
        <Icon>edit</Icon>
      </LinkButton>
    </span>
  );
};

const CustomPropertyAction: FC<CustomPropertyActionProps> = ({
  repository,
  customProperty,
  modifyUrl,
  isCreateAllowed,
}) => {
  const [t] = useTranslation("plugins");
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const { deleteCustomProperty, isLoading } = useDeleteCustomProperty(repository);
  const isEditAllowed = customProperty._links?.update !== undefined;
  const isDeletionAllowed = customProperty._links?.delete !== undefined;

  const confirmDelete = () => {
    setIsDeleteModalOpen(false);
    deleteCustomProperty(customProperty);
  };

  return (
    <>
      {customProperty.defaultProperty && isCreateAllowed ? (
        <span className="mr-4">
          <LinkButton
            className="px-2"
            to={`${modifyUrl}?defaultProperty=${encodeURIComponent(customProperty.key)}`}
            aria-label={t("scm-custom-properties-plugin.table.body.edit", { key: customProperty.key })}
          >
            <Icon>edit</Icon>
          </LinkButton>
        </span>
      ) : null}
      {isEditAllowed ? (
        <span className="mr-4">
          <LinkButton
            className="px-2"
            to={`${modifyUrl}?key=${encodeURIComponent(customProperty.key)}`}
            aria-label={t("scm-custom-properties-plugin.table.body.edit", { key: customProperty.key })}
          >
            <Icon>edit</Icon>
          </LinkButton>
        </span>
      ) : null}
      {isDeletionAllowed ? (
        <Dialog
          trigger={
            <IconButton aria-label={t("scm-custom-properties-plugin.table.body.delete", { key: customProperty.key })}>
              {isLoading ? <SmallLoadingSpinner /> : <Icon>trash</Icon>}
            </IconButton>
          }
          title={t("scm-custom-properties-plugin.modal.deleteCustomProperty.title")}
          footer={[
            <Button key={`delete-${customProperty.key}`} onClick={confirmDelete}>
              {t("scm-custom-properties-plugin.modal.deleteCustomProperty.submit")}
            </Button>,
            <Button
              key={`cancel-delete-${customProperty.key}`}
              variant="primary"
              autoFocus
              onClick={() => setIsDeleteModalOpen(false)}
            >
              {t("scm-custom-properties-plugin.modal.deleteCustomProperty.cancel")}
            </Button>,
          ]}
          open={isDeleteModalOpen}
          onOpenChange={setIsDeleteModalOpen}
        >
          {t("scm-custom-properties-plugin.modal.deleteCustomProperty.message", { key: customProperty.key })}
        </Dialog>
      ) : null}
    </>
  );
};

type CustomPropertiesTableProps = {
  repository: Repository;
  customProperties: CustomProperty[];
  missingProperties: string[];
  modifyUrl: string;
};

const CustomPropertiesTable: FC<CustomPropertiesTableProps> = ({
  repository,
  customProperties,
  modifyUrl,
  missingProperties,
}) => {
  const location = useLocation();
  const [t] = useTranslation("plugins");
  const isCreateAllowed = (repository._embedded?.customProperties as HalRepresentation)?._links?.create !== undefined;

  const defaultValueTag = t("scm-custom-properties-plugin.editor.defaultValueTag");
  const mandatoryValueTag = t("scm-custom-properties-plugin.editor.mandatoryValueTag");

  const keyHeader = t("scm-custom-properties-plugin.table.header.key");
  const valueHeader = t("scm-custom-properties-plugin.table.header.value");
  const actionHeader = t("scm-custom-properties-plugin.table.header.action");

  useEffect(() => {
    if (location.hash) {
      document.getElementById(location.hash.substring(1))?.scrollIntoView({ behavior: "instant" });
    }
  }, [location.hash]);

  return (
    <>
      <Level
        left={<span className="subtitle">{t("scm-custom-properties-plugin.navLink")}</span>}
        right={
          isCreateAllowed ? (
            <LinkButton to={`${modifyUrl}?mode=add`} variant="ghost">
              <Icon className="mr-1">edit</Icon>
              {t("scm-custom-properties-plugin.table.footer.add")}
            </LinkButton>
          ) : undefined
        }
      />
      <SubpageForm.SubpageTable>
        <thead>
          <tr>
            <th>{keyHeader}</th>
            <th>{valueHeader}</th>
            <th>{actionHeader}</th>
          </tr>
        </thead>
        <tbody>
          {customProperties.length === 0 && missingProperties.length === 0 ? (
            <SubpageForm.SubpageTable.EmptyRow colSpan={3}>
              {t("scm-custom-properties-plugin.table.empty")}
            </SubpageForm.SubpageTable.EmptyRow>
          ) : null}
          {[...customProperties]
            .sort((propA, propB) => propA.key.localeCompare(propB.key))
            .map((property) => (
              <tr key={property.key}>
                <td data-label={keyHeader}>{property.key}</td>
                <td data-label={valueHeader}>
                  {property.value.includes(property.separator) ? (
                    <ul>
                      {property.value
                        .split(property.separator)
                        .sort((a, b) => a.localeCompare(b))
                        .map((value) => (
                          <li key={`${property.key}-${value}`}>{value}</li>
                        ))}
                    </ul>
                  ) : (
                    <>
                      {property.value}
                      {property.defaultProperty && <PropertyTag>{defaultValueTag}</PropertyTag>}
                      {property.mandatory && <PropertyTag>{mandatoryValueTag}</PropertyTag>}
                    </>
                  )}
                </td>
                <MinWidthTableCell label={actionHeader}>
                  <CustomPropertyAction
                    repository={repository}
                    customProperty={property}
                    modifyUrl={modifyUrl}
                    isCreateAllowed={isCreateAllowed}
                  />
                </MinWidthTableCell>
              </tr>
            ))}
          {missingProperties.length !== 0 ? (
            <>
              <tr>
                <th id="missing-properties" colSpan={2}>
                  <div className="is-flex is-align-items-center">
                    {t("scm-custom-properties-plugin.table.header.missingProperty")}
                    <Icon className="has-text-warning">exclamation</Icon>
                  </div>
                </th>
                <th>{actionHeader}</th>
              </tr>
              {[...missingProperties]
                .sort((keyA, keyB) => keyA.localeCompare(keyB))
                .map((value) => (
                  <tr key={value}>
                    <td colSpan={2} data-label={keyHeader}>
                      {value}
                    </td>
                    <MinWidthTableCell label={actionHeader}>
                      {isCreateAllowed && <MissingPropertyAction missingProperty={value} modifyUrl={modifyUrl} />}
                    </MinWidthTableCell>
                  </tr>
                ))}
            </>
          ) : null}
        </tbody>
      </SubpageForm.SubpageTable>
    </>
  );
};

type Props = {
  repository: Repository;
};

const CustomPropertiesOverview: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const location = useLocation();
  const navigate = useNavigate();
  const customProperties = (repository._embedded?.customProperties as { properties: CustomProperty[] }).properties;
  const missingMandatoryProperties = (repository._embedded?.missingMandatoryProperties as { missing: string[] })
    .missing;
  const modifyUrl = `/repo/${repository.namespace}/${repository.name}/custom-properties`;

  const mode = urls.getValueStringFromLocationByKey(location, "mode");
  const existingPropertyKey = urls.getValueStringFromLocationByKey(location, "key");
  const defaultProperty = urls.getValueStringFromLocationByKey(location, "defaultProperty");
  const missingProperty = urls.getValueStringFromLocationByKey(location, "missingProperty");
  const isEditing = mode === "add" || !!existingPropertyKey || !!defaultProperty || !!missingProperty;

  const closeSubpage = () => navigate(modifyUrl);

  const subpageTitle = isEditing
    ? existingPropertyKey
      ? t("scm-custom-properties-plugin.editor.subtitle_edit", { key: existingPropertyKey })
      : t("scm-custom-properties-plugin.editor.subtitle_create")
    : undefined;

  const titleParts = subpageTitle
    ? [subpageTitle, t("scm-custom-properties-plugin.repository.subtitle")]
    : [t("scm-custom-properties-plugin.repository.subtitle")];
  useDocumentTitleForRepository(repository, ...titleParts);

  return (
    <>
      <CustomPropertiesBreadcrumb
        title={t("scm-custom-properties-plugin.repository.subtitle")}
        activeSubpageTitle={subpageTitle}
        onBack={closeSubpage}
      />
      {isEditing ? (
        <CustomPropertiesEditor repository={repository} />
      ) : (
        <CustomPropertiesTable
          repository={repository}
          customProperties={customProperties}
          modifyUrl={modifyUrl}
          missingProperties={missingMandatoryProperties}
        />
      )}
    </>
  );
};

export default CustomPropertiesOverview;
