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
import com.cloudogu.custom.properties.config.PredefinedKeyDto;
import com.cloudogu.custom.properties.config.PredefinedKeyMapper;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import sonia.scm.ContextEntry;
import sonia.scm.api.v2.resources.ErrorDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.web.VndMediaType;

import java.util.Collection;
import java.util.Map;

import static com.cloudogu.custom.properties.CustomPropertiesContext.MULTIPLE_CHOICE_VALUE_SEPARATOR;
import static sonia.scm.NotFoundException.notFound;

@OpenAPIDefinition(tags = {
  @Tag(name = "Custom Properties", description = "Custom Properties related endpoints")
})
@Path(CustomPropertiesContext.CUSTOM_PROPERTIES_V2_PATH)
public class CustomPropertiesResource {

  public static final String PROPERTY_KEY_VALUE_MEDIA_TYPE = VndMediaType.PREFIX + "CustomProperty" + VndMediaType.SUFFIX;

  private final RepositoryManager repositoryManager;
  private final CustomPropertiesService service;
  private final ConfigService configService;
  private final CustomPropertyMapper customPropertyMapper;
  private final PredefinedKeyMapper predefinedKeyMapper;
  private final RepositoryMapper repositoryMapper;
  private final CustomPropertiesSearchService searchService;

  @Inject
  public CustomPropertiesResource(RepositoryManager repositoryManager,
                                  CustomPropertiesService service,
                                  ConfigService configService,
                                  CustomPropertyMapper customPropertyMapper,
                                  PredefinedKeyMapper predefinedKeyMapper,
                                  RepositoryMapper repositoryMapper,
                                  CustomPropertiesSearchService searchService) {
    this.repositoryManager = repositoryManager;
    this.service = service;
    this.configService = configService;
    this.customPropertyMapper = customPropertyMapper;
    this.predefinedKeyMapper = predefinedKeyMapper;
    this.repositoryMapper = repositoryMapper;
    this.searchService = searchService;
  }

