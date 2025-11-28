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
import com.cloudogu.custom.properties.config.GlobalConfig;
import com.cloudogu.custom.properties.config.PredefinedKey;
import com.cloudogu.custom.properties.config.PredefinedKeyMapperImpl;
import com.fasterxml.jackson.databind.JsonNode;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.ConfigurationEntryStoreFactory;
import sonia.scm.store.DataStore;
import sonia.scm.store.InMemoryByteConfigurationEntryStoreFactory;
import sonia.scm.store.InMemoryByteConfigurationStoreFactory;
import sonia.scm.web.JsonMockHttpResponse;
import sonia.scm.web.RestDispatcher;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
class CustomPropertiesResourceTest {

  private static final String TEST_KEY = "hello";
  private static final String TEST_KEY_VALUE = """
    { "key": "hello", "value": "world"}
    """;
  private static final String TEST_KEY_VALUE_2_SAME_KEY = """
    { "key": "hello", "value": "monde"}
    """;
  private static final String TEST_KEY_VALUE_2_DIFF_KEY = """
    { "key": "ice", "value": "cream"}
    """;
  private static final String TEST_KEY_VALUE_WITH_JSON_LITERAL = """
    { "key": "someJson", "value": "[{'key': 'lorem_ipsum', 'text': 'aöß76&$'}, {'key': 'lorem_ipsum2', 'text': 'hello'}]"}
    """;
  private static final String TEST_KEY_VALUE_INVALID_KEY = """
    { "key": "maßband", "value": "12cm"}
    """;
  private static final String TEST_KEY_VALUE_WITH_ALL_ALLOWED_KEY_CHARS = """
    { "key": "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789. _-:@/", "value": "12cm"}
    """;
  private static final String TEST_KEY_VALUE_REPLACEMENT = """
      {"key": "hallo", "value": "welt"}
    """;
  private static final String TEST_KEY_VALUE_INVALID_REPLACEMENT = """
      {"key": "this-is-extreme#", "value": "somethingelse"}
    """;
  private final CustomPropertyMapper customPropertyMapper = new CustomPropertyMapperImpl();
  private final ConfigurationEntryStoreFactory storeFactory = new InMemoryByteConfigurationEntryStoreFactory();
  @Mock
  ScmEventBus eventBus;
  private Repository repository;
  private RestDispatcher dispatcher;
  private ConfigService configService;
  @Mock
  private RepositoryManager repositoryManager;

  @BeforeEach
  void setUp() {
    repository = RepositoryTestData.createHeartOfGold("git");
    this.configService = new ConfigService(new InMemoryByteConfigurationStoreFactory());
    CustomPropertiesResource resource = new CustomPropertiesResource(
      repositoryManager,
      new CustomPropertiesService(storeFactory, configService, eventBus),
      configService,
      customPropertyMapper,
      new PredefinedKeyMapperImpl()
    );

    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(resource);

    lenient().when(repositoryManager.get(eq(repository.getNamespaceAndName()))).thenReturn(repository);
  }

  @Nested
  class ReadPredefinedKeys {

