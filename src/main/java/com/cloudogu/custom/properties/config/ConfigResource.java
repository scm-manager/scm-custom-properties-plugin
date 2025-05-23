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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import sonia.scm.config.ConfigurationPermissions;

@Path(CustomPropertiesContext.CUSTOM_PROPERTIES_V2_PATH)
public class ConfigResource {

  private static final String GLOBAL_CONFIG_PATH = "/global-configuration";

  private final ConfigService configService;
  private final GlobalConfigMapper globalConfigMapper;

  @Inject
  public ConfigResource(ConfigService configService, GlobalConfigMapper globalConfigMapper) {
    this.configService = configService;
    this.globalConfigMapper = globalConfigMapper;
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
  public void setGlobalConfig(GlobalConfigDto globalConfigDto) {
    ConfigurationPermissions.write(CustomPropertiesContext.CONFIG_PERMISSION_NAME).check();
    configService.setGlobalConfig(globalConfigMapper.map(globalConfigDto));
  }
}
