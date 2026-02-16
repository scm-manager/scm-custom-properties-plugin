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
  Subtitle,
  LinkButton,
  IconButton,
  Icon,
  Dialog,
  Button,
  useDocumentTitleForRepository,
} from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";
import { SmallLoadingSpinner } from "@scm-manager/ui-components";
import { CustomProperty } from "../types";
import { useDeleteCustomProperty } from "../hooks";
import CenteredTableFooter from "../component/CenteredTableFooter";
import MinWidthTableCell from "../component/MinWidthTableCell";
import PropertyTag from "../component/PropertyTag";
import { useLocation } from "react-router";

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

//Copy and Paste from the Core, due to the limitation of core version for this plugin
const WarningIcon: FC = () => {
  return (
    <span className="icon has-text-warning" aria-hidden="true">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        id="Ebene_1"
        data-name="Ebene 1"
        height="1em"
        width="1em"
        viewBox="0 0 140 140"
        fill="currentColor"
        className="fas fa-fw fa-custom-icon fa-lg"
      >
        <defs>
          <mask id="cutoutMask">
            <rect x="0" y="0" width="140" height="140" fill="white" />
            <path
              className="cls-2"
              d="M79.93,109.67c-2.75,2.75-6.06,4.13-9.93,4.13s-7.18-1.38-9.93-4.13-4.13-6.06-4.13-9.93,1.38-7.18,4.13-9.93,6.06-4.13,9.93-4.13,7.18,1.38,9.93,4.13,4.13,6.06,4.13,9.93-1.38,7.18-4.13,9.93ZM59.98,76.01c0,1.17.41,2.14,1.23,2.9s1.82,1.14,2.99,1.14h11.6c1.17,0,2.17-.38,2.99-1.14s1.23-1.73,1.23-2.9l2.46-47.81c0-1.17-.41-2.2-1.23-3.08s-1.82-1.32-2.99-1.32h-16.52c-1.17,0-2.17.44-2.99,1.32s-1.23,1.9-1.23,3.08l2.46,47.81Z"
              fill="black"
            />
          </mask>
        </defs>
        <path
          className="cls-1"
          d="M125,0c4.17,0,7.71,1.46,10.62,4.38s4.38,6.46,4.38,10.62v110c0,4.17-1.46,7.71-4.38,10.63-2.92,2.92-6.46,4.38-10.62,4.38H15c-4.17,0-7.71-1.46-10.62-4.38-2.92-2.92-4.38-6.46-4.38-10.63V15c0-4.17,1.46-7.71,4.38-10.62S10.83,0,15,0h110Z"
          mask="url(#cutoutMask)"
          fill="currentColor"
        />
      </svg>
    </span>
  );
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

  useEffect(() => {
    if (location.hash) {
      document.getElementById(location.hash.substring(1))?.scrollIntoView({ behavior: "instant" });
    }
  }, [location.hash]);

  return (
    <table className="table">
      <thead>
        <tr>
          <th>{t("scm-custom-properties-plugin.table.header.key")}</th>
          <th>{t("scm-custom-properties-plugin.table.header.value")}</th>
          <th>{t("scm-custom-properties-plugin.table.header.action")}</th>
        </tr>
      </thead>
      <tbody>
        {[...customProperties]
          .sort((propA, propB) => propA.key.localeCompare(propB.key))
          .map((property) => (
            <tr key={property.key}>
              <td>{property.key}</td>
              <td>
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
              <MinWidthTableCell>
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
                  <span className="ml-2">
                    <WarningIcon>exclamation</WarningIcon>
                  </span>
                </div>
              </th>
              <th>{t("scm-custom-properties-plugin.table.header.action")}</th>
            </tr>
            {[...missingProperties]
              .sort((keyA, keyB) => keyA.localeCompare(keyB))
              .map((value) => (
                <tr key={value}>
                  <td colSpan={2}>{value}</td>
                  <MinWidthTableCell>
                    {isCreateAllowed && <MissingPropertyAction missingProperty={value} modifyUrl={modifyUrl} />}
                  </MinWidthTableCell>
                </tr>
              ))}
          </>
        ) : null}
      </tbody>
      {isCreateAllowed ? (
        <CenteredTableFooter>
          <tr>
            <td colSpan={3}>
              <LinkButton to={modifyUrl} variant="primary">
                {t("scm-custom-properties-plugin.table.footer.add")}
              </LinkButton>
            </td>
          </tr>
        </CenteredTableFooter>
      ) : null}
    </table>
  );
};

type Props = {
  repository: Repository;
};

const CustomPropertiesOverview: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  useDocumentTitleForRepository(repository, t("scm-custom-properties-plugin.repository.subtitle"));
  const customProperties = (repository._embedded?.customProperties as { properties: CustomProperty[] }).properties;
  const missingMandatoryProperties = (repository._embedded?.missingMandatoryProperties as { missing: string[] })
    .missing;
  const modifyUrl = `/repo/${repository.namespace}/${repository.name}/custom-properties/modify`;

  return (
    <>
      <Subtitle>{t("scm-custom-properties-plugin.repository.subtitle")}</Subtitle>
      <CustomPropertiesTable
        repository={repository}
        customProperties={customProperties}
        modifyUrl={modifyUrl}
        missingProperties={missingMandatoryProperties}
      />
    </>
  );
};

export default CustomPropertiesOverview;