    @Test
    @SubjectAware(value = "cannotRead")
    void shouldReturnUnauthorizedForLackingReadPermissions() throws URISyntaxException {

      String uri = format("/v2/custom-properties/%s/%s/predefined-keys", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.get(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }

    @Test
    @SubjectAware(value = "cannotRead")
    void shouldReturnNotFoundForNonexistingRepository() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s/predefined-keys", "bogus", "repo");
      MockHttpRequest request = MockHttpRequest.get(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasReadPermissions", permissions = "repository:read:*")
    void shouldReturnCollectionWithoutFilter() throws URISyntaxException {
      GlobalConfig globalConfig = new GlobalConfig();
      globalConfig.setPredefinedKeys(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript"))
      ));
      configService.setGlobalConfig(globalConfig);

      String uri = format("/v2/custom-properties/%s/%s/predefined-keys", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.get(uri);
      JsonMockHttpResponse response = new JsonMockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());

      JsonNode responseBody = response.getContentAsJson();
      assertThat(responseBody.get("lang").get("allowedValues").get(0).asText()).isEqualTo("Java");
      assertThat(responseBody.get("lang").get("allowedValues").get(1).asText()).isEqualTo("TypeScript");
    }

    @Test
    @SubjectAware(value = "hasReadPermissions", permissions = "repository:read:*")
    void shouldReturnCollectionWithFilter() throws URISyntaxException {
      GlobalConfig globalConfig = new GlobalConfig();
      globalConfig.setPredefinedKeys(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript")),
        "arbitrary", new PredefinedKey(List.of())
      ));
      configService.setGlobalConfig(globalConfig);

      String uri = format("/v2/custom-properties/%s/%s/predefined-keys?filter=l", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.get(uri);
      JsonMockHttpResponse response = new JsonMockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());

      JsonNode responseBody = response.getContentAsJson();
      assertThat(responseBody.get("lang").get("allowedValues").get(0).asText()).isEqualTo("Java");
      assertThat(responseBody.get("lang").get("allowedValues").get(1).asText()).isEqualTo("TypeScript");
    }

    @Test
    @SubjectAware(value = "hasReadPermissions", permissions = "repository:read:*")
    void shouldReturnCollectionWithEmptyFilter() throws URISyntaxException {
      GlobalConfig globalConfig = new GlobalConfig();
      globalConfig.setPredefinedKeys(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript"))
      ));
      configService.setGlobalConfig(globalConfig);

      String uri = format("/v2/custom-properties/%s/%s/predefined-keys?filter=", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.get(uri);
      JsonMockHttpResponse response = new JsonMockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());

      JsonNode responseBody = response.getContentAsJson();
      assertThat(responseBody.get("lang").get("allowedValues").get(0).asText()).isEqualTo("Java");
      assertThat(responseBody.get("lang").get("allowedValues").get(1).asText()).isEqualTo("TypeScript");
    }
  }

  @Nested
  class Read {

