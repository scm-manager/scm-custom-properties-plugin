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

import com.cloudogu.custom.properties.config.ConfigService;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sonia.scm.ContextEntry;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.web.VndMediaType;

import java.util.Collection;

import static sonia.scm.NotFoundException.notFound;

@OpenAPIDefinition(tags = {
  @Tag(name = "Custom Properties", description = "Custom Properties related endpoints")
})
@Path(CustomPropertiesContext.CUSTOM_PROPERTIES_V2_PATH)
public class CustomPropertiesResource {

  public static final String PROPERTY_KEY_VALUE_MEDIA_TYPE = VndMediaType.PREFIX + "CustomProperty" + VndMediaType.SUFFIX;
  public static final String PROPERTY_KEY_VALUE_REPLACE_MEDIA_TYPE = VndMediaType.PREFIX + "ReplaceCustomProperty" + VndMediaType.SUFFIX;

  private final RepositoryManager repositoryManager;
  private final CustomPropertiesService service;
  private final ConfigService configService;
  private final CustomPropertyMapper customPropertyMapper;

  @Inject
  public CustomPropertiesResource(RepositoryManager repositoryManager,
                                  CustomPropertiesService service,
                                  ConfigService configService,
                                  CustomPropertyMapper customPropertyMapper) {
    this.repositoryManager = repositoryManager;
    this.service = service;
    this.configService = configService;
    this.customPropertyMapper = customPropertyMapper;
  }

  @GET
  @Path("/{namespace}/{name}")
  @Operation(
    summary = "Get all repository key-value pairs",
    description = "Gets all key-value pairs from the specified repository, if available.",
    tags = "Custom Properties",
    operationId = "custom-properties_get_all_repository_key_values"
  )
  @ApiResponse(responseCode = "200", description = "get success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the the general repository read privilege, or plugin deactivated")
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
  public Collection<CustomPropertyDto> read(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    checkIsFeatureEnabled();
    Repository repository = tryToGetRepository(namespace, name);
    RepositoryPermissions.read(repository).check();

    Collection<CustomProperty> entities = service.get(repository);
    return customPropertyMapper.mapToDtoCollection(entities);
  }

  @POST
  @Path("/{namespace}/{name}")
  @Operation(
    summary = "Creates repository key-value pair",
    description = "Creates a key-value pairs for the specified repository, if valid.",
    tags = "Custom Properties",
    operationId = "custom-properties_create_repository_key_value"
  )
  @ApiResponse(responseCode = "200", description = "create success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the general repository write privilege, or plugin deactivated")
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
  public Response create(@PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @NotNull @Valid CustomPropertyDto customPropertyDto) {
    checkIsFeatureEnabled();
    Repository repository = tryToGetRepository(namespace, name);
    RepositoryPermissions.modify(repository).check();

    service.create(repository, customPropertyMapper.map(customPropertyDto));

    return Response.noContent().build();
  }

  @PUT
  @Path("/{namespace}/{name}")
  @Operation(
    summary = "Replaces repository key-value pair",
    description = "Replaces a key-value pair for the specified repository, if valid and existing.",
    tags = "Custom Properties",
    operationId = "custom-properties_replace_repository_key_value"
  )
  @ApiResponse(responseCode = "200", description = "replace success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the general repository write privilege, or plugin deactivated")
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
  public Response update(@PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @NotNull @Valid ReplaceCustomPropertyDto replaceCustomPropertyDto) {
    checkIsFeatureEnabled();
    Repository repository = tryToGetRepository(namespace, name);
    RepositoryPermissions.modify(repository).check();

    CustomPropertyMapper.CustomPropertyReplacement replacement = customPropertyMapper.map(replaceCustomPropertyDto);
    service.replace(repository, replacement.oldEntity(), replacement.newEntity());

    return Response.noContent().build();
  }

  @Operation(
    summary = "Deletes repository key-value pair",
    description = "Deletes a key-value pair for the specified repository, if existing.",
    tags = "Custom Properties",
    operationId = "custom-properties_delete_repository_key_value"
  )
  @ApiResponse(responseCode = "204", description = "delete success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the general repository write privilege, or plugin deactivated")
  @ApiResponse(responseCode = "404", description = "not found")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  @DELETE
  @Path("/{namespace}/{name}")
  @Consumes(PROPERTY_KEY_VALUE_MEDIA_TYPE)
  public Response delete(@PathParam("namespace") String namespace,
                         @PathParam("name") String name,
                         @NotNull @Valid CustomPropertyDto customPropertyDto) {
    checkIsFeatureEnabled();
    Repository repository = tryToGetRepository(namespace, name);
    RepositoryPermissions.modify(repository).check();

    service.delete(repository, customPropertyMapper.map(customPropertyDto));
    return Response.noContent().build();
  }

  private void checkIsFeatureEnabled() {
    if (!configService.getGlobalConfig().isEnabled()) {
      throw new ForbiddenException();
    }
  }

  private Repository tryToGetRepository(String namespace, String name) {
    NamespaceAndName namespaceAndName = new NamespaceAndName(namespace, name);
    Repository repository = repositoryManager.get(namespaceAndName);
    if (repository == null) {
      throw notFound(ContextEntry.ContextBuilder.entity(namespaceAndName));
    }

    return repository;
  }

}
