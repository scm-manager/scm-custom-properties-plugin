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

import { BaseConfig } from "../types";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Dialog, Icon, IconButton } from "@scm-manager/ui-core";
import { SmallLoadingSpinner } from "@scm-manager/ui-components";

type DeleteActionProps<T extends BaseConfig> = {
  originalKey: string;
  config: T;
  update: (config: T) => Promise<Response> | undefined;
  isLoading: boolean;
};

const DeleteAction = <T extends BaseConfig>({ originalKey, config, update, isLoading }: DeleteActionProps<T>) => {
  const [t] = useTranslation("plugins");
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  const onConfirmDelete = async () => {
    update({ ...config, predefinedKeys: config.predefinedKeys.filter((key) => key !== originalKey) });
  };

  return (
    <Dialog
      trigger={
        <IconButton aria-label={t("scm-custom-properties-plugin.table.body.delete", { key: originalKey })}>
          {isLoading ? <SmallLoadingSpinner /> : <Icon className="pl-2 pr-2">trash</Icon>}
        </IconButton>
      }
      title={t("scm-custom-properties-plugin.modal.deletePredefinedKey.title")}
      footer={[
        <Button key={`delete-${originalKey}`} onClick={onConfirmDelete}>
          {isLoading ? <SmallLoadingSpinner /> : t("scm-custom-properties-plugin.modal.deletePredefinedKey.submit")}
        </Button>,
        <Button
          key={`cancel-delete-${originalKey}`}
          variant="primary"
          autoFocus
          onClick={() => setIsDeleteModalOpen(false)}
        >
          {t("scm-custom-properties-plugin.modal.deletePredefinedKey.cancel")}
        </Button>,
      ]}
      open={isDeleteModalOpen}
      onOpenChange={setIsDeleteModalOpen}
    >
      {t("scm-custom-properties-plugin.modal.deletePredefinedKey.message", { key: originalKey })}
    </Dialog>
  );
};

export default DeleteAction;
