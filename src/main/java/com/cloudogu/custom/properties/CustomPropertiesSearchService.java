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

import com.google.common.base.Strings;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.util.GlobUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.cloudogu.custom.properties.CustomPropertiesContext.MULTIPLE_CHOICE_VALUE_SEPARATOR;

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

    Stream<RepositoryWithProps> repoWithPropsStream = repoStream.map(this::loadCustomProps);

    if (filter.hasNoCustomPropertyFilter()) {
      return repoWithPropsStream.toList();
    }

    Predicate<CustomProperty> keyFilter = filter.getKeyFilter();
    Predicate<CustomProperty> valueFilter = filter.getValueFilter();
    Predicate<CustomProperty> keyValuePairFilter = filter.getKeyValueFilter();

    return repoWithPropsStream
      .filter(repositoryWithProps -> filterByProperties(repositoryWithProps, keyFilter, valueFilter, keyValuePairFilter))
      .toList();
  }

  private boolean removeArchived(Repository repository) {
    return !repository.isArchived();
  }

  private RepositoryWithProps loadCustomProps(Repository repository) {
    return new RepositoryWithProps(repository, customPropertiesService.get(repository));
  }

  private boolean filterByProperties(RepositoryWithProps repositoryWithProps,
                                     Predicate<CustomProperty> keyFilter,
                                     Predicate<CustomProperty> valueFilter,
                                     Predicate<CustomProperty> keyValuePairFilter) {
    boolean keyMatches = false;
    boolean valueMatches = false;
    boolean keyValuePairMatches = false;

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
      return hasNoKeyFilter() && hasNoValueFilter() && hasNoKeyValueFilter();
    }

    boolean hasNoKeyFilter() {
      return Strings.isNullOrEmpty(key);
    }

    Predicate<CustomProperty> getKeyFilter() {
      if (hasNoKeyFilter()) {
        return property -> true;
      }

      String loweredKey = key.toLowerCase(Locale.ENGLISH);
      return property -> GlobUtil.matches(loweredKey, property.loweredKey());
    }

    boolean hasNoValueFilter() {
      return Strings.isNullOrEmpty(value);
    }

    Predicate<CustomProperty> getValueFilter() {
      if (hasNoValueFilter()) {
        return property -> true;
      }

      return buildValueFilter(value);
    }

    private Predicate<CustomProperty> buildValueFilter(String filterValue) {
      String[] filterValues = filterValue.toLowerCase(Locale.ENGLISH).split(MULTIPLE_CHOICE_VALUE_SEPARATOR);

      return property -> {
        Predicate<String> matchPropValue;
        if (property.getValue().contains(MULTIPLE_CHOICE_VALUE_SEPARATOR)) {
          String[] values = property.loweredValue().split(MULTIPLE_CHOICE_VALUE_SEPARATOR);
          matchPropValue = filter -> Arrays.stream(values).anyMatch(value -> GlobUtil.matches(filter, value));
        } else {
          String loweredValue = property.loweredValue();
          matchPropValue = filter -> GlobUtil.matches(filter, loweredValue);
        }

        return Arrays.stream(filterValues).allMatch(matchPropValue);
      };
    }

    boolean hasNoKeyValueFilter() {
      return Strings.isNullOrEmpty(keyValuePair);
    }

    Predicate<CustomProperty> getKeyValueFilter() {
      if (hasNoKeyValueFilter()) {
        return property -> true;
      }

      String[] loweredKeyValue = keyValuePair.split("=", 2);
      String filterKey = loweredKeyValue[0].toLowerCase(Locale.ENGLISH);
      Predicate<CustomProperty> filterValue = buildValueFilter(loweredKeyValue[1]);

      return property -> GlobUtil.matches(filterKey, property.loweredKey()) && filterValue.test(property);
    }
  }

  record RepositoryWithProps(Repository repository, Collection<CustomProperty> props) {
  }
}
