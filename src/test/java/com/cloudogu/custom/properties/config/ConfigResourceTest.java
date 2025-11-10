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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.web.JsonMockHttpRequest;
import sonia.scm.web.JsonMockHttpResponse;
import sonia.scm.web.RestDispatcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware("Trainer Red")
class ConfigResourceTest {

  private final String domain = "https://scm-test.de/scm/api/";
  private final String globalConfigPath = "/v2/custom-properties/global-configuration";
  private final String namespaceConfigPath = "/v2/custom-properties/namespace-configuration/hitchhiker";
  private final String namespace = "hitchhiker";

  @Mock
  private ConfigService configService;
  private RestDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    ScmPathInfoStore pathInfoStore = new ScmPathInfoStore();
    pathInfoStore.set(() -> URI.create(domain));

    GlobalConfigMapper globalConfigMapper = new GlobalConfigMapperImpl();
    globalConfigMapper.setScmPathInfoStore(pathInfoStore);
    NamespaceConfigMapper namespaceConfigMapper = new NamespaceConfigMapperImpl();
    namespaceConfigMapper.setScmPathInfoStore(pathInfoStore);
    namespaceConfigMapper.setPredefinedKeyMapper(new PredefinedKeyMapperImpl());

    ConfigResource resource = new ConfigResource(
      configService,
      globalConfigMapper,
      namespaceConfigMapper
    );
    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(resource);
  }

  @Nested
  class NamespaceConfigTest {

    @Test
    void shouldNotGetNamespaceConfigButReturnForbiddenBecausePermissionIsMissing() throws URISyntaxException {
      MockHttpRequest request = MockHttpRequest.get(namespaceConfigPath);
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @SubjectAware(permissions = {"namespace:customProperties:hitchhiker"})
    void shouldReturnNamespaceConfiguration() throws URISyntaxException {
      String expectedLink = domain + namespaceConfigPath.substring(1);

      GlobalConfig globalConfig = new GlobalConfig();
      globalConfig.setPredefinedKeys(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript"))
      ));
      when(configService.getGlobalConfig()).thenReturn(globalConfig);

      NamespaceConfig expected = new NamespaceConfig();
      expected.setPredefinedKeys(Map.of(
        "arbitrary", new PredefinedKey(List.of())
      ));
      when(configService.getNamespaceConfig(namespace)).thenReturn(expected);

      MockHttpRequest request = MockHttpRequest.get(namespaceConfigPath);
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(200);
      JsonNode responseBody = response.getContentAsJson();

      assertThat(responseBody.get("predefinedKeys").get("arbitrary").get("allowedValues").size()).isZero();

      assertThat(responseBody.get("globallyPredefinedKeys").get("lang").get("allowedValues").get(0).asText()).isEqualTo("Java");
      assertThat(responseBody.get("globallyPredefinedKeys").get("lang").get("allowedValues").get(1).asText()).isEqualTo("TypeScript");

      assertThat(responseBody.get("_links").get("update").get("href").asText()).isEqualTo(expectedLink);
      assertThat(responseBody.get("_links").get("self").get("href").asText()).isEqualTo(expectedLink);
    }

    @Test
    void shouldNotSetNamespaceConfigButReturnForbiddenBecausePermissionIsMissing() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put(namespaceConfigPath)
        .contentType(MediaType.APPLICATION_JSON)
        .json("{ 'predefinedKeys': {} }");
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(403);
    }

    @ParameterizedTest
    @ValueSource(
      strings = {
        "{ }",
        "{ 'predefinedKeys': null }",
        "{ 'predefinedKeys': { '': {'validationEnabled': false, 'allowedValues': [] } } }",
        "{ 'predefinedKeys': { 'Ungültiger/Schlüssel': {'validationEnabled': false, 'allowedValues': [] } } }"
      }
    )
    void shouldReturnBadRequestBecausePredefinedKeysAreMalformed(String predefinedKeys) throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put(namespaceConfigPath)
        .contentType(MediaType.APPLICATION_JSON)
        .json(predefinedKeys);
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @SubjectAware(permissions = {"namespace:customProperties:hitchhiker"})
    void shouldReturnBadRequestBecausePredefinedKeyContainsTooLongKey() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put(namespaceConfigPath)
        .contentType(MediaType.APPLICATION_JSON)
        .json(String.format(
          "{ 'predefinedKeys': { '%s': {'validationEnabled': false, 'allowedValues': [] } } }",
          "a".repeat(256))
        );
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @SubjectAware(permissions = {"namespace:customProperties:hitchhiker"})
    void shouldSetNamespaceConfiguration() throws URISyntaxException {
      NamespaceConfig expected = new NamespaceConfig();
      expected.setPredefinedKeys(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript"))
      ));

      JsonMockHttpRequest request = JsonMockHttpRequest
        .put(namespaceConfigPath)
        .contentType(MediaType.APPLICATION_JSON)
        .json("{ 'predefinedKeys': { 'lang': { 'validationEnabled': true, 'allowedValues': ['Java', 'TypeScript'] } } }");
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(204);
      verify(configService).setNamespaceConfig(namespace, expected);
    }
  }

  @Nested
  class GlobalConfigTest {

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
      GlobalConfig expected = new GlobalConfig();
      expected.setEnabled(true);
      expected.setEnableNamespaceConfig(false);
      expected.setPredefinedKeys(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript"))
      ));

      when(configService.getGlobalConfig()).thenReturn(expected);
      MockHttpRequest request = MockHttpRequest.get(globalConfigPath);
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(200);
      JsonNode responseBody = response.getContentAsJson();
      assertThat(responseBody.get("enabled").asBoolean()).isTrue();
      assertThat(responseBody.get("enableNamespaceConfig").asBoolean()).isFalse();

      assertThat(responseBody.get("predefinedKeys").get("lang").get("allowedValues").get(0).asText()).isEqualTo("Java");
      assertThat(responseBody.get("predefinedKeys").get("lang").get("allowedValues").get(1).asText()).isEqualTo("TypeScript");

      assertThat(responseBody.get("_links").get("update").get("href").asText()).isEqualTo(expectedLink);
      assertThat(responseBody.get("_links").get("self").get("href").asText()).isEqualTo(expectedLink);
    }

    @Test
    void shouldNotSetGlobalConfigButReturnForbiddenBecausePermissionIsMissing() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put(globalConfigPath)
        .contentType(MediaType.APPLICATION_JSON)
        .json("{ 'enabled': false, 'enableNamespaceConfig': false, 'predefinedKeys': {} }");
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(403);
    }

    @ParameterizedTest
    @ValueSource(
      strings = {
        ", 'predefinedKeys': null",
        ", 'predefinedKeys': { '': {'validationEnabled': false, 'allowedValues': [] } }",
        ", 'predefinedKeys': { 'Ungültiger/Schlüssel': {'validationEnabled': false, 'allowedValues': [] } }"
      }
    )
    void shouldReturnBadRequestBecausePredefinedKeysAreMalformed(String predefinedKeys) throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put(globalConfigPath)
        .contentType(MediaType.APPLICATION_JSON)
        .json(String.format("{ 'enabled': false, 'enableNamespaceConfig': false%s }", predefinedKeys));
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @SubjectAware(permissions = {"configuration:read,write:customProperties"})
    void shouldReturnBadRequestBecausePredefinedKeyContainsTooLongKey() throws URISyntaxException {
      JsonMockHttpRequest request = JsonMockHttpRequest
        .put(globalConfigPath)
        .contentType(MediaType.APPLICATION_JSON)
        .json(String.format(
          "{ 'enabled': false, 'enableNamespaceConfig': false, 'predefinedKeys': {'%s': {'validationEnabled': false, 'allowedValues': []}} }",
          "a".repeat(256)
        ));
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @SubjectAware(permissions = {"configuration:read,write:customProperties"})
    void shouldSetGlobalConfiguration() throws URISyntaxException {
      GlobalConfig expected = new GlobalConfig();
      expected.setEnabled(false);
      expected.setEnableNamespaceConfig(false);
      expected.setPredefinedKeys(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript"))
      ));

      JsonMockHttpRequest request = JsonMockHttpRequest
        .put(globalConfigPath)
        .contentType(MediaType.APPLICATION_JSON)
        .json("{ 'enabled': false, 'enableNamespaceConfig': false, 'predefinedKeys': {'lang': {'validationEnabled': true, 'allowedValues': ['Java', 'TypeScript']} } }");
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(204);
      verify(configService).setGlobalConfig(expected);
    }
  }
}
