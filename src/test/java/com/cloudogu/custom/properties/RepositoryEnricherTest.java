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
import com.google.inject.util.Providers;
import de.otto.edison.hal.Links;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.net.URI;
import java.util.List;

import static de.otto.edison.hal.Link.link;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware("Trainer Red")
class RepositoryEnricherTest {

  private final Repository repository = RepositoryTestData.create42Puzzle();

  @Mock
  private HalEnricherContext context;
  @Mock
  private HalAppender appender;

  @Mock
  private ConfigService configService;
  @Mock
  private CustomPropertiesService customPropertiesService;
  private RepositoryEnricher enricher;

  @Captor
  private ArgumentCaptor<RepositoryEnricher.CustomPropertyCollection> captor;

  @BeforeEach
  void setUp() {
    repository.setId("1337");

    ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    scmPathInfoStore.set(() -> URI.create("https://scm-test.de/scm/api/"));
    CustomPropertyMapper mapper = new CustomPropertyMapperImpl();
    mapper.setScmPathInfoStore(Providers.of(scmPathInfoStore));

    enricher = new RepositoryEnricher(
      Providers.of(scmPathInfoStore),
      customPropertiesService,
      configService,
      mapper
    );
  }

  @Test
  @SubjectAware(permissions = {"repository:read:1337"})
  void shouldNotEnrichBecauseFeatureIsGloballyDisabled() {
    when(configService.getGlobalConfig()).thenReturn(new GlobalConfig(false));
    enricher.enrich(context, appender);
    verifyNoInteractions(appender);
  }

  @Test
  void shouldNotEnrichBecauseReadPermissionIsMissing() {
    when(configService.getGlobalConfig()).thenReturn(new GlobalConfig(true));
    when(context.oneRequireByType(Repository.class)).thenReturn(repository);
    enricher.enrich(context, appender);
    verifyNoInteractions(appender);
  }

  @Test
  @SubjectAware(permissions = {"repository:read:1337"})
  void shouldEnrichWithCustomPropertiesAsEmbeddedButWithoutLinksBecauseModifyPermissionIsMissing() {
    when(configService.getGlobalConfig()).thenReturn(new GlobalConfig(true));
    when(context.oneRequireByType(Repository.class)).thenReturn(repository);
    when(customPropertiesService.get(repository)).thenReturn(
      List.of(
        new CustomProperty("lang", "java"),
        new CustomProperty("published", "true")
      )
    );

    enricher.enrich(context, appender);

    verify(appender).appendEmbedded(eq("customProperties"), captor.capture());
    RepositoryEnricher.CustomPropertyCollection embedded = captor.getValue();
    assertThat(embedded.getProperties()).isEqualTo(List.of(
      new CustomPropertyDto("lang", "java"),
      new CustomPropertyDto("published", "true")
    ));

    Links expectedLinks = new Links.Builder()
      .self("https://scm-test.de/scm/api/v2/custom-properties/hitchhiker/42Puzzle")
      .build();
    assertThat(embedded.getLinks()).isEqualTo(expectedLinks);
  }

  @Test
  @SubjectAware(permissions = {"repository:read,modify:1337"})
  void shouldEnrichWithCustomPropertiesAsEmbeddedAndWithModificationLinks() {
    when(configService.getGlobalConfig()).thenReturn(new GlobalConfig(true));
    when(context.oneRequireByType(Repository.class)).thenReturn(repository);
    when(customPropertiesService.get(repository)).thenReturn(
      List.of(
        new CustomProperty("lang", "java"),
        new CustomProperty("published", "true")
      )
    );

    enricher.enrich(context, appender);

    CustomPropertyDto expectedLangDto = new CustomPropertyDto("lang", "java");
    expectedLangDto.add(new Links.Builder()
      .single(link("update", "https://scm-test.de/scm/api/v2/custom-properties/hitchhiker/42Puzzle/lang"))
      .single(link("delete", "https://scm-test.de/scm/api/v2/custom-properties/hitchhiker/42Puzzle/lang"))
      .build()
    );
    CustomPropertyDto expectedPublishedDto = new CustomPropertyDto("published", "true");
    expectedPublishedDto.add(new Links.Builder()
      .single(link("update", "https://scm-test.de/scm/api/v2/custom-properties/hitchhiker/42Puzzle/published"))
      .single(link("delete", "https://scm-test.de/scm/api/v2/custom-properties/hitchhiker/42Puzzle/published"))
      .build()
    );

    verify(appender).appendEmbedded(eq("customProperties"), captor.capture());
    RepositoryEnricher.CustomPropertyCollection embedded = captor.getValue();
    assertThat(embedded.getProperties()).isEqualTo(List.of(
      expectedLangDto, expectedPublishedDto
    ));

    Links expectedCollectionLinks = new Links.Builder()
      .self("https://scm-test.de/scm/api/v2/custom-properties/hitchhiker/42Puzzle")
      .single(link("create", "https://scm-test.de/scm/api/v2/custom-properties/hitchhiker/42Puzzle"))
      .build();
    assertThat(embedded.getLinks()).isEqualTo(expectedCollectionLinks);
  }
}
