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

import java.util.Locale;
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
    Predicate<CustomProperty> keyFilter = filter.getKeyFilter();

    boolean valueMatches = !filter.hasValueFilter();
    Predicate<CustomProperty> valueFilter = filter.getValueFilter();

    boolean keyValuePairMatches = !filter.hasKeyValueFilter();
    Predicate<CustomProperty> keyValuePairFilter = filter.getKeyValueFilter();

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

    Predicate<CustomProperty> getKeyFilter() {
      if (hasKeyFilter()) {
        String loweredKey = key.toLowerCase(Locale.ENGLISH);
        return property -> GlobUtil.matches(loweredKey, property.loweredKey());
      }
      return property -> false;
    }

    boolean hasValueFilter() {
      return !Strings.isNullOrEmpty(value);
    }

    Predicate<CustomProperty> getValueFilter() {
      if (hasValueFilter()) {
        String loweredValue = value.toLowerCase(Locale.ENGLISH);
        return property -> GlobUtil.matches(loweredValue, property.loweredValue());
      }
      return property -> false;
    }

    boolean hasKeyValueFilter() {
      return !Strings.isNullOrEmpty(keyValuePair);
    }

    Predicate<CustomProperty> getKeyValueFilter() {
      if (hasKeyValueFilter()) {
        String[] loweredKeyValue = keyValuePair.toLowerCase(Locale.ENGLISH).split("=", 2);
        return property -> GlobUtil.matches(loweredKeyValue[0], property.loweredKey()) &&
          GlobUtil.matches(loweredKeyValue[1], property.loweredValue());
      }
      return property -> false;
    }
  }

  record RepositoryWithProps(Repository repository, Collection<CustomProperty> props) {}
}
