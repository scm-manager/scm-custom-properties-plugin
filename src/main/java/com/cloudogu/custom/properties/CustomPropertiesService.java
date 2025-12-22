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
import com.cloudogu.custom.properties.config.PredefinedKey;
import com.cloudogu.custom.properties.config.ValueMode;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.ContextEntry;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.store.ConfigurationEntryStoreFactory;
import sonia.scm.store.DataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static sonia.scm.AlreadyExistsException.alreadyExists;
import static sonia.scm.NotFoundException.notFound;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Slf4j
public class CustomPropertiesService {
  private final ConfigurationEntryStoreFactory storeFactory;
  private final ConfigService configService;
  private final ScmEventBus eventBus;
  private final RepositoryManager repositoryManager;

  @Inject
  CustomPropertiesService(ConfigurationEntryStoreFactory storeFactory, ConfigService configService, ScmEventBus eventBus, RepositoryManager repositoryManager) {
    this.storeFactory = storeFactory;
    this.configService = configService;
    this.eventBus = eventBus;
    this.repositoryManager = repositoryManager;
  }

  Collection<CustomProperty> get(Repository repository) {
    Map<String, PredefinedKey> predefinedKeys = configService.getAllPredefinedKeys(repository.getNamespace());
    Collection<CustomProperty> existingProperties = createStore(repository)
      .getAll()
      .values()
      .stream()
      .map(
        customProp -> new CustomProperty(customProp.getKey(), customProp.getValue(), false, isMandatoryKey(customProp.getKey(), predefinedKeys))
      )
      .toList();

    Stream<CustomProperty> defaultProperties = predefinedKeys
      .entrySet()
      .stream()
      .filter(entry -> isDefaultProperty(entry.getValue()))
      .filter(entry -> isKeyUndefined(entry.getKey(), existingProperties))
      .map(entry -> new CustomProperty(entry.getKey(), entry.getValue().getDefaultValue(), true, false));

    return Stream.concat(existingProperties.stream(), defaultProperties).sorted().toList();
  }

  private boolean isMandatoryKey(String key, Map<String, PredefinedKey> predefinedKeys) {
    return predefinedKeys.containsKey(key) && predefinedKeys.get(key).getMode() == ValueMode.MANDATORY;
  }

  private boolean isDefaultProperty(PredefinedKey predefinedKey) {
    return predefinedKey.getMode() == ValueMode.DEFAULT;
  }

  private boolean isKeyUndefined(String key, Collection<CustomProperty> existingProperties) {
    return existingProperties.stream().noneMatch(property -> property.getKey().equals(key));
  }

  Map<String, PredefinedKey> getFilteredPredefinedKeys(String namespace, String filter) {
    Map<String, PredefinedKey> predefinedKeys = configService.getAllPredefinedKeys(namespace);

    if (filter == null || filter.isEmpty()) {
      return predefinedKeys;
    }

    String lowerCasedFilter = filter.toLowerCase();
    return predefinedKeys.entrySet().stream()
      .filter(entry -> entry.getKey().toLowerCase().contains(lowerCasedFilter))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  Map<String, Collection<Repository>> getMissingMandatoryProperties() {
    return getMissingMandatoryPropertiesForRepositoryCollection(repositoryManager.getAll());
  }

  Map<String, Collection<Repository>> getMissingMandatoryPropertiesForNamespace(String namespace) {
    return getMissingMandatoryPropertiesForRepositoryCollection(
      repositoryManager.getAll(repository -> repository.getNamespace().equals(namespace))
    );
  }

  Collection<String> getMissingMandatoryPropertiesForRepository(Repository repository) {
    return getMissingMandatoryPropertiesForRepositoryCollection(List.of(repository)).keySet();
  }

  private Map<String, Collection<Repository>> getMissingMandatoryPropertiesForRepositoryCollection(Collection<Repository> allRepositories) {
    Map<String, Collection<Repository>> result = new HashMap<>();
    Map<String, Map<String, PredefinedKey>> predefinedKeysCacheForNamespace = new HashMap<>();

    allRepositories.forEach(repository -> {
      Map<String, CustomProperty> definedKeysInRepository = createStore(repository).getAll();
      predefinedKeysCacheForNamespace
        .computeIfAbsent(repository.getNamespace(), this::getMandatoryKeys)
        .keySet()
        .stream()
        .filter(s -> !definedKeysInRepository.containsKey(s))
        .forEach(missingKey -> result.computeIfAbsent(missingKey, key -> new ArrayList<>()).add(repository));
    });

    return result;
  }

  private Map<String, PredefinedKey> getMandatoryKeys(String namespace) {
    return configService.getAllPredefinedKeys(namespace)
      .entrySet()
      .stream()
      .filter(entry -> entry.getValue().getMode() == ValueMode.MANDATORY)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  void create(Repository repository, CustomProperty entity) {
    log.trace("Creating custom property {} on {}", entity, repository);
    validateValue(repository, entity);
    DataStore<CustomProperty> store = createStore(repository);
    Optional<CustomProperty> entityInDb = store.getOptional(entity.getKey());
    if (entityInDb.isPresent()) {
      throw alreadyExists(ContextEntry.ContextBuilder.entity("custom-property", entity.getKey()).in(repository));
    }

    store.put(entity.getKey(), entity);
    eventBus.post(new CustomPropertyCreateEvent(repository, entity));
  }

  private void validateValue(Repository repository, CustomProperty entity) {
    PredefinedKey predefinedKey = configService.getAllPredefinedKeys(repository.getNamespace()).get(entity.getKey());
    if (predefinedKey != null && !predefinedKey.isValueValid(entity.getValue())) {
      throw new InvalidValueException(repository, entity);
    }
  }

  void update(Repository repository, String currentKey, CustomProperty updatedEntity) {
    log.trace("Updating custom property {} to {} on {}", currentKey, updatedEntity, repository);
    validateValue(repository, updatedEntity);
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
