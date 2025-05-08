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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.web.VndMediaType;

@Path(CustomPropertiesResource.CUSTOM_PROPERTIES_V2_PATH + "/{namespace}/{name}")
public class CustomPropertiesResource {

  @SuppressWarnings("java:S1075")
  public static final String CUSTOM_PROPERTIES_V2_PATH = "/v2/custom-properties";
  public static final String PROPERTY_KEY_VALUE_MEDIA_TYPE = VndMediaType.PREFIX + "propertyKeyValue" + VndMediaType.SUFFIX;
  public static final String PROPERTY_KEY_VALUE_REPLACE_MEDIA_TYPE = VndMediaType.PREFIX + "propertyKeyValueReplace" + VndMediaType.SUFFIX;

  @GET
  @Operation(
    summary = "Get all repository key-value pairs",
    description = "Gets all key-value pairs from the specified repository, if available.",
    tags = "Custom Properties",
    operationId = "custom-properties_get_all_repository_key_values"
  )
  @ApiResponse(responseCode = "200", description = "get success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the the general repository read privilege")
  @ApiResponse(responseCode = "404", description = "not found")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllRepositoryKeyValuePairs(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    return Response.ok(namespace + "/" + name, MediaType.APPLICATION_JSON).build();
  }

  @Operation(
    summary = "Creates repository key-value pair",
    description = "Creates a key-value pairs for the specified repository, if valid.",
    tags = "Custom Properties",
    operationId = "custom-properties_create_repository_key_value"
  )
  @ApiResponse(responseCode = "200", description = "create success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the general repository write privilege")
  @ApiResponse(responseCode = "409", description = "already exists")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  @Consumes(PROPERTY_KEY_VALUE_MEDIA_TYPE)
  public Response createKeyValuePair(@PathParam("namespace") String namespace, @PathParam("name") String name, @NotNull @Valid KeyValueRestDto keyValueDto) {
    return Response.ok(namespace + "/" + name, PROPERTY_KEY_VALUE_MEDIA_TYPE).build();
  }

  @Operation(
    summary = "Replaces repository key-value pair",
    description = "Replaces a key-value pair for the specified repository, if valid and existing.",
    tags = "Custom Properties",
    operationId = "custom-properties_replace_repository_key_value"
  )
  @ApiResponse(responseCode = "200", description = "replace success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the general repository write privilege")
  @ApiResponse(responseCode = "404", description = "not found")
  @ApiResponse(responseCode = "409", description = "new key-value pair already exists")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  @Consumes(PROPERTY_KEY_VALUE_REPLACE_MEDIA_TYPE)
  public Response replaceKeyValuePair(@PathParam("namespace") String namespace, @PathParam("name") String name, @NotNull @Valid KeyValueReplaceRestDto keyValueReplaceDto) {
    return Response.ok(namespace + "/" + name, PROPERTY_KEY_VALUE_REPLACE_MEDIA_TYPE).build();
  }

  @Operation(
    summary = "Deletes repository key-value pair",
    description = "Deletes a key-value pair for the specified repository, if existing.",
    tags = "Custom Properties",
    operationId = "custom-properties_delete_repository_key_value"
  )
  @ApiResponse(responseCode = "200", description = "delete success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the general repository write privilege")
  @ApiResponse(responseCode = "404", description = "not found")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  @Consumes(PROPERTY_KEY_VALUE_MEDIA_TYPE)
  public Response deleteKeyValuePair(@PathParam("namespace") String namespace, @PathParam("name") String name, @NotNull @Valid KeyValueRestDto keyValueDto) {
    return Response.ok(namespace + "/" + name, PROPERTY_KEY_VALUE_REPLACE_MEDIA_TYPE).build();
  }

}