    @Test
    @SubjectAware(value = "cannotRead")
    void shouldReturnUnauthorizedForLackingReadPermissions() throws URISyntaxException {

      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.get(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }

    @Test
    @SubjectAware(value = "cannotRead")
    void shouldReturnNotFoundForNonexistingRepository() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s", "bogus", "repo");
      MockHttpRequest request = MockHttpRequest.get(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasReadPermissions", permissions = "repository:read:*")
    void shouldReturnEmptyDtoForEmptyRepository() throws URISyntaxException, UnsupportedEncodingException {

      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.get(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
      assertThat(response.getContentAsString()).isEqualTo("[]");
    }

    @Test
    @SubjectAware(value = "hasReadPermissions", permissions = "repository:read:*")
    void shouldReturnSingleKeyValueEntry() throws URISyntaxException, UnsupportedEncodingException {
      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      store.put(new CustomProperty("hello", "world"));

      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.get(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
      assertThat(response.getContentAsString()).isEqualTo("""
        [{"key":"hello","value":"world","defaultProperty":false}]""");
    }

    @Test
    @SubjectAware(value = "hasReadPermissions", permissions = "repository:read:*")
    void shouldReturnSingleKeyValueEntryFromDefaultProperty() throws URISyntaxException, UnsupportedEncodingException {
      GlobalConfig globalConfig = new GlobalConfig();
      globalConfig.setPredefinedKeys(Map.of(
        "hello", new PredefinedKey(List.of(), "world")
      ));
      configService.setGlobalConfig(globalConfig);

      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.get(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
      assertThat(response.getContentAsString()).isEqualTo("""
        [{"key":"hello","value":"world","defaultProperty":true}]""");
    }

    @Test
    @SubjectAware(value = "hasReadPermissions", permissions = "repository:read:*")
    void shouldReturnForbiddenIfPluginIsDisabled() throws URISyntaxException {
      GlobalConfig disabledPlugin = new GlobalConfig();
      disabledPlugin.setEnabled(false);

      configService.setGlobalConfig(disabledPlugin);
      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.get(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }
  }

  @Nested
  class Create {

    @Test
    @SubjectAware(value = "hasReadPermissionsOnly", permissions = "repository:read:*")
    void shouldReturnUnauthorizedForLackingWritePermissions() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.post(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissionsOnly", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldReturnNotFoundForNonExistingRepository() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s", "bogus", "repo");
      MockHttpRequest request = MockHttpRequest.post(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldWriteKeyValue() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.post(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());

      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      Map<String, CustomProperty> values = store.getAll();

      assertThat(values).hasSize(1);
      assertThat(values.get("hello").getValue()).isEqualTo("world");
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldReturnConflictIfKeyValueAlreadyExists() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.post(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      MockHttpRequest request2 = MockHttpRequest.post(uri);
      request2.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request2.content(TEST_KEY_VALUE.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response2 = new MockHttpResponse();
      dispatcher.invoke(request2, response2);

      assertThat(response2.getStatus()).isEqualTo(CONFLICT.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldReturnConflictIfKeyAlreadyExists() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.post(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      MockHttpRequest request2 = MockHttpRequest.post(uri);
      request2.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request2.content(TEST_KEY_VALUE_2_SAME_KEY.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response2 = new MockHttpResponse();
      dispatcher.invoke(request2, response2);

      assertThat(response2.getStatus()).isEqualTo(CONFLICT.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldWriteTwoKeyValues() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.post(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      MockHttpRequest request2 = MockHttpRequest.post(uri);
      request2.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request2.content(TEST_KEY_VALUE_2_DIFF_KEY.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response2 = new MockHttpResponse();

      dispatcher.invoke(request2, response2);

      assertThat(response2.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());

      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      Map<String, CustomProperty> values = store.getAll();

      assertThat(values).hasSize(2);
      assertThat(values.get("hello").getValue()).isEqualTo("world");
      assertThat(values.get("ice").getValue()).isEqualTo("cream");
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldWriteValueAndPertainJSONFormat() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.post(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_WITH_JSON_LITERAL.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());

      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      Map<String, CustomProperty> values = store.getAll();

      assertThat(values).hasSize(1);
      assertThat(values.get("someJson").getValue()).isEqualTo("""
        [{'key': 'lorem_ipsum', 'text': 'aöß76&$'}, {'key': 'lorem_ipsum2', 'text': 'hello'}]""");
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldRespondBadRequestForInvalidKey() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.post(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_INVALID_KEY.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());

      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-property").forRepository(repository).build();
      Map<String, CustomProperty> values = store.getAll();

      assertThat(values).isEmpty();
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldSupportAllKeyChars() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.post(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_WITH_ALL_ALLOWED_KEY_CHARS.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(204);

      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      Map<String, CustomProperty> values = store.getAll();

      assertThat(values).hasSize(1);
      assertThat(values.get("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789. _-:@/")).isNotNull();
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldReturnForbiddenIfPluginIsDisabled() throws URISyntaxException {
      GlobalConfig disabledPlugin = new GlobalConfig();
      disabledPlugin.setEnabled(false);
      configService.setGlobalConfig(disabledPlugin);

      String uri = format("/v2/custom-properties/%s/%s", repository.getNamespace(), repository.getName());
      MockHttpRequest request = MockHttpRequest.post(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_WITH_ALL_ALLOWED_KEY_CHARS.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }
  }

  @Nested
  class Update {

    @Test
    @SubjectAware(value = "hasReadPermissionsOnly", permissions = "repository:read:*")
    void shouldReturnUnauthorizedForLackingWritePermissions() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), "hello");
      MockHttpRequest request = MockHttpRequest.put(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_REPLACEMENT.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldRespondNotFoundIfOldEntityDoesntExist() throws URISyntaxException {
      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();

      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), "hallo");
      MockHttpRequest request = MockHttpRequest.put(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_REPLACEMENT.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());

      Map<String, CustomProperty> result = store.getAll();

      assertThat(result).isEmpty();
    }

    @Test
    @SubjectAware(value = "cannotRead")
    void shouldReturnNotFoundForNonExistingRepository() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s/%s", "bogus", "repo", "hello");
      MockHttpRequest request = MockHttpRequest.put(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_REPLACEMENT.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldRespondConflictIfNewEntityAlreadyExists() throws URISyntaxException {
      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      store.put("hello", new CustomProperty("hello", "world"));
      store.put("hallo", new CustomProperty("hallo", "other"));

      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), "hello");
      MockHttpRequest request = MockHttpRequest.put(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_REPLACEMENT.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(CONFLICT.getStatusCode());

      Map<String, CustomProperty> result = store.getAll();

      assertThat(result).hasSize(2);
      assertThat(result.get("hallo").getValue()).isEqualTo("other");
      assertThat(result.get("hello").getValue()).isEqualTo("world");
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldRespondBadRequestIfNewEntityKeyIsInvalid() throws URISyntaxException {
      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      store.put("hello", new CustomProperty("hello", "world"));

      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), "hello");
      MockHttpRequest request = MockHttpRequest.put(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_INVALID_REPLACEMENT.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());

      Map<String, CustomProperty> result = store.getAll();

      assertThat(result).hasSize(1);
      assertThat(result.get("hello").getValue()).isEqualTo("world");
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldReplaceExistingKeyValueEntryWithDifferentKeyAndValue() throws URISyntaxException {
      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      store.put("hello", new CustomProperty("hello", "world"));

      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), "hello");
      MockHttpRequest request = MockHttpRequest.put(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_REPLACEMENT.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());

      Map<String, CustomProperty> result = store.getAll();

      assertThat(result).hasSize(1);
      assertThat(result.get("hallo").getValue()).isEqualTo("welt");
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldSupportAllKeys() throws URISyntaxException {
      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      store.put("hello", new CustomProperty("hello", "world"));

      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), "hello");
      MockHttpRequest request = MockHttpRequest.put(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_WITH_ALL_ALLOWED_KEY_CHARS.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());

      Map<String, CustomProperty> result = store.getAll();

      assertThat(result).hasSize(1);
      assertThat(result.get("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789. _-:@/").getValue()).isEqualTo("12cm");
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldReturnForbiddenIfPluginIsDisabled() throws URISyntaxException {
      GlobalConfig disabledPlugin = new GlobalConfig();
      disabledPlugin.setEnabled(false);
      configService.setGlobalConfig(disabledPlugin);

      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), "hello");
      MockHttpRequest request = MockHttpRequest.put(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_REPLACEMENT.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldReturnConflictIfKeyAlreadyExistsForNewProperty() throws URISyntaxException {
      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      store.put("hello", new CustomProperty("hello", "world"));
      store.put("hallo", new CustomProperty("hallo", "world"));
      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), "hello");

      MockHttpRequest request = MockHttpRequest.put(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE_REPLACEMENT.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(CONFLICT.getStatusCode());
    }
  }

  @Nested
  class Delete {

    @Test
    @SubjectAware(value = "hasReadPermissionsOnly", permissions = "repository:read:*")
    void shouldReturnUnauthorizedForLackingWritePermissions() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), TEST_KEY);
      MockHttpRequest request = MockHttpRequest.delete(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldDoNothingIfEntityDoesntExist() throws URISyntaxException {
      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), TEST_KEY);
      MockHttpRequest request = MockHttpRequest.delete(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());
    }

    @Test
    @SubjectAware(value = "cannotRead")
    void shouldReturnNotFoundForNonExistingRepository() throws URISyntaxException {

      String uri = format("/v2/custom-properties/%s/%s/%s", "bogus", "repo", TEST_KEY);
      MockHttpRequest request = MockHttpRequest.delete(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldDeletePreviouslyExistingEntity() throws URISyntaxException {
      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      store.put("hello", new CustomProperty("hello", "world"));

      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), TEST_KEY);
      MockHttpRequest request = MockHttpRequest.delete(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());

      Map<String, CustomProperty> result = store.getAll();

      assertThat(result).isEmpty();
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldKeepOtherEntities() throws URISyntaxException {
      DataStore<CustomProperty> store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
      store.put("hello", new CustomProperty("hello", "world"));
      store.put("lorem", new CustomProperty("lorem", "world"));

      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), TEST_KEY);
      MockHttpRequest request = MockHttpRequest.delete(uri);
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());

      Map<String, CustomProperty> result = store.getAll();

      assertThat(result).hasSize(1);
      assertThat(result.get("lorem").getValue()).isEqualTo("world");
    }

    @Test
    @SubjectAware(value = "hasModifyAndReadPermissions", permissions = {"repository:modify:*", "repository:read:*"})
    void shouldReturnForbiddenIfPluginIsDisabled() throws URISyntaxException {
      GlobalConfig disabledPlugin = new GlobalConfig();
      disabledPlugin.setEnabled(false);
      configService.setGlobalConfig(disabledPlugin);

      String uri = format("/v2/custom-properties/%s/%s/%s", repository.getNamespace(), repository.getName(), TEST_KEY);
      MockHttpRequest request = MockHttpRequest.delete(uri);
      request.contentType(CustomPropertiesResource.PROPERTY_KEY_VALUE_MEDIA_TYPE);
      request.content(TEST_KEY_VALUE.getBytes(StandardCharsets.UTF_8));
      MockHttpResponse response = new MockHttpResponse();

      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
    }
  }
}
