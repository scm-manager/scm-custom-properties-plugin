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
import sonia.scm.repository.Repository;
import sonia.scm.web.JsonMockHttpResponse;
import sonia.scm.web.RestDispatcher;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware("Trainer Red")
class MandatoryPropertiesResourceTest {

  private final Repository viridianCity = new Repository("1", "git", "Kanto", "Viridian_City");
  private final Repository pewterCity = new Repository("1", "git", "Kanto", "Pewter_City");

  @Mock
  private CustomPropertiesService customPropertiesService;
  private RestDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    MandatoryPropertiesResource resource = new MandatoryPropertiesResource(customPropertiesService);
    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(resource);
  }

  @Nested
  class ReadMissingMandatoryPropertiesTest {

    private final String apiPath = "/v2/mandatory-properties/missing-properties";

    @Test
    void shouldReturnForbiddenBecausePermissionIsMissing() throws URISyntaxException {
      MockHttpRequest request = MockHttpRequest.get(apiPath);
      MockHttpResponse response = new MockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @SubjectAware(permissions = {"configuration:read,write:customProperties"})
    void shouldReturnAllMissingMandatoryProperties() throws URISyntaxException {
      when(customPropertiesService.getMissingMandatoryProperties()).thenReturn(Map.of(
        "gym", List.of(viridianCity, pewterCity)
      ));

      MockHttpRequest request = MockHttpRequest.get(apiPath);
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(200);
      JsonNode responseBody = response.getContentAsJson();

      assertThat(responseBody.get("gym").isArray()).isTrue();
      assertThat(responseBody.get("gym").get(0).asText()).isEqualTo("Kanto/Viridian_City");
      assertThat(responseBody.get("gym").get(1).asText()).isEqualTo("Kanto/Pewter_City");
    }
  }

  @Nested
  class ReadMissingMandatoryPropertiesFromNamespaceTest {
    private final String apiPath = "/v2/mandatory-properties/missing-properties/Kanto";

    @Test
    void shouldReturnForbiddenBecausePermissionIsMissing() throws URISyntaxException {
      MockHttpRequest request = MockHttpRequest.get(apiPath);
      MockHttpResponse response = new MockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @SubjectAware(permissions = {"namespace:customProperties:Kanto"})
    void shouldReturnAllMissingMandatoryProperties() throws URISyntaxException {
      when(customPropertiesService.getMissingMandatoryPropertiesForNamespace("Kanto")).thenReturn(Map.of(
        "gym", List.of(viridianCity, pewterCity)
      ));

      MockHttpRequest request = MockHttpRequest.get(apiPath);
      JsonMockHttpResponse response = new JsonMockHttpResponse();
      dispatcher.invoke(request, response);

      assertThat(response.getStatus()).isEqualTo(200);
      JsonNode responseBody = response.getContentAsJson();

      assertThat(responseBody.get("gym").isArray()).isTrue();
      assertThat(responseBody.get("gym").get(0).asText()).isEqualTo("Kanto/Viridian_City");
      assertThat(responseBody.get("gym").get(1).asText()).isEqualTo("Kanto/Pewter_City");
    }
  }
}
