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

import { HalRepresentation, Link, Namespace, Repository } from "@scm-manager/ui-types";
import { useMutation, useQuery, useQueryClient } from "react-query";
import { CustomProperty, PredefinedKeys } from "./types";
import { apiClient, useRequiredIndexLink } from "@scm-manager/ui-api";

const customPropertyContentType = "application/vnd.scmm-CustomProperty+json;v=2";

export const useCreateCustomProperty = (repository: Repository) => {
  const queryClient = useQueryClient();
  const { mutateAsync, isLoading, error } = useMutation<unknown, Error, CustomProperty>(
    (customProperty) =>
      apiClient.post(
        requiredLink(repository._embedded?.customProperties as HalRepresentation, "create"),
        customProperty,
        customPropertyContentType,
      ),
    { onSuccess: () => queryClient.invalidateQueries(["repository", repository.namespace, repository.name]) },
  );

  return { createCustomProperty: mutateAsync, isLoading, error };
};

export const useEditCustomProperty = (repository: Repository) => {
  const queryClient = useQueryClient();
  const { mutateAsync, isLoading, error } = useMutation<unknown, Error, CustomProperty>(
    (customProperty) =>
      apiClient.put(requiredLink(customProperty, "update"), customProperty, customPropertyContentType),
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
        requiredLink(customProperty, "delete"),
        customPropertyContentType,
        {},
      ),
    { onSuccess: () => queryClient.invalidateQueries(["repository", repository.namespace, repository.name]) },
  );

  return { deleteCustomProperty: mutate, isLoading };
};

export const useQueryPredefinedKeys = (repository: Repository, filter: string) => {
  const predefinedKeysLink = requiredLink(repository, "predefinedCustomPropertyKeys");
  return useQuery<PredefinedKeys, Error>(
    ["repository", repository.namespace, repository.name, "predefinedKeys", filter],
    () => apiClient.get(`${predefinedKeysLink}?filter=${filter}`).then((response) => response.json()),
  );
};

export const useMissingMandatoryProperties = () => {
  const mandatoryPropertiesLink = useRequiredIndexLink("missingMandatoryProperties");
  return useQuery<Record<string, string[]>, Error>([], () =>
    apiClient.get(mandatoryPropertiesLink).then((response) => response.json()),
  );
};

export const useMissingMandatoryPropertiesForNamespace = (namespace: Namespace) => {
  const mandatoryPropertiesLink = requiredLink(namespace, "missingMandatoryProperties");
  return useQuery<Record<string, string[]>, Error>([], () =>
    apiClient.get(mandatoryPropertiesLink).then((response) => response.json()),
  );
};

const requiredLink = (halObject: HalRepresentation | undefined, linkName: string): string => {
  if (!halObject?._links || !halObject?._links[linkName]) {
    throw new Error("You are missing the needed permissions");
  }
  return (halObject._links[linkName] as Link).href;
};
