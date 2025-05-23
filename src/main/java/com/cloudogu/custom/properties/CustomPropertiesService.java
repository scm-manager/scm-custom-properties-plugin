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

import com.google.inject.Inject;
import sonia.scm.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.AlreadyExistsException;
import sonia.scm.ContextEntry;
import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import java.util.Collection;
import java.util.Optional;

import static sonia.scm.NotFoundException.notFound;

@Slf4j
public class CustomPropertiesService {
  private final DataStoreFactory storeFactory;
  private static final String CUSTOM_PROPERTY = "Custom property "; // constant in order to avoid Sonar complaints.

  @Inject
  CustomPropertiesService(DataStoreFactory storeFactory) {
    this.storeFactory = storeFactory;
  }

  Collection<CustomProperty> get(Repository repository) {
    DataStore<CustomProperty> store = createStore(repository);
    return store.getAll().values().stream().sorted().toList();
  }

  void create(Repository repository, CustomProperty entity) {
    log.trace("Creating custom property {} on {}", entity, repository);
    DataStore<CustomProperty> store = createStore(repository);
    Optional<CustomProperty> entityInDb = store.getOptional(entity.getKey());
    if (entityInDb.isPresent()) {
      throw AlreadyExistsException.alreadyExists(ContextEntry.ContextBuilder.entity("custom-property", entity.getKey()).in(repository));
    } else {
      store.put(entity.getKey(), entity);
    }
  }

  void replace(Repository repository, CustomProperty oldEntity, CustomProperty newEntity) {
    log.trace("Updating custom property {} to {} on {}", oldEntity, newEntity, repository);
    DataStore<CustomProperty> store = createStore(repository);
    CustomProperty oldEntityInDb = store
      .getOptional(oldEntity.getKey())
      .orElseThrow(() -> notFound(
        ContextEntry.ContextBuilder.entity("custom-property", oldEntity.getKey()).in(repository)
      ));

    boolean hasKeyChanged = !oldEntity.getKey().equals(newEntity.getKey());
    if (store.getOptional(newEntity.getKey()).isPresent() && hasKeyChanged) {
      throw AlreadyExistsException.alreadyExists(ContextEntry.ContextBuilder.entity("custom-property", newEntity.getKey()).in(repository));
    }

    store.put(newEntity.getKey(), newEntity);
    if (hasKeyChanged) {
      store.remove(oldEntityInDb.getKey());
    }
  }

  void delete(Repository repository, CustomProperty entity) throws NotFoundException {
    log.trace("Deleting custom property {} on {}", entity, repository);
    DataStore<CustomProperty> store = createStore(repository);
    Optional<CustomProperty> entityInDb = store.getOptional(entity.getKey());
    if (entityInDb.isEmpty()) {
      return;
    }
    store.remove(entity.getKey());
  }

  private DataStore<CustomProperty> createStore(Repository repository) {
    return storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
  }
}
