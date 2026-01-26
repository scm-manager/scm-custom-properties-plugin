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

import java.util.function.Predicate;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.util.GlobUtil;

import java.util.Collection;
import java.util.stream.Stream;

@Slf4j
public class CustomPropertiesSearchService {

  private final CustomPropertiesService customPropertiesService;
  private final RepositoryManager repositoryManager;

  @Inject
  CustomPropertiesSearchService(CustomPropertiesService customPropertiesService, RepositoryManager repositoryManager) {
    this.customPropertiesService = customPropertiesService;
    this.repositoryManager = repositoryManager;
  }

  Collection<RepositoryWithProps> findRepositoriesWithCustomProperties(Filter filter) {
    Stream<Repository> repoStream = repositoryManager.getAll().stream();

    if (filter.excludeArchived) {
      repoStream = repoStream.filter(this::removeArchived);
    }

    return repoStream
      .map(this::loadCustomProps)
      .filter(repositoryWithProps -> filterByProperties(repositoryWithProps, filter))
      .toList();
  }

  private boolean removeArchived(Repository repository) {
    return !repository.isArchived();
  }

  private RepositoryWithProps loadCustomProps(Repository repository) {
    return new RepositoryWithProps(repository, customPropertiesService.get(repository));
  }

  private boolean filterByProperties(RepositoryWithProps repositoryWithProps, Filter filter) {
    if (filter.hasNoCustomPropertyFilter()) {
      return true;
    }

    boolean keyMatches = !filter.hasKeyFilter();
    Predicate<CustomProperty> keyFilter = filter.hasKeyFilter() ?
      property -> GlobUtil.matches(filter.key, property.getKey()) :
      property -> false;

    boolean valueMatches = !filter.hasValueFilter();
    Predicate<CustomProperty> valueFilter = filter.hasValueFilter() ?
      property -> GlobUtil.matches(filter.value, property.getValue()) :
      property -> false;

    boolean keyValuePairMatches = !filter.hasKeyValueFilter();
    String[] keyValuePair = filter.hasKeyValueFilter() ? filter.splitKeyValuePair() : null;
    Predicate<CustomProperty> keyValuePairFilter = filter.hasKeyValueFilter() ?
      property -> GlobUtil.matches(keyValuePair[0], property.getKey()) && GlobUtil.matches(keyValuePair[1], property.getValue()) :
      property -> false;

    for (CustomProperty property : repositoryWithProps.props) {
      if (keyFilter.test(property)) {
        keyMatches = true;
      }

      if (valueFilter.test(property)) {
        valueMatches = true;
      }

      if (keyValuePairFilter.test(property)) {
        keyValuePairMatches = true;
      }
    }

    return keyMatches && valueMatches && keyValuePairMatches;
  }

  record Filter(String key, String value, String keyValuePair, boolean excludeArchived) {

    boolean hasNoCustomPropertyFilter() {
      return Strings.isNullOrEmpty(key) && Strings.isNullOrEmpty(value) && Strings.isNullOrEmpty(keyValuePair);
    }

    boolean hasKeyFilter() {
      return !Strings.isNullOrEmpty(key);
    }

    boolean hasValueFilter() {
      return !Strings.isNullOrEmpty(value);
    }

    boolean hasKeyValueFilter() {
      return !Strings.isNullOrEmpty(keyValuePair);
    }

    String[] splitKeyValuePair() {
      return keyValuePair.split("=", 2);
    }
  }

  record RepositoryWithProps(Repository repository, Collection<CustomProperty> props) {}
}
