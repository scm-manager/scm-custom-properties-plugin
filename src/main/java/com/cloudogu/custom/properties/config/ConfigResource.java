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

package com.cloudogu.custom.properties.config;

import com.cloudogu.custom.properties.CustomPropertiesContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import sonia.scm.config.ConfigurationPermissions;
import sonia.scm.repository.NamespacePermissions;

@Path(CustomPropertiesContext.CUSTOM_PROPERTIES_V2_PATH)
public class ConfigResource {

  private static final String GLOBAL_CONFIG_PATH = "/global-configuration";
  private static final String NAMESPACE_CONFIG_PATH = "/namespace-configuration/{namespace}";

  private final ConfigService configService;
  private final GlobalConfigMapper globalConfigMapper;
  private final NamespaceConfigMapper namespaceConfigMapper;

  @Inject
  public ConfigResource(ConfigService configService,
                        GlobalConfigMapper globalConfigMapper,
                        NamespaceConfigMapper namespaceConfigMapper) {
    this.configService = configService;
    this.globalConfigMapper = globalConfigMapper;
    this.namespaceConfigMapper = namespaceConfigMapper;
  }

  @GET
  @Path(GLOBAL_CONFIG_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public GlobalConfigDto getGlobalConfig() {
    ConfigurationPermissions.read(CustomPropertiesContext.CONFIG_PERMISSION_NAME).check();
    GlobalConfig globalConfig = configService.getGlobalConfig();
    return globalConfigMapper.map(globalConfig);
  }

  @PUT
  @Path(GLOBAL_CONFIG_PATH)
  public void setGlobalConfig(@Valid GlobalConfigDto globalConfigDto) {
    ConfigurationPermissions.write(CustomPropertiesContext.CONFIG_PERMISSION_NAME).check();
    configService.setGlobalConfig(globalConfigMapper.map(globalConfigDto));
  }

  @GET
  @Path(NAMESPACE_CONFIG_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public NamespaceConfigDto getNamespaceConfig(@PathParam("namespace") String namespace) {
    NamespacePermissions.custom(CustomPropertiesContext.CONFIG_PERMISSION_NAME, namespace).check();
    return namespaceConfigMapper.map(configService.getNamespaceConfig(namespace), configService.getGlobalConfig(), namespace);
  }

  @PUT
  @Path(NAMESPACE_CONFIG_PATH)
  public void setNamespaceConfig(@PathParam("namespace") String namespace, @Valid NamespaceConfigDto namespaceConfigDto) {
    NamespacePermissions.custom(CustomPropertiesContext.CONFIG_PERMISSION_NAME, namespace).check();
    configService.setNamespaceConfig(namespace, namespaceConfigMapper.map(namespaceConfigDto));
  }
}
