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
import lombok.extern.slf4j.Slf4j;
import sonia.scm.ContextEntry;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.store.ConfigurationEntryStoreFactory;
import sonia.scm.store.DataStore;

import java.util.Collection;
import java.util.Optional;

import static sonia.scm.AlreadyExistsException.alreadyExists;
import static sonia.scm.NotFoundException.notFound;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Slf4j
public class CustomPropertiesService {
  private final ConfigurationEntryStoreFactory storeFactory;
  private final ScmEventBus eventBus;

  @Inject
  CustomPropertiesService(ConfigurationEntryStoreFactory storeFactory, ScmEventBus eventBus) {
    this.storeFactory = storeFactory;
    this.eventBus = eventBus;
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
      throw alreadyExists(ContextEntry.ContextBuilder.entity("custom-property", entity.getKey()).in(repository));
    } else {
      store.put(entity.getKey(), entity);
      eventBus.post(new CustomPropertyCreateEvent(repository, entity));
    }
  }

  void update(Repository repository, String currentKey, CustomProperty updatedEntity) {
    log.trace("Updating custom property {} to {} on {}", currentKey, updatedEntity, repository);
    boolean hasKeyChanged = !currentKey.equals(updatedEntity.getKey());
    if (hasKeyChanged) {
      replaceEntity(repository, currentKey, updatedEntity);
    } else {
      updateValue(repository, updatedEntity);
    }
  }

  private void updateValue(Repository repository, CustomProperty updatedEntity) {
    DataStore<CustomProperty> store = createStore(repository);
    CustomProperty currentEntity = store.getOptional(updatedEntity.getKey()).orElseThrow(() -> notFound(
      ContextEntry.ContextBuilder.entity("custom-property", updatedEntity.getKey()).in(repository)
    ));

    if (currentEntity.equals(updatedEntity)) {
      return;
    }

    store.put(updatedEntity.getKey(), updatedEntity);
    eventBus.post(
      new CustomPropertyUpdateEvent(
        repository,
        new CustomProperty(updatedEntity.getKey(), updatedEntity.getValue()),
        new CustomProperty(currentEntity.getKey(), currentEntity.getValue())
      )
    );
  }

  private void replaceEntity(Repository repository, String currentKey, CustomProperty updatedEntity) {
    DataStore<CustomProperty> store = createStore(repository);
    Optional<CustomProperty> outdatedEntityInDb = store.getOptional(currentKey);
    Optional<CustomProperty> alreadyUpdatedEntityInDb = store.getOptional(updatedEntity.getKey());
    if (isChangeAlreadyDone(outdatedEntityInDb, alreadyUpdatedEntityInDb, updatedEntity)) {
      return;
    }

    if (isKeyAlreadyInUse(alreadyUpdatedEntityInDb, updatedEntity)) {
      throw alreadyExists(
        ContextEntry.ContextBuilder.entity("custom-property", updatedEntity.getKey()).in(repository)
      );
    }

    store.put(updatedEntity.getKey(), updatedEntity);
    if (outdatedEntityInDb.isPresent()) {
      store.remove(currentKey);
    }

    eventBus.post(
      new CustomPropertyUpdateEvent(
        repository,
        new CustomProperty(updatedEntity.getKey(), updatedEntity.getValue()),
        outdatedEntityInDb.map(
          customProperty -> new CustomProperty(customProperty.getKey(), customProperty.getValue())
        ).orElse(null)
      )
    );
  }

  private boolean isChangeAlreadyDone(Optional<CustomProperty> currentEntityInDb, Optional<CustomProperty> updatedEntityInDb, CustomProperty updatedEntity) {
    return currentEntityInDb.isEmpty() && updatedEntityInDb.isPresent() && updatedEntityInDb.get().equals(updatedEntity);
  }

  private boolean isKeyAlreadyInUse(Optional<CustomProperty> updatedEntityInDb, CustomProperty updatedEntity) {
    return updatedEntityInDb.isPresent() && !updatedEntityInDb.get().equals(updatedEntity);
  }

  void delete(Repository repository, String key) throws NotFoundException {
    log.trace("Deleting custom property with key {} on {}", key, repository);
    DataStore<CustomProperty> store = createStore(repository);
    store.getOptional(key).ifPresent(
      customProperty -> {
        eventBus.post(new CustomPropertyDeleteEvent(repository, customProperty));
        store.remove(key);
      });
  }

  private DataStore<CustomProperty> createStore(Repository repository) {
    return storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
  }
}
