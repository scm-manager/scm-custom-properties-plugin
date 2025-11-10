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

import React, { FC, useState } from "react";
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

type CustomPropertyActionProps = {
  repository: Repository;
  customProperty: CustomProperty;
  modifyUrl: string;
};

const CustomPropertyAction: FC<CustomPropertyActionProps> = ({ repository, customProperty, modifyUrl }) => {
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
  modifyUrl: string;
};

const CustomPropertiesTable: FC<CustomPropertiesTableProps> = ({ repository, customProperties, modifyUrl }) => {
  const [t] = useTranslation("plugins");
  const isCreateAllowed = (repository._embedded?.customProperties as HalRepresentation)?._links?.create !== undefined;
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
              <td>{property.value}</td>
              <MinWidthTableCell>
                <CustomPropertyAction repository={repository} customProperty={property} modifyUrl={modifyUrl} />
              </MinWidthTableCell>
            </tr>
          ))}
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
  const modifyUrl = `/repo/${repository.namespace}/${repository.name}/custom-properties/modify`;

  return (
    <>
      <Subtitle>{t("scm-custom-properties-plugin.repository.subtitle")}</Subtitle>
      <CustomPropertiesTable repository={repository} customProperties={customProperties} modifyUrl={modifyUrl} />
    </>
  );
};

export default CustomPropertiesOverview;
