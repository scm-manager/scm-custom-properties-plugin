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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.AlreadyExistsException;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.DataStore;
import sonia.scm.store.InMemoryByteConfigurationEntryStoreFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomPropertiesServiceTest {

  @Mock
  ScmEventBus eventBus;
  @Captor
  ArgumentCaptor<Object> eventCaptor;

  private final Repository repository = RepositoryTestData.createHeartOfGold();
  private DataStore<CustomProperty> store;
  @Mock
  private ConfigService configService;
  private CustomPropertiesService customPropertiesService;

  @BeforeEach
  void initEventBus() {
    lenient().doNothing().when(eventBus).post(eventCaptor.capture());
  }

  @BeforeEach
  void setup() {
    InMemoryByteConfigurationEntryStoreFactory storeFactory = new InMemoryByteConfigurationEntryStoreFactory();
    store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
    customPropertiesService = new CustomPropertiesService(storeFactory, configService, eventBus);
  }

  @Nested
  class GetFilteredPredefinedKeysTest {

    @Test
    void shouldReturnNoKeysBecauseNoKeysWerePredefined() {
      when(configService.getAllPredefinedKeys(repository)).thenReturn(Set.of());

      Collection<String> result = customPropertiesService.getFilteredPredefinedKeys(repository, "");
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnNoKeysBecauseFilterDoesNotMatchAnyKeys() {
      when(configService.getAllPredefinedKeys(repository)).thenReturn(Set.of("lang", "java.jdbc"));

      Collection<String> result = customPropertiesService.getFilteredPredefinedKeys(repository, "Some complete nonsense");
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnAllPredefinedKeysBecauseFilterIsEmpty() {
      when(configService.getAllPredefinedKeys(repository)).thenReturn(Set.of("lang", "java.jdbc"));

      Collection<String> result = customPropertiesService.getFilteredPredefinedKeys(repository, "");
      assertThat(result).containsOnly("lang", "java.jdbc");
    }

    @Test
    void shouldReturnAllPredefinedKeysBecauseFilterIsNull() {
      when(configService.getAllPredefinedKeys(repository)).thenReturn(Set.of("lang", "java.jdbc"));

      Collection<String> result = customPropertiesService.getFilteredPredefinedKeys(repository, null);
      assertThat(result).containsOnly("lang", "java.jdbc");
    }

    @Test
    void shouldReturnAllPredefinedKeysThatMatchTheFilter() {
      when(configService.getAllPredefinedKeys(repository)).thenReturn(Set.of("lang", "java.jdbc"));

      Collection<String> result = customPropertiesService.getFilteredPredefinedKeys(repository, "lan");
      assertThat(result).containsOnly("lang");
    }

    @Test
    void shouldApplyFilterCaseInsensitive() {
      when(configService.getAllPredefinedKeys(repository)).thenReturn(Set.of("lang", "LANG"));

      Collection<String> result = customPropertiesService.getFilteredPredefinedKeys(repository, "lAn");
      assertThat(result).containsOnly("lang", "LANG");
    }
  }

  @Nested
  class GetCustomPropertyTest {

    @Test
    void shouldGetEmptyCollectionBecauseRepositoryHasNoProperties() {
      Collection<CustomProperty> properties = customPropertiesService.get(repository);
      assertThat(properties).isEmpty();
    }

    @Test
    void shouldGetAllPropertiesOfRepository() {
      store.put("key1", new CustomProperty("key1", "value1"));
      store.put("key2", new CustomProperty("key2", "value2"));

      Collection<CustomProperty> properties = customPropertiesService.get(repository);

      assertThat(properties).hasSize(2);
      assertThat(properties).containsExactly(new CustomProperty("key1", "value1"), new CustomProperty("key2", "value2"));
    }
  }

  @Nested
  class CreateCustomPropertyTest {

    @Test
    void shouldThrowAlreadyExistsBecauseKeyIsAlreadyInUse() {
      store.put("key1", new CustomProperty("key1", "value1"));

      assertThatThrownBy(() -> customPropertiesService.create(repository, new CustomProperty("key1", "otherValue")))
        .isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    void shouldCreateNewCustomProperty() {
      store.put("key2", new CustomProperty("key2", "value2"));
      customPropertiesService.create(repository, new CustomProperty("key1", "value1"));

      assertThat(store.get("key1")).isEqualTo(new CustomProperty("key1", "value1"));
      assertThat(store.get("key2")).isEqualTo(new CustomProperty("key2", "value2"));

      CustomPropertyCreateEvent event = (CustomPropertyCreateEvent) eventCaptor.getValue();
      assertThat(event).isInstanceOf(CustomPropertyCreateEvent.class);
      assertThat(event.getProperty()).isEqualTo(new CustomProperty("key1", "value1"));
      assertThat(event.getRepository()).isEqualTo(repository);
    }
  }

  @Nested
  class UpdateCustomPropertyTest {

    @Test
    void shouldThrowNotFoundBecauseCurrentKeyIsUnknown() {
      assertThatThrownBy(() -> customPropertiesService.update(
        repository, "key1", new CustomProperty("key1", "otherValue")
      )).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldThrowAlreadyExistsBecauseNewKeyIsAlreadyInUseByAnotherCustomPropertyThatIsNotReplaced() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      assertThatThrownBy(() -> customPropertiesService.update(
        repository, "old", new CustomProperty("other", "new")
      )).isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    void shouldReplaceCustomPropertyWithSameKeyDifferentValue() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      CustomProperty newCustomProperty = new CustomProperty("old", "new");
      customPropertiesService.update(repository, "old", newCustomProperty);

      assertThat(store.getAll()).hasSize(2);
      assertThat(store.get("old")).isEqualTo(newCustomProperty);
      assertThat(store.get("other")).isEqualTo(otherCustomProperty);

      CustomPropertyUpdateEvent event = (CustomPropertyUpdateEvent) eventCaptor.getValue();
      assertThat(event).isInstanceOf(CustomPropertyUpdateEvent.class);
      assertThat(event.getProperty()).isEqualTo(newCustomProperty);
      assertThat(event.getPreviousProperty()).isEqualTo(Optional.of(oldCustomProperty));
      assertThat(event.getRepository()).isEqualTo(repository);
    }

    @Test
    void shouldReplaceCustomPropertyWithDifferentKeySameValue() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      CustomProperty newCustomProperty = new CustomProperty("new", "old");
      customPropertiesService.update(repository, "old", newCustomProperty);

      assertThat(store.getAll()).hasSize(2);
      assertThat(store.get("new")).isEqualTo(newCustomProperty);
      assertThat(store.get("other")).isEqualTo(otherCustomProperty);

      CustomPropertyUpdateEvent event = (CustomPropertyUpdateEvent) eventCaptor.getValue();
      assertThat(event).isInstanceOf(CustomPropertyUpdateEvent.class);
      assertThat(event.getProperty()).isEqualTo(newCustomProperty);
      assertThat(event.getPreviousProperty()).isEqualTo(Optional.of(oldCustomProperty));
      assertThat(event.getRepository()).isEqualTo(repository);
    }

    @Test
    void shouldReplaceCustomPropertyWithDifferentKeyDifferentValue() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      CustomProperty newCustomProperty = new CustomProperty("new", "new");
      customPropertiesService.update(repository, "old", newCustomProperty);

      assertThat(store.getAll()).hasSize(2);
      assertThat(store.get("new")).isEqualTo(newCustomProperty);
      assertThat(store.get("other")).isEqualTo(otherCustomProperty);

      CustomPropertyUpdateEvent event = (CustomPropertyUpdateEvent) eventCaptor.getValue();
      assertThat(event).isInstanceOf(CustomPropertyUpdateEvent.class);
      assertThat(event.getProperty()).isEqualTo(newCustomProperty);
      assertThat(event.getPreviousProperty()).isEqualTo(Optional.of(oldCustomProperty));
      assertThat(event.getRepository()).isEqualTo(repository);
    }

    @Test
    void shouldNotReplaceCustomPropertyBecauseKeyAndValueIsSame() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      customPropertiesService.update(repository, "old", oldCustomProperty);

      assertThat(store.getAll()).hasSize(2);
      assertThat(store.get("old")).isEqualTo(oldCustomProperty);
      assertThat(store.get("other")).isEqualTo(otherCustomProperty);

      verifyNoInteractions(eventBus);
    }

    @Test
    void shouldHandleSameUpdateTwice() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      CustomProperty newCustomProperty = new CustomProperty("new", "old");
      customPropertiesService.update(repository, "old", newCustomProperty);
      customPropertiesService.update(repository, "old", newCustomProperty);

      assertThat(store.getAll()).hasSize(2);
      assertThat(store.get("new")).isEqualTo(newCustomProperty);
      assertThat(store.get("other")).isEqualTo(otherCustomProperty);

      CustomPropertyUpdateEvent event = (CustomPropertyUpdateEvent) eventCaptor.getValue();
      assertThat(event).isInstanceOf(CustomPropertyUpdateEvent.class);
      assertThat(event.getProperty()).isEqualTo(newCustomProperty);
      assertThat(event.getPreviousProperty()).isEqualTo(Optional.of(oldCustomProperty));
      assertThat(event.getRepository()).isEqualTo(repository);
    }
  }

  @Nested
  class DeleteCustomPropertyTest {

    @Test
    void shouldDeleteNothingBecauseKeyIsUnknown() {
      store.put("key1", new CustomProperty("key1", "value1"));
      customPropertiesService.delete(repository, "key2");
      assertThat(store.getAll()).hasSize(1);
    }

    @Test
    void shouldDeleteExistingCustomPropertyByKey() {
      store.put("key1", new CustomProperty("key1", "value1"));
      customPropertiesService.delete(repository, "key1");
      assertThat(store.getAll()).hasSize(0);

      CustomPropertyDeleteEvent event = (CustomPropertyDeleteEvent) eventCaptor.getValue();
      assertThat(event).isInstanceOf(CustomPropertyDeleteEvent.class);
      assertThat(event.getProperty()).isEqualTo(new CustomProperty("key1", "value1"));
      assertThat(event.getRepository()).isEqualTo(repository);
    }
  }
}
