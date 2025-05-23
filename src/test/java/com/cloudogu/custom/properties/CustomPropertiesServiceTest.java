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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sonia.scm.AlreadyExistsException;
import sonia.scm.NotFoundException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.DataStore;
import sonia.scm.store.InMemoryByteDataStoreFactory;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomPropertiesServiceTest {

  private final Repository repository = RepositoryTestData.createHeartOfGold();
  private InMemoryByteDataStoreFactory storeFactory;
  private DataStore<CustomProperty> store;
  private CustomPropertiesService customPropertiesService;

  @BeforeEach
  void setup() {
    storeFactory = new InMemoryByteDataStoreFactory();
    store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
    customPropertiesService = new CustomPropertiesService(storeFactory);
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
    }
  }

  @Nested
  class ReplaceCustomPropertyTest {

    @Test
    void shouldThrowNotFoundBecauseOldKeyIsUnknown() {
      assertThatThrownBy(() -> customPropertiesService.replace(
        repository, new CustomProperty("key1", "value1"), new CustomProperty("key1", "otherValue")
      )).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldThrowAlreadyExistsBecauseNewKeyIsAlreadyInUseByAnotherCustomPropertyThatIsNotReplaced() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      assertThatThrownBy(() -> customPropertiesService.replace(
        repository, oldCustomProperty, new CustomProperty("other", "new")
      )).isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    void shouldReplaceCustomPropertyWithSameKeyDifferentValue() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      CustomProperty newCustomProperty = new CustomProperty("old", "new");
      customPropertiesService.replace(repository, oldCustomProperty, newCustomProperty);

      assertThat(store.getAll()).hasSize(2);
      assertThat(store.get("old")).isEqualTo(newCustomProperty);
      assertThat(store.get("other")).isEqualTo(otherCustomProperty);
    }

    @Test
    void shouldReplaceCustomPropertyWithDifferentKeySameValue() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      CustomProperty newCustomProperty = new CustomProperty("new", "old");
      customPropertiesService.replace(repository, oldCustomProperty, newCustomProperty);

      assertThat(store.getAll()).hasSize(2);
      assertThat(store.get("new")).isEqualTo(newCustomProperty);
      assertThat(store.get("other")).isEqualTo(otherCustomProperty);
    }

    @Test
    void shouldReplaceCustomPropertyWithDifferentKeyDifferentValue() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      CustomProperty newCustomProperty = new CustomProperty("new", "new");
      customPropertiesService.replace(repository, oldCustomProperty, newCustomProperty);

      assertThat(store.getAll()).hasSize(2);
      assertThat(store.get("new")).isEqualTo(newCustomProperty);
      assertThat(store.get("other")).isEqualTo(otherCustomProperty);
    }

    @Test
    void shouldNotReplaceCustomPropertyBecauseKeyAndValueIsSame() {
      CustomProperty oldCustomProperty = new CustomProperty("old", "old");
      CustomProperty otherCustomProperty = new CustomProperty("other", "other");
      store.put(oldCustomProperty.getKey(), oldCustomProperty);
      store.put(otherCustomProperty.getKey(), otherCustomProperty);

      customPropertiesService.replace(repository, oldCustomProperty, oldCustomProperty);

      assertThat(store.getAll()).hasSize(2);
      assertThat(store.get("old")).isEqualTo(oldCustomProperty);
      assertThat(store.get("other")).isEqualTo(otherCustomProperty);
    }
  }

  @Nested
  class DeleteCustomPropertyTest {

    @Test
    void shouldDeleteNothingBecauseKeyIsUnknown() {
      store.put("key1", new CustomProperty("key1", "value1"));
      customPropertiesService.delete(repository, new CustomProperty("key2", "value2"));
      assertThat(store.getAll()).hasSize(1);
    }

    @Test
    void shouldDeleteExistingCustomPropertyByKey() {
      store.put("key1", new CustomProperty("key1", "value1"));
      customPropertiesService.delete(repository, new CustomProperty("key1", "value1"));
      assertThat(store.getAll()).hasSize(0);
    }
  }
}
