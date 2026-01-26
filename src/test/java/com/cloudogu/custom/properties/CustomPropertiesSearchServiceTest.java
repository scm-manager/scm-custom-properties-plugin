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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith({MockitoExtension.class})
class CustomPropertiesSearchServiceTest {

  private final Repository javaRepo = RepositoryTestData.createHeartOfGold();
  private final CustomProperty javaLangProp = new CustomProperty("lang", "java");
  private final CustomProperty javaPendingReleaseProp = new CustomProperty("pending_release", "true");
  private final CustomProperty javaVersionProp = new CustomProperty("version", "1.0.0");
  private final CustomProperty javaTimeoutProp = new CustomProperty("timeout", "1000");
  private final List<CustomProperty> javaRepoProps = List.of(
    javaLangProp, javaPendingReleaseProp, javaVersionProp, javaTimeoutProp
  );
  private final Repository goRepo = RepositoryTestData.create42Puzzle();
  private final CustomProperty goLangProp = new CustomProperty("lang", "go");
  private final CustomProperty goSonarAnalysisProp = new CustomProperty("sonar_analysis", "true");
  private final CustomProperty goVersionProp = new CustomProperty("version", "2.0.0");
  private final CustomProperty goTimeoutProp = new CustomProperty("timeout", "1000");
  private final List<CustomProperty> goRepoProps = List.of(
    goLangProp, goSonarAnalysisProp, goVersionProp, goTimeoutProp
  );
  private final Repository archivedRepo = RepositoryTestData.createHappyVerticalPeopleTransporter();
  private final CustomProperty archivedLangProp = new CustomProperty("lang", "c");
  private final CustomProperty archivedDeprecatedProp = new CustomProperty("deprecated", "true");
  private final CustomProperty archivedVersionProp = new CustomProperty("version", "3.0.0");
  private final CustomProperty archivedTimeoutProp = new CustomProperty("timeout", "1000");
  private final List<CustomProperty> archivedRepoProps = List.of(
    archivedLangProp, archivedDeprecatedProp, archivedVersionProp, archivedTimeoutProp
  );
  @Mock
  private CustomPropertiesService customPropertiesService;
  @Mock
  private RepositoryManager repositoryManager;
  private CustomPropertiesSearchService searchService;

  @BeforeEach
  void setup() {
    searchService = new CustomPropertiesSearchService(customPropertiesService, repositoryManager);

    archivedRepo.setArchived(true);

    lenient().when(customPropertiesService.get(javaRepo)).thenReturn(javaRepoProps);
    lenient().when(customPropertiesService.get(goRepo)).thenReturn(goRepoProps);
    lenient().when(customPropertiesService.get(archivedRepo)).thenReturn(archivedRepoProps);

    List<Repository> allRepos = List.of(javaRepo, goRepo, archivedRepo);
    lenient().when(repositoryManager.getAll()).thenReturn(allRepos);
  }

  @Test
  void shouldHandleFiltersBeingNull() {
    Collection<CustomPropertiesSearchService.RepositoryWithProps> result = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter(null, null, null, false)
    );

    assertThat(result).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(goRepo, goRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(archivedRepo, archivedRepoProps)
    ));
  }

  @Test
  void shouldHandleFiltersBeingEmpty() {
    Collection<CustomPropertiesSearchService.RepositoryWithProps> result = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("", "", "", false)
    );

    assertThat(result).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(goRepo, goRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(archivedRepo, archivedRepoProps)
    ));
  }

  @Test
  void shouldExcludeArchivedRepositories() {
    Collection<CustomPropertiesSearchService.RepositoryWithProps> result = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("", "", "", true)
    );

    assertThat(result).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(goRepo, goRepoProps)
    ));
  }

  @Test
  void shouldMatchRepositoriesBasedOnKeys() {
    Collection<CustomPropertiesSearchService.RepositoryWithProps> firstResult = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("lang", "", "", false)
    );

    assertThat(firstResult).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(goRepo, goRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(archivedRepo, archivedRepoProps)
    ));

    Collection<CustomPropertiesSearchService.RepositoryWithProps> secondResult = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("pending_release", "", "", false)
    );
    assertThat(secondResult).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps)
    ));
  }

  @Test
  void shouldMatchRepositoriesBasedOnValues() {
    Collection<CustomPropertiesSearchService.RepositoryWithProps> firstResult = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("", "true", "", false)
    );
    assertThat(firstResult).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(goRepo, goRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(archivedRepo, archivedRepoProps)
    ));

    Collection<CustomPropertiesSearchService.RepositoryWithProps> secondResult = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("", "java", "", false)
    );
    assertThat(secondResult).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps)
    ));
  }

  @Test
  void shouldMatchRepositoriesBasedOnKeyValuePair() {
    Collection<CustomPropertiesSearchService.RepositoryWithProps> firstResult = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("", "", "timeout=1000", false)
    );
    assertThat(firstResult).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(goRepo, goRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(archivedRepo, archivedRepoProps)
    ));

    Collection<CustomPropertiesSearchService.RepositoryWithProps> secondResult = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("", "", "version=1.0.0", false)
    );
    assertThat(secondResult).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps)
    ));
  }

  @Test
  void shouldMatchRepositoriesBasedOnKeysValuesAndPairs() {
    Collection<CustomPropertiesSearchService.RepositoryWithProps> firstResult = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("timeout", "1000", "timeout=1000", false)
    );
    assertThat(firstResult).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(goRepo, goRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(archivedRepo, archivedRepoProps)
    ));

    Collection<CustomPropertiesSearchService.RepositoryWithProps> secondResult = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("pending_release", "java", "timeout=1000", false)
    );
    assertThat(secondResult).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps)
    ));
  }

  @Test
  void shouldMatchRepositoriesUsingGlobPatterns() {
    Collection<CustomPropertiesSearchService.RepositoryWithProps> firstResult = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("la*", "*1000", "l*g=*", false)
    );
    assertThat(firstResult).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(goRepo, goRepoProps),
      new CustomPropertiesSearchService.RepositoryWithProps(archivedRepo, archivedRepoProps)
    ));

    Collection<CustomPropertiesSearchService.RepositoryWithProps> secondResult = searchService.findRepositoriesWithCustomProperties(
      new CustomPropertiesSearchService.Filter("vers*ion", "java*", "*ending*re*=*", false)
    );
    assertThat(secondResult).isEqualTo(List.of(
      new CustomPropertiesSearchService.RepositoryWithProps(javaRepo, javaRepoProps)
    ));
  }
}
