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

import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryImportEvent;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.search.Id;
import sonia.scm.search.Index;
import sonia.scm.search.IndexLog;
import sonia.scm.search.IndexLogStore;
import sonia.scm.search.ReindexRepositoryEvent;
import sonia.scm.search.SearchEngine;
import sonia.scm.search.SerializableIndexTask;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomPropertiesIndexerTest {
  private final Repository repository = new Repository("1", "git", "hitchhiker", "42");

  private final CustomProperty customProperty = new CustomProperty("key", "value");
  @Mock
  Index<IndexedCustomProperty> index;
  @Mock
  Index.Deleter<IndexedCustomProperty> deleter;
  @Mock
  private SearchEngine searchEngine;
  @Mock
  private SearchEngine.ForType<IndexedCustomProperty> forType;
  @InjectMocks
  private CustomPropertiesIndexer indexer;

  @Captor
  private ArgumentCaptor<SerializableIndexTask<IndexedCustomProperty>> captor;

  @BeforeEach
  void setup() {
    lenient().when(searchEngine.forType(IndexedCustomProperty.class)).thenReturn(forType);
    lenient().when(index.delete()).thenReturn(deleter);
  }

  private Id<IndexedCustomProperty> buildIdString(String key, String value, Repository repository) {
    return Id.of(IndexedCustomProperty.class, String.format("%s=%s", key, value)).and(repository);
  }

  private void verifyIndexStore(String storedKey, String storedValue, Repository storedForRepository) {
    verify(index).store(
      buildIdString(storedKey, storedValue, storedForRepository),
      String.format("repository:read:%s", storedForRepository.getId()),
      new IndexedCustomProperty(storedKey, storedValue)
    );
  }

  private void verifyIndexDelete(String deletedKey, String deletedValue, Repository deletedForRepository) {
    verify(deleter).byId(
      buildIdString(deletedKey, deletedValue, deletedForRepository)
    );
  }

  @Test
  void shouldIndexCustomPropertyForCreateEvent() {
    CustomPropertyCreateEvent event = new CustomPropertyCreateEvent(repository, customProperty);

    indexer.handleEvent(event);

    verify(forType, times(1)).update(captor.capture());

    SerializableIndexTask<IndexedCustomProperty> task = captor.getValue();
    task.update(index);

    verifyIndexStore(customProperty.getKey(), customProperty.getValue(), repository);
  }

  @Test
  void shouldIndexCustomPropertyForCreateEventWithMultipleChoiceValue() {
    CustomProperty multipleChoiceProperty = new CustomProperty("multiple", "a\tb\tc");
    CustomPropertyCreateEvent event = new CustomPropertyCreateEvent(repository, multipleChoiceProperty);

    indexer.handleEvent(event);

    verify(forType, times(1)).update(captor.capture());

    SerializableIndexTask<IndexedCustomProperty> task = captor.getValue();
    task.update(index);

    verifyIndexStore(multipleChoiceProperty.getKey(), "a", repository);
    verifyIndexStore(multipleChoiceProperty.getKey(), "b", repository);
    verifyIndexStore(multipleChoiceProperty.getKey(), "c", repository);
  }

  @Test
  void shouldIndexCustomPropertyForUpdateEventWithDifferentValue() {
    CustomPropertyUpdateEvent event = new CustomPropertyUpdateEvent(
      repository, new CustomProperty(customProperty.getKey(), "OtherValue"), customProperty
    );

    indexer.handleEvent(event);
    verify(forType, times(2)).update(captor.capture());

    captor.getAllValues().forEach(task -> task.update(index));

    verifyIndexDelete(customProperty.getKey(), customProperty.getValue(), repository);
    verifyIndexStore(customProperty.getKey(), "OtherValue", repository);
  }

  @Test
  void shouldIndexMultipleChoicePropertyForUpdateEventWithDifferentValue() {
    CustomProperty oldProp = new CustomProperty("key", "a\tb\tc");
    CustomProperty newProp = new CustomProperty("key", "c\td\te");

    CustomPropertyUpdateEvent event = new CustomPropertyUpdateEvent(
      repository, newProp, oldProp
    );

    indexer.handleEvent(event);
    verify(forType, times(2)).update(captor.capture());

    captor.getAllValues().forEach(task -> task.update(index));

    verifyIndexDelete(oldProp.getKey(), "a", repository);
    verifyIndexDelete(oldProp.getKey(), "b", repository);
    verifyIndexDelete(oldProp.getKey(), "c", repository);

    verifyIndexStore(newProp.getKey(), "c", repository);
    verifyIndexStore(newProp.getKey(), "d", repository);
    verifyIndexStore(newProp.getKey(), "e", repository);
  }

  @Test
  void shouldIndexCustomPropertyForUpdateEventWithDifferentKey() {
    CustomPropertyUpdateEvent event = new CustomPropertyUpdateEvent(
      repository, new CustomProperty("OtherKey", customProperty.getValue()), customProperty
    );

    indexer.handleEvent(event);
    verify(forType, times(2)).update(captor.capture());

    captor.getAllValues().forEach(task -> task.update(index));

    verifyIndexDelete(customProperty.getKey(), customProperty.getValue(), repository);
    verifyIndexStore("OtherKey", customProperty.getValue(), repository);
  }

  @Test
  void shouldIndexMultipleChoicePropertyForUpdateEventWithDifferentKey() {
    CustomProperty oldProp = new CustomProperty("key", "a\tb\tc");
    CustomProperty newProp = new CustomProperty("otherKey", "a\tb\tc");

    CustomPropertyUpdateEvent event = new CustomPropertyUpdateEvent(
      repository, newProp, oldProp
    );

    indexer.handleEvent(event);
    verify(forType, times(2)).update(captor.capture());

    captor.getAllValues().forEach(task -> task.update(index));

    verifyIndexDelete(oldProp.getKey(), "a", repository);
    verifyIndexDelete(oldProp.getKey(), "b", repository);
    verifyIndexDelete(oldProp.getKey(), "c", repository);

    verifyIndexStore(newProp.getKey(), "a", repository);
    verifyIndexStore(newProp.getKey(), "b", repository);
    verifyIndexStore(newProp.getKey(), "c", repository);
  }

  @Test
  void shouldDeleteCustomPropertyForDeleteEvent() {
    CustomPropertyDeleteEvent event = new CustomPropertyDeleteEvent(repository, customProperty);

    indexer.handleEvent(event);
    verify(forType, times(1)).update(captor.capture());

    SerializableIndexTask<IndexedCustomProperty> task = captor.getValue();
    task.update(index);

    verifyIndexDelete(customProperty.getKey(), customProperty.getValue(), repository);
  }

  @Test
  void shouldDeleteMultipleChoicePropertyForDeleteEvent() {
    CustomProperty multiple = new CustomProperty("key", "a\tb\tc");
    CustomPropertyDeleteEvent event = new CustomPropertyDeleteEvent(repository, multiple);

    indexer.handleEvent(event);
    verify(forType, times(1)).update(captor.capture());

    SerializableIndexTask<IndexedCustomProperty> task = captor.getValue();
    task.update(index);

    verifyIndexDelete(customProperty.getKey(), "a", repository);
    verifyIndexDelete(customProperty.getKey(), "b", repository);
    verifyIndexDelete(customProperty.getKey(), "c", repository);
  }

  @Test
  void shouldReindexRepository() {
    indexer.handleEvent(new ReindexRepositoryEvent(repository));
    verify(forType).update(any(CustomPropertiesIndexer.ReindexRepositoryTask.class));
  }

  @Test
  void shouldReindexRepositoryUponStartup() {
    indexer.contextInitialized(mock(ServletContextEvent.class));
    verify(forType).update(CustomPropertiesIndexer.ReindexAllTask.class);
  }

  @Test
  void shouldIndexRepositoryAfterSuccessfulImport() {
    indexer.handleEvent(new RepositoryImportEvent(repository, false));
    verify(forType).update(any(CustomPropertiesIndexer.IndexRepositoryTask.class));
  }

  @Test
  void shouldNotIndexRepositoryAfterFailedImport() {
    indexer.handleEvent(new RepositoryImportEvent(repository, true));
    verifyNoInteractions(forType);
  }

  @Nested
  class IndexRepositoryTaskTest {

    @Mock
    private CustomPropertiesService customPropertiesService;

    @Test
    void shouldReindex() {
      when(customPropertiesService.get(repository)).thenReturn(List.of(customProperty));
      CustomPropertiesIndexer.IndexRepositoryTask task = new CustomPropertiesIndexer.IndexRepositoryTask(repository);
      task.setCustomPropertiesService(customPropertiesService);

      task.update(index);

      verifyIndexStore(customProperty.getKey(), customProperty.getValue(), repository);
    }
  }

  @Nested
  class ReindexRepositoryTaskTest {

    @Mock
    private CustomPropertiesService customPropertiesService;

    @Mock
    private Index.DeleteBy deleteBy;

    @Test
    void shouldReindex() {
      when(customPropertiesService.get(repository)).thenReturn(List.of(customProperty));
      CustomPropertiesIndexer.ReindexRepositoryTask task = new CustomPropertiesIndexer.ReindexRepositoryTask(repository);
      task.setCustomPropertiesService(customPropertiesService);
      when(deleter.by(Repository.class, repository)).thenReturn(deleteBy);

      task.update(index);

      verify(index.delete()).by(Repository.class, repository);
      verify(deleteBy).execute();
      verifyIndexStore(customProperty.getKey(), customProperty.getValue(), repository);
    }
  }

  @Nested
  class ReindexAllTaskTest {

    @Mock
    private RepositoryManager repositoryManager;

    @Mock
    private CustomPropertiesService customPropertiesService;

    @Mock
    private IndexLogStore indexLogStore;

    @Mock
    private IndexLogStore.ForIndex forIndex;

    @InjectMocks
    private CustomPropertiesIndexer.ReindexAllTask task;

    @BeforeEach
    void mockLogStore() {
      when(indexLogStore.defaultIndex()).thenReturn(forIndex);
    }

    @Test
    void shouldNotReindexIfVersionHasNotChanged() {
      when(forIndex.get(IndexedCustomProperty.class)).thenReturn(Optional.of(new IndexLog(IndexedCustomProperty.VERSION)));

      task.update(index);

      verify(index, never()).delete();
    }

    @Test
    void shouldReindexAllIfLogStoreIsEmpty() {
      when(forIndex.get(IndexedCustomProperty.class))
        .thenReturn(Optional.empty());
      when(repositoryManager.getAll()).thenReturn(List.of(repository));
      when(customPropertiesService.get(repository)).thenReturn(List.of(customProperty));

      task.update(index);

      verify(deleter).all();
      verifyIndexStore(customProperty.getKey(), customProperty.getValue(), repository);
    }

    @Test
    void shouldReindexAllIfVersionDiffers() {
      when(forIndex.get(IndexedCustomProperty.class))
        .thenReturn(Optional.of(new IndexLog(IndexedCustomProperty.VERSION - 1)));
      when(repositoryManager.getAll()).thenReturn(List.of(repository));
      when(customPropertiesService.get(repository)).thenReturn(List.of(customProperty));

      task.update(index);

      verify(deleter).all();
      verifyIndexStore(customProperty.getKey(), customProperty.getValue(), repository);
    }
  }
}
