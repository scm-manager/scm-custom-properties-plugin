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

import { HalRepresentation, Link, Repository } from "@scm-manager/ui-types";
import { useMutation, useQueryClient } from "react-query";
import { CustomProperty, ReplaceCustomProperty } from "./types";
import { apiClient } from "@scm-manager/ui-api";

const customPropertyContentType = "application/vnd.scmm-CustomProperty+json;v=2";
const replaceCustomPropertyContentType = "application/vnd.scmm-ReplaceCustomProperty+json;v=2";

export const useCreateCustomProperty = (repository: Repository) => {
  const queryClient = useQueryClient();
  const { mutateAsync, isLoading, error } = useMutation<unknown, Error, CustomProperty>(
    (customProperty) =>
      apiClient.post(requiredLink(repository, "customPropertiesCreate"), customProperty, customPropertyContentType),
    { onSuccess: () => queryClient.invalidateQueries(["repository", repository.namespace, repository.name]) },
  );

  return { createCustomProperty: mutateAsync, isLoading, error };
};

export const useEditCustomProperty = (repository: Repository) => {
  const queryClient = useQueryClient();
  const { mutateAsync, isLoading, error } = useMutation<unknown, Error, ReplaceCustomProperty>(
    (replaceCustomProperty) =>
      apiClient.put(
        requiredLink(repository, "customPropertiesUpdate"),
        replaceCustomProperty,
        replaceCustomPropertyContentType,
      ),
    { onSuccess: () => queryClient.invalidateQueries(["repository", repository.namespace, repository.name]) },
  );

  return { editCustomProperty: mutateAsync, isLoading, error };
};

export const useDeleteCustomProperty = (repository: Repository) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading } = useMutation<unknown, Error, CustomProperty>(
    (customProperty) =>
      apiClient.httpRequestWithJSONBody(
        "DELETE",
        requiredLink(repository, "customPropertiesDelete"),
        customPropertyContentType,
        {},
        customProperty,
      ),
    { onSuccess: () => queryClient.invalidateQueries(["repository", repository.namespace, repository.name]) },
  );

  return { deleteCustomProperty: mutate, isLoading };
};

const requiredLink = (halObject: HalRepresentation, linkName: string): string => {
  if (!halObject._links[linkName]) {
    throw new Error("You are missing the needed permissions");
  }
  return (halObject._links[linkName] as Link).href;
};
