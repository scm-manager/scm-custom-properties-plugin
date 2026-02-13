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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.AlreadyExistsException;
import sonia.scm.NotFoundException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.DataStore;
import sonia.scm.store.InMemoryByteConfigurationEntryStoreFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomPropertiesServiceTest {

  private final Repository repository = RepositoryTestData.createHeartOfGold();
  private final String namespace = repository.getNamespace();
  @Mock
  ScmEventBus eventBus;
  @Captor
  ArgumentCaptor<Object> eventCaptor;
  private DataStore<CustomProperty> store;
  @Mock
  private ConfigService configService;
  @Mock
  private RepositoryManager repositoryManager;
  private CustomPropertiesService customPropertiesService;

  @BeforeEach
  void initEventBus() {
    lenient().doNothing().when(eventBus).post(eventCaptor.capture());
  }

  @BeforeEach
  void setup() {
    InMemoryByteConfigurationEntryStoreFactory storeFactory = new InMemoryByteConfigurationEntryStoreFactory();
    store = storeFactory.withType(CustomProperty.class).withName("custom-properties").forRepository(repository).build();
    customPropertiesService = new CustomPropertiesService(storeFactory, configService, eventBus, repositoryManager);
  }

  @Nested
  class GetFilteredPredefinedKeysTest {

    @Test
    void shouldReturnNoKeysBecauseNoKeysWerePredefined() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of());

      Map<String, PredefinedKey> result = customPropertiesService.getFilteredPredefinedKeys(namespace, "");
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnNoKeysBecauseFilterDoesNotMatchAnyKeys() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript"))
      ));

      Map<String, PredefinedKey> result = customPropertiesService.getFilteredPredefinedKeys(namespace, "Some complete nonsense");
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnAllPredefinedKeysBecauseFilterIsEmpty() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript")),
        "arbitrary", new PredefinedKey(List.of())
      ));

      Map<String, PredefinedKey> result = customPropertiesService.getFilteredPredefinedKeys(namespace, "");
      assertThat(result).containsOnly(
        entry("lang", new PredefinedKey(List.of("Java", "TypeScript"))),
        entry("arbitrary", new PredefinedKey(List.of()))
      );
    }

    @Test
    void shouldReturnAllPredefinedKeysBecauseFilterIsNull() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript")),
        "arbitrary", new PredefinedKey(List.of())
      ));

      Map<String, PredefinedKey> result = customPropertiesService.getFilteredPredefinedKeys(namespace, null);
      assertThat(result).containsOnly(
        entry("lang", new PredefinedKey(List.of("Java", "TypeScript"))),
        entry("arbitrary", new PredefinedKey(List.of()))
      );
    }

    @Test
    void shouldReturnAllPredefinedKeysThatMatchTheFilter() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript")),
        "arbitrary", new PredefinedKey(List.of())
      ));

      Map<String, PredefinedKey> result = customPropertiesService.getFilteredPredefinedKeys(namespace, "lan");
      assertThat(result).containsOnly(
        entry("lang", new PredefinedKey(List.of("Java", "TypeScript")))
      );
    }

    @Test
    void shouldApplyFilterCaseInsensitive() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "TypeScript")),
        "LANG", new PredefinedKey(List.of())
      ));

      Map<String, PredefinedKey> result = customPropertiesService.getFilteredPredefinedKeys(namespace, "lAn");
      assertThat(result).containsOnly(
        entry("lang", new PredefinedKey(List.of("Java", "TypeScript"))),
        entry("LANG", new PredefinedKey(List.of()))
      );
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

    @Test
    void shouldGetDefaultPropertyBecauseOfPredefinedKey() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(
        Map.of(
          "lang", new PredefinedKey(List.of(), "Java")
        )
      );
      store.put("pending_release", new CustomProperty("pending_release", "true"));

      Collection<CustomProperty> properties = customPropertiesService.get(repository);

      assertThat(properties).containsExactly(
        new CustomProperty("lang", "Java", true, false), new CustomProperty("pending_release", "true")
      );
    }

    @Test
    void shouldOverrideDefaultPropertyBecauseItsAlreadyDefinedAsProperty() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(
        Map.of(
          "lang", new PredefinedKey(List.of(), "Java")
        )
      );
      store.put("lang", new CustomProperty("lang", "C++"));

      Collection<CustomProperty> properties = customPropertiesService.get(repository);

      assertThat(properties).containsExactly(
        new CustomProperty("lang", "C++")
      );
    }

    @Test
    void shouldIgnoreDefaultPropertyBecauseDefaultValueIsEmpty() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(
        Map.of(
          "lang", new PredefinedKey(List.of(), "")
        )
      );
      store.put("pending_release", new CustomProperty("pending_release", "true"));

      Collection<CustomProperty> properties = customPropertiesService.get(repository);

      assertThat(properties).containsExactly(
        new CustomProperty("pending_release", "true")
      );
    }

    @Test
    void shouldIgnoreDefaultPropertyBecauseDefaultValueIsNull() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(
        Map.of(
          "lang", new PredefinedKey(List.of(), null)
        )
      );
      store.put("pending_release", new CustomProperty("pending_release", "true"));

      Collection<CustomProperty> properties = customPropertiesService.get(repository);

      assertThat(properties).containsExactly(
        new CustomProperty("pending_release", "true")
      );
    }

    @ParameterizedTest
    @EnumSource(value = ValueMode.class, mode = EnumSource.Mode.EXCLUDE, names = {"DEFAULT"})
    void shouldIgnoreDefaultValueBecauseModeIsNotSetToDefault(ValueMode mode) {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(
        Map.of(
          "lang", new PredefinedKey(List.of(), mode, "value")
        )
      );
      store.put("pending_release", new CustomProperty("pending_release", "true"));

      Collection<CustomProperty> properties = customPropertiesService.get(repository);

      assertThat(properties).containsExactly(
        new CustomProperty("pending_release", "true")
      );
    }

    @Test
    void shouldDeclareMandatoryPropertiesAsSuch() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(
        Map.of(
          "lang", new PredefinedKey(List.of(), ValueMode.DEFAULT, "Java"),
          "timeout", new PredefinedKey(List.of(), ValueMode.NONE, ""),
          "mandatoryButNotSet", new PredefinedKey(List.of(), ValueMode.MANDATORY, ""),
          "pending_release", new PredefinedKey(List.of(), ValueMode.MANDATORY, "")
        )
      );

      store.put("pending_release", new CustomProperty("pending_release", "true"));

      Collection<CustomProperty> properties = customPropertiesService.get(repository);

      assertThat(properties).containsExactly(
        new CustomProperty("lang", "Java", true, false),
        new CustomProperty("pending_release", "true", false, true)
      );
    }
  }

  @Nested
  class GetMissingMandatoryProperties {

    @Test
    void shouldReturnEveryMissingPropertiesFromEachRepository() {
      Repository otherRepository = RepositoryTestData.create42Puzzle();
      when(repositoryManager.getAll()).thenReturn(List.of(repository, otherRepository));
      when(configService.getAllPredefinedKeys(any())).thenReturn(Map.of(
        "a", new PredefinedKey(List.of(), ValueMode.MANDATORY, ""),
        "b", new PredefinedKey(List.of(), ValueMode.MANDATORY, ""),
        "c", new PredefinedKey(List.of("1", "2"), ValueMode.MANDATORY, ""),
        "d", new PredefinedKey(List.of("3", "4"), ValueMode.MANDATORY, "")
      ));

      customPropertiesService.create(repository, new CustomProperty("a", "value"));
      customPropertiesService.create(repository, new CustomProperty("c", "1"));

      customPropertiesService.create(otherRepository, new CustomProperty("b", "value"));
      customPropertiesService.create(otherRepository, new CustomProperty("c", "1"));

      Map<String, Collection<Repository>> result = customPropertiesService.getMissingMandatoryProperties();

      assertThat(result).containsOnly(
        entry("a", List.of(otherRepository)),
        entry("b", List.of(repository)),
        entry("d", List.of(repository, otherRepository))
      );

      //getAllPredefinedKeys should be called 5 times in total.
      //Three times for `repository` because of the two custom property creation and for the check of missing mandatory properties
      //Two times for `otherRepository` because of the two custom property creation,
      //but not for the check of missing mandatory properties, because at this point the cache should be used
      verify(configService, times(5)).getAllPredefinedKeys(namespace);
      verifyNoMoreInteractions(configService);
    }
  }

  @Nested
  class GetMissingMandatoryPropertiesForNamespace {

    @Test
    void shouldReturnMissingPropertiesFromEachNamespaceRepositories() {
      Repository otherRepository = RepositoryTestData.create42Puzzle();
      otherRepository.setNamespace("Kanto");

      when(repositoryManager.getAll(any())).thenAnswer(
        invocation -> Stream.of(repository, otherRepository).filter(invocation.getArgument(0)).toList()
      );

      when(configService.getAllPredefinedKeys(otherRepository.getNamespace())).thenReturn(Map.of(
        "a", new PredefinedKey(List.of(), ValueMode.MANDATORY, ""),
        "b", new PredefinedKey(List.of(), ValueMode.MANDATORY, ""),
        "c", new PredefinedKey(List.of("1", "2"), ValueMode.MANDATORY, ""),
        "d", new PredefinedKey(List.of("3", "4"), ValueMode.MANDATORY, "")
      ));

      customPropertiesService.create(otherRepository, new CustomProperty("b", "value"));
      customPropertiesService.create(otherRepository, new CustomProperty("c", "1"));

      Map<String, Collection<Repository>> result = customPropertiesService.getMissingMandatoryPropertiesForNamespace("Kanto");

      assertThat(result).containsOnly(
        entry("a", List.of(otherRepository)),
        entry("d", List.of(otherRepository))
      );
    }
  }

  @Nested
  class GetMissingMandatoryPropertiesForRepository {

    @Test
    void shouldReturnMissingMandatoryPropertiesForRepository() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "a", new PredefinedKey(List.of(), ValueMode.MANDATORY, ""),
        "b", new PredefinedKey(List.of(), ValueMode.MANDATORY, ""),
        "c", new PredefinedKey(List.of("1", "2"), ValueMode.MANDATORY, ""),
        "d", new PredefinedKey(List.of("3", "4"), ValueMode.MANDATORY, "")
      ));

      customPropertiesService.create(repository, new CustomProperty("b", "value"));
      customPropertiesService.create(repository, new CustomProperty("c", "1"));

      Collection<String> result = customPropertiesService.getMissingMandatoryPropertiesForRepository(repository);

      assertThat(result).containsOnly("a", "d");
    }
  }

  @Nested
  class CreateCustomPropertyTest {

    private void assertCustomPropertyCreateEvent(CustomProperty newProp) {
      CustomPropertyCreateEvent event = (CustomPropertyCreateEvent) eventCaptor.getValue();
      assertThat(event).isInstanceOf(CustomPropertyCreateEvent.class);
      assertThat(event.getProperty()).isEqualTo(newProp);
      assertThat(event.getRepository()).isEqualTo(repository);
    }

    @Test
    void shouldThrowAlreadyExistsBecauseKeyIsAlreadyInUse() {
      store.put("key1", new CustomProperty("key1", "value1"));

      assertThatThrownBy(() -> customPropertiesService.create(repository, new CustomProperty("key1", "otherValue")))
        .isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    void shouldThrowBadRequestBecauseValueIsInvalid() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "key1", new PredefinedKey(List.of("allowedValue"))
      ));

      assertThatThrownBy(() -> customPropertiesService.create(repository, new CustomProperty("key1", "disallowedValue")))
        .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void shouldCreateNewCustomPropertyWithoutPredefinedKey() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of());

      customPropertiesService.create(repository, new CustomProperty("key1", "value1"));

      assertThat(store.get("key1")).isEqualTo(new CustomProperty("key1", "value1"));

      assertCustomPropertyCreateEvent(new CustomProperty("key1", "value1"));
    }

    @Test
    void shouldCreateNewCustomPropertyWithValidationDisabled() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "key1", new PredefinedKey(List.of())
      ));

      customPropertiesService.create(repository, new CustomProperty("key1", "value1"));

      assertThat(store.get("key1")).isEqualTo(new CustomProperty("key1", "value1"));

      assertCustomPropertyCreateEvent(new CustomProperty("key1", "value1"));
    }

    @Test
    void shouldCreateNewCustomPropertyWithValidatedValue() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "key1", new PredefinedKey(List.of("value1"))
      ));

      customPropertiesService.create(repository, new CustomProperty("key1", "value1"));

      assertThat(store.get("key1")).isEqualTo(new CustomProperty("key1", "value1"));

      assertCustomPropertyCreateEvent(new CustomProperty("key1", "value1"));
    }

    @Test
    void shouldAllowMultipleChoicePropertyWithMultipleValues() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "Go", "C"), ValueMode.MULTIPLE_CHOICE, "")
      ));
      CustomProperty expected = new CustomProperty("lang", "Java\tGo\tC");

      customPropertiesService.create(repository, expected);

      assertThat(store.get("lang")).isEqualTo(expected);

      assertCustomPropertyCreateEvent(expected);
    }

    @Test
    void shouldAllowMultipleChoicePropertyWithSingleValue() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "Go", "C"), ValueMode.MULTIPLE_CHOICE, "")
      ));
      CustomProperty expected = new CustomProperty("lang", "C");

      customPropertiesService.create(repository, expected);

      assertThat(store.get("lang")).isEqualTo(expected);

      assertCustomPropertyCreateEvent(expected);
    }

    @Test
    void shouldThrowForMultipleChoicePropertyWithNoValue() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "Go", "C"), ValueMode.MULTIPLE_CHOICE, "")
      ));
      CustomProperty expected = new CustomProperty("lang", "");

      assertThatThrownBy(() -> customPropertiesService.create(repository, expected))
        .isInstanceOf(InvalidValueException.class);

      verifyNoInteractions(eventBus);
    }

    @Test
    void shouldThrowForMultipleChoicePropertyWithInvalidValue() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "Go", "C"), ValueMode.MULTIPLE_CHOICE, "")
      ));
      CustomProperty expected = new CustomProperty("lang", "Rust");

      assertThatThrownBy(() -> customPropertiesService.create(repository, expected))
        .isInstanceOf(InvalidValueException.class);

      verifyNoInteractions(eventBus);
    }
  }

  @Nested
  class UpdateCustomPropertyTest {

    private void assertCustomPropertyUpdateEvent(CustomProperty newProp, CustomProperty oldProp) {
      CustomPropertyUpdateEvent event = (CustomPropertyUpdateEvent) eventCaptor.getValue();
      assertThat(event).isInstanceOf(CustomPropertyUpdateEvent.class);
      assertThat(event.getProperty()).isEqualTo(newProp);
      assertThat(event.getPreviousProperty()).isEqualTo(Optional.of(oldProp));
      assertThat(event.getRepository()).isEqualTo(repository);
    }

    @Test
    void shouldThrowNotFoundBecauseCurrentKeyIsUnknown() {
      assertThatThrownBy(() -> customPropertiesService.update(
        repository, "key1", new CustomProperty("key1", "otherValue")
      )).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldThrowBadRequestBecauseValueIsInvalid() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "key1", new PredefinedKey(List.of("allowedValue"))
      ));

      assertThatThrownBy(() -> customPropertiesService.create(repository, new CustomProperty("key1", "disallowedValue")))
        .isInstanceOf(InvalidValueException.class);
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

      assertCustomPropertyUpdateEvent(newCustomProperty, oldCustomProperty);
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

      assertCustomPropertyUpdateEvent(newCustomProperty, oldCustomProperty);
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

      assertCustomPropertyUpdateEvent(newCustomProperty, oldCustomProperty);
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

      assertCustomPropertyUpdateEvent(newCustomProperty, oldCustomProperty);
    }

    @Test
    void shouldAllowMultipleChoicePropertyWithMultipleValues() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "Go", "C"), ValueMode.MULTIPLE_CHOICE, "")
      ));
      CustomProperty oldProperty = new CustomProperty("lang", "C");
      store.put(oldProperty.getKey(), oldProperty);

      CustomProperty newProperty = new CustomProperty("lang", "Java\tGo\tC");

      customPropertiesService.update(repository, oldProperty.getKey(), newProperty);

      assertThat(store.get("lang")).isEqualTo(newProperty);

      assertCustomPropertyUpdateEvent(newProperty, oldProperty);
    }

    @Test
    void shouldAllowMultipleChoicePropertyWithSingleValue() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "Go", "C"), ValueMode.MULTIPLE_CHOICE, "")
      ));
      CustomProperty oldProperty = new CustomProperty("lang", "C");
      store.put(oldProperty.getKey(), oldProperty);

      CustomProperty newProperty = new CustomProperty("lang", "Java");

      customPropertiesService.update(repository, oldProperty.getKey(), newProperty);

      assertThat(store.get("lang")).isEqualTo(newProperty);

      assertCustomPropertyUpdateEvent(newProperty, oldProperty);
    }

    @Test
    void shouldThrowForMultipleChoicePropertyWithNoValue() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "Go", "C"), ValueMode.MULTIPLE_CHOICE, "")
      ));
      CustomProperty oldProperty = new CustomProperty("lang", "C");
      store.put(oldProperty.getKey(), oldProperty);

      CustomProperty newProperty = new CustomProperty("lang", "");

      assertThatThrownBy(() -> customPropertiesService.update(repository, oldProperty.getKey(), newProperty))
        .isInstanceOf(InvalidValueException.class);

      verifyNoInteractions(eventBus);
    }

    @Test
    void shouldThrowForMultipleChoicePropertyWithInvalidValue() {
      when(configService.getAllPredefinedKeys(namespace)).thenReturn(Map.of(
        "lang", new PredefinedKey(List.of("Java", "Go", "C"), ValueMode.MULTIPLE_CHOICE, "")
      ));
      CustomProperty oldProperty = new CustomProperty("lang", "C");
      store.put(oldProperty.getKey(), oldProperty);

      CustomProperty newProperty = new CustomProperty("lang", "Java\tRust\tC");

      assertThatThrownBy(() -> customPropertiesService.update(repository, oldProperty.getKey(), newProperty))
        .isInstanceOf(InvalidValueException.class);

      verifyNoInteractions(eventBus);
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
