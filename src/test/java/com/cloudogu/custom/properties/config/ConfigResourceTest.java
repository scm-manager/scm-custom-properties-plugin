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


import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.MediaType;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.web.JsonMockHttpRequest;
import sonia.scm.web.JsonMockHttpResponse;
import sonia.scm.web.RestDispatcher;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware("Trainer Red")
class ConfigResourceTest {

  @Nested
  class GlobalConfigTest {

    @Mock
    private ConfigService configService;
    private RestDispatcher dispatcher;

    private final String domain = "https://scm-test.de/scm/api/";
    private final String globalConfigPath = "/v2/custom-properties/global-configuration";

    @BeforeEach
    void setUp() {
      ScmPathInfoStore pathInfoStore = new ScmPathInfoStore();
      pathInfoStore.set(() -> URI.create(domain));
      GlobalConfigMapper globalConfigMapper = new GlobalConfigMapperImpl();
      globalConfigMapper.setScmPathInfoStore(pathInfoStore);

      ConfigResource resource = new ConfigResource(configService, globalConfigMapper);
      dispatcher = new RestDispatcher();
      dispatcher.addSingletonResource(resource);
    }

    @Test
    void shouldNotGetGlobalConfigButReturnForbiddenBecausePermissionIsMissing() throws URISyntaxException {
      MockHttpRequest request = MockHttpRequest.get(globalConfigPath);
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @SubjectAware(permissions = {"configuration:read,write:customProperties"})
    void shouldReturnGlobalConfiguration() throws URISyntaxException {
      String expectedLink = domain + globalConfigPath.substring(1);

      when(configService.getGlobalConfig()).thenReturn(new GlobalConfig());
      MockHttpRequest request = MockHttpRequest.get(globalConfigPath);
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(200);
      JsonNode responseBody = response.getContentAsJson();
      assertThat(responseBody.get("enabled").asBoolean()).isTrue();
      assertThat(responseBody.get("_links").get("update").get("href").asText()).isEqualTo(expectedLink);
      assertThat(responseBody.get("_links").get("self").get("href").asText()).isEqualTo(expectedLink);
    }

    @Test
    void shouldNotSetGlobalConfigButReturnForbiddenBecausePermissionIsMissing() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put(globalConfigPath)
        .contentType(MediaType.APPLICATION_JSON)
        .json("{ 'enabled': false }");
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @SubjectAware(permissions = {"configuration:read,write:customProperties"})
    void shouldSetGlobalConfiguration() throws URISyntaxException {
      GlobalConfig expected = new GlobalConfig();
      expected.setEnabled(false);

      JsonMockHttpRequest request = JsonMockHttpRequest
        .put(globalConfigPath)
        .contentType(MediaType.APPLICATION_JSON)
        .json("{ 'enabled': false }");
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(204);
      verify(configService).setGlobalConfig(expected);
    }
  }
}