  @GET
  @Path("/{namespace}/{name}")
  @Operation(
    summary = "Get all custom properties for the repository",
    description = "Gets all custom properties for the specified repository, if available.",
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
  public Collection<CustomPropertyDto> read(@PathParam("namespace") String namespace,
                                            @PathParam("name") String name,
                                            @DefaultValue(MULTIPLE_CHOICE_VALUE_SEPARATOR) @QueryParam("separator") String separator) {
    checkIsFeatureEnabled();
    Repository repository = tryToGetRepository(namespace, name);
    RepositoryPermissions.read(repository).check();

    Collection<CustomProperty> entities = service.get(repository);
    return customPropertyMapper.mapToDtoCollection(entities, repository, separator);
  }

  @POST
  @Path("/{namespace}/{name}")
  @Operation(
    summary = "Creates custom property for repository",
    description = "Creates a custom property for the specified repository with a unique key, if valid.",
    tags = "Custom Properties",
    operationId = "custom-properties_create_repository_key_value"
  )
  @ApiResponse(responseCode = "204", description = "create success")
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
  public void create(@PathParam("namespace") String namespace,
                     @PathParam("name") String name,
                     @NotNull @Valid WriteCustomPropertyDto contentDto) {
    checkIsFeatureEnabled();
    Repository repository = tryToGetRepository(namespace, name);
    RepositoryPermissions.modify(repository).check();

    service.create(repository, customPropertyMapper.map(contentDto));
  }

  @GET
  @Path("/{namespace}/{name}/predefined-keys")
  @Operation(
    summary = "Get all predefined custom property keys for the repository",
    description = "Gets all predefined custom property keys for the specified repository, if available.",
    tags = "Custom Properties",
    operationId = "custom-properties_get_all_repository_predefined_keys"
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
  public Map<String, PredefinedKeyDto> readPredefinedKeys(@PathParam("namespace") String namespace,
                                                          @PathParam("name") String name,
                                                          @QueryParam("filter") @DefaultValue("") String filter) {
    checkIsFeatureEnabled();
    Repository repository = tryToGetRepository(namespace, name);
    RepositoryPermissions.read(repository).check();

    return predefinedKeyMapper.mapAll(service.getFilteredPredefinedKeys(repository.getNamespace(), filter));
  }

  @PUT
  @Path("/{namespace}/{name}/{key}")
  @Operation(
    summary = "Replaces custom property for repository",
    description = "Replaces a custom property for the specified repository, if valid and existing.",
    tags = "Custom Properties",
    operationId = "custom-properties_replace_repository_key_value"
  )
  @ApiResponse(responseCode = "204", description = "replace success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the general repository write privilege, or plugin deactivated")
  @ApiResponse(responseCode = "404", description = "not found")
  @ApiResponse(responseCode = "409", description = "new custom property already exists")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  @Consumes(PROPERTY_KEY_VALUE_MEDIA_TYPE)
  public void update(@PathParam("namespace") String namespace,
                     @PathParam("name") String name,
                     @PathParam("key") String key,
                     @NotNull @Valid WriteCustomPropertyDto replacementDto) {
    checkIsFeatureEnabled();
    Repository repository = tryToGetRepository(namespace, name);
    RepositoryPermissions.modify(repository).check();

    service.update(repository, key, customPropertyMapper.map(replacementDto));
  }

  @Operation(
    summary = "Deletes custom property from repository",
    description = "Deletes a custom property for the specified repository, if existing.",
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
  @Path("/{namespace}/{name}/{key}")
  public void delete(@PathParam("namespace") String namespace,
                     @PathParam("name") String name,
                     @PathParam("key") String key) {
    checkIsFeatureEnabled();
    Repository repository = tryToGetRepository(namespace, name);
    RepositoryPermissions.modify(repository).check();

    service.delete(repository, key);
  }

  @Operation(
    summary = "Filters repositories that match filters applied to their custom properties",
    description = """
      Returns a list of repositories, if it contains custom properties that matches all the supplied filters.
      The filters are applied in a case insensitive way.
      
      The filters also support multiple choice values, by using a separation character.
      The separation character can be specified with the `separator` query parameter.
      If the `separator` parameter is not set, then the default character is used, which is the tab stop character (`\\t`).
      The separation character might need to be URL encoded.
      The encoding of `\\t` would be `%09`.
      The following characters are not allowed to be used as a separation character, because their use is reserved for other purposes:
      - `?`
      - `*`
      - `=`
      The order of the multiple choice values is not considered.
      
      The filters also support the `?` and `*` wildcards.
      - `?`: Any one character
      - `*`: Any multiple characters and with any amount of them
      
      Examples:
      
      If you want to search for repositories that have at least one property with the values Java and TypeScript:
      `?value=Java%09TypeScript`
      
      If you want to search for repositories that have at least one property with the key Language:
      `?key=Language`
      
      If you want to search for repositories that have at least one property with the key Language and the values Java and TypeScript:
      `?property=Language=Java%09TypeScript`
      
      If you want to use a semicolon as a separation character:
      `?property=Language=Java;TypeScript&separator=;`
      
      If you want to additionally exclude archived repositories from the result set:
      `?excludeArchived=true`
      By default, archived repositories are included within the result.
      
      If you want to additionally include the custom properties of each repository to the result set:
      `?includeProps=true`
      Per default, custom properties are excluded within the result.
      
      It is also possible to combine multiple filters, each of them is combined via a logical AND:
      `?key=Analysis&value=Java&property=Version=1.*`
      """,
    tags = "Custom Properties",
    operationId = "custom-properties_find_repositories_with_custom_properties"
  )
  @ApiResponse(responseCode = "200", description = "success")
  @ApiResponse(responseCode = "401", description = "not authenticated / invalid credentials")
  @ApiResponse(responseCode = "403", description = "not authorized, the current user does not have the general repository write privilege, or plugin deactivated")
  @ApiResponse(
    responseCode = "500",
    description = "internal server error",
    content = @Content(
      mediaType = VndMediaType.ERROR_TYPE,
      schema = @Schema(implementation = ErrorDto.class)
    )
  )
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/repositories")
  public Collection<BasicRepositoryDto> findRepositoriesWithCustomProperties(@QueryParam("key") String key,
                                                                             @QueryParam("value") String value,
                                                                             @QueryParam("property") @Pattern(regexp = ".+=.+", message = "Property must match the format <key>=<value>") String property,
                                                                             @QueryParam("excludeArchived") boolean excludeArchived,
                                                                             @QueryParam("includeProps") boolean includeProps,
                                                                             @QueryParam("separator") @Pattern(regexp = "^[^?*=]*$", message = "The characters '?', '*' and '=' are not allowed as a separator") @DefaultValue(MULTIPLE_CHOICE_VALUE_SEPARATOR) String separator) {
    checkIsFeatureEnabled();

    CustomPropertiesSearchService.Filter filter = new CustomPropertiesSearchService.Filter(
      key,
      applySeparatorForValue(value, separator),
      applySeparatorForProperty(property, separator),
      excludeArchived
    );

    return searchService.findRepositoriesWithCustomProperties(filter)
      .stream()
      .map(
        repoWithProps -> repositoryMapper.map(repoWithProps.repository(), repoWithProps.props(), includeProps, separator)
      )
      .toList();
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

  private String applySeparatorForValue(String value, String separator) {
    if (Strings.isNullOrEmpty(value) || separator.equals(MULTIPLE_CHOICE_VALUE_SEPARATOR)) {
      return value;
    }

    return value.replace(separator, MULTIPLE_CHOICE_VALUE_SEPARATOR);
  }

  private String applySeparatorForProperty(String property, String separator) {
    if (Strings.isNullOrEmpty(property) || separator.equals(MULTIPLE_CHOICE_VALUE_SEPARATOR)) {
      return property;
    }

    String[] keyAndValues = property.split("=", 2);
    if (!keyAndValues[1].contains(separator)) {
      return property;
    }

    return String.join("=", keyAndValues[0], keyAndValues[1].replace(separator, MULTIPLE_CHOICE_VALUE_SEPARATOR));
  }
}
