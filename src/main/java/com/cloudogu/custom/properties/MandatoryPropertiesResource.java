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

package com.cloudogu.custom.properties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.config.ConfigurationPermissions;
import sonia.scm.repository.NamespacePermissions;
import sonia.scm.repository.Repository;
import sonia.scm.web.VndMediaType;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Path(CustomPropertiesContext.MANDATORY_PROPERTIES_V2_PATH)
public class MandatoryPropertiesResource {

  private final CustomPropertiesService customPropertiesService;

  @Inject
  public MandatoryPropertiesResource(CustomPropertiesService customPropertiesService) {
    this.customPropertiesService = customPropertiesService;
  }

  @GET
  @Path("/missing-properties")
  @Operation(
    summary = "Get all missing mandatory custom properties from all repositories",
    description = "Gets all missing mandatory custom properties from all repositories",
    tags = "Custom Properties",
    operationId = "read_missing_mandatory_properties"
  )
  @ApiResponse(responseCode = "200", description = "get success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the the general repository read privilege, or plugin deactivated")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Collection<String>> readMissingMandatoryProperties() {
    ConfigurationPermissions.read(CustomPropertiesContext.CONFIG_PERMISSION_NAME).check();
    return customPropertiesService.getMissingMandatoryProperties()
      .entrySet()
      .stream()
      .map(this::transformMissingPropertyEntry)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @GET
  @Path("/missing-properties/{namespace}")
  @Operation(
    summary = "Get all missing mandatory custom properties from all repositories of the specified namespace",
    description = "Gets all missing mandatory custom properties from all repositories of the specified namespace",
    tags = "Custom Properties",
    operationId = "read_missing_mandatory_properties_from_namespace"
  )
  @ApiResponse(responseCode = "200", description = "get success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the the general repository read privilege, or plugin deactivated")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Collection<String>> readMissingMandatoryPropertiesFromNamespace(@PathParam("namespace") String namespace) {
    NamespacePermissions.custom(CustomPropertiesContext.CONFIG_PERMISSION_NAME, namespace).check();
    return customPropertiesService.getMissingMandatoryPropertiesForNamespace(namespace)
      .entrySet()
      .stream()
      .map(this::transformMissingPropertyEntry)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map.Entry<String, Collection<String>> transformMissingPropertyEntry(Map.Entry<String, Collection<Repository>> entry) {
    return new AbstractMap.SimpleEntry<>(entry.getKey(), transformRepositories(entry.getValue()));
  }

  private Collection<String> transformRepositories(Collection<Repository> repositories) {
    return repositories.stream().map(repository -> repository.getNamespaceAndName().toString()).toList();
  }
}
