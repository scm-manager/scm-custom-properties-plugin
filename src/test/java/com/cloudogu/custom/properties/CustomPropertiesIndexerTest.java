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
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

  @BeforeEach
  void setup() {
    lenient().when(searchEngine.forType(IndexedCustomProperty.class)).thenReturn(forType);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldIndexCustomPropertyForCreateEvent() {
    CustomPropertyCreateEvent event = new CustomPropertyCreateEvent(repository, customProperty);

    ArgumentCaptor<SerializableIndexTask<IndexedCustomProperty>> captor =
      ArgumentCaptor.forClass(SerializableIndexTask.class);

    indexer.handleEvent(event);

    verify(forType, times(1)).update(captor.capture());

    SerializableIndexTask<IndexedCustomProperty> task = captor.getValue();
    task.update(index);

    verify(index, times(1)).store(
      argThat(id ->
        id.toString().contains(customProperty.getClass().getSimpleName()) &&
          id.toString().contains(customProperty.getKey()) &&
          id.toString().contains(repository.getId())
      ),
      anyString(),
      eq(new IndexedCustomProperty(customProperty))
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldIndexCustomPropertyForUpdateEventWithDifferentValue() {
    CustomPropertyUpdateEvent event = new CustomPropertyUpdateEvent(
      repository, customProperty, new CustomProperty(customProperty.getKey(), "OtherValue")
    );

    ArgumentCaptor<SerializableIndexTask<IndexedCustomProperty>> captor =
      ArgumentCaptor.forClass(SerializableIndexTask.class);

    indexer.handleEvent(event);
    verify(forType, times(1)).update(captor.capture());

    SerializableIndexTask<IndexedCustomProperty> task = captor.getValue();
    task.update(index);

    verify(index, times(1)).store(
      argThat(id ->
        id.toString().contains(customProperty.getClass().getSimpleName()) &&
          id.toString().contains(customProperty.getKey()) &&
          id.toString().contains(repository.getId())
      ),
      anyString(),
      eq(new IndexedCustomProperty(customProperty))
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldIndexCustomPropertyForUpdateEventWithDifferentKey() {
    CustomPropertyUpdateEvent event = new CustomPropertyUpdateEvent(
      repository, customProperty, new CustomProperty("OtherKey", customProperty.getValue())
    );
    when(index.delete()).thenReturn(deleter);
    ArgumentCaptor<SerializableIndexTask<IndexedCustomProperty>> captor =
      ArgumentCaptor.forClass(SerializableIndexTask.class);

    indexer.handleEvent(event);
    verify(forType, times(2)).update(captor.capture());

    captor.getAllValues().forEach(task -> task.update(index));

    verify(index, times(1)).store(
      argThat(id ->
        id.toString().contains(customProperty.getClass().getSimpleName()) &&
          id.toString().contains(customProperty.getKey()) &&
          id.toString().contains(repository.getId())
      ),
      anyString(),
      eq(new IndexedCustomProperty(customProperty))
    );

    verify(index, times(1)).delete();

    Id<IndexedCustomProperty> expectedId =
      Id.of(IndexedCustomProperty.class, "OtherKey")
        .and(Repository.class, repository);
    verify(deleter, times(1)).byId(expectedId);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldDeleteCustomPropertyForDeleteEvent() {
    CustomPropertyDeleteEvent event = new CustomPropertyDeleteEvent(repository, customProperty);

    ArgumentCaptor<SerializableIndexTask<IndexedCustomProperty>> captor =
      ArgumentCaptor.forClass(SerializableIndexTask.class);

    indexer.handleEvent(event);
    verify(forType, times(1)).update(captor.capture());

    when(index.delete()).thenReturn(deleter);

    SerializableIndexTask<IndexedCustomProperty> task = captor.getValue();
    task.update(index);

    verify(index, times(1)).delete();

    Id<IndexedCustomProperty> expectedId =
      Id.of(IndexedCustomProperty.class, customProperty.getKey())
        .and(Repository.class, repository);
    verify(deleter, times(1)).byId(expectedId);

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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Index<IndexedCustomProperty> index;

    @Test
    void shouldReindex() {
      when(customPropertiesService.get(repository)).thenReturn(List.of(customProperty));
      CustomPropertiesIndexer.IndexRepositoryTask task = new CustomPropertiesIndexer.IndexRepositoryTask(repository);
      task.setCustomPropertiesService(customPropertiesService);

      task.update(index);

      verify(index).store(
        Id.of(IndexedCustomProperty.class, customProperty.getKey()).and(Repository.class, repository.getId()),
        "repository:read:" + repository.getId(),
        new IndexedCustomProperty(customProperty)
      );
    }
  }

  @Nested
  class ReindexRepositoryTaskTest {

    @Mock
    private CustomPropertiesService customPropertiesService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Index<IndexedCustomProperty> index;

    @Test
    void shouldReindex() {
      when(customPropertiesService.get(repository)).thenReturn(List.of(customProperty));
      CustomPropertiesIndexer.ReindexRepositoryTask task = new CustomPropertiesIndexer.ReindexRepositoryTask(repository);
      task.setCustomPropertiesService(customPropertiesService);

      task.update(index);

      verify(index.delete()).by(Repository.class, repository);
      verify(index).store(
        Id.of(IndexedCustomProperty.class, customProperty.getKey()).and(Repository.class, repository.getId()),
        "repository:read:" + repository.getId(),
        new IndexedCustomProperty(customProperty)
      );
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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Index<IndexedCustomProperty> index;

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
      when(index.delete()).thenReturn(deleter);
      when(repositoryManager.getAll()).thenReturn(List.of(repository));
      when(customPropertiesService.get(repository)).thenReturn(List.of(customProperty));

      task.update(index);

      verify(deleter).all();
      verify(index).store(
        Id.of(IndexedCustomProperty.class, customProperty.getKey()).and(Repository.class, repository.getId()),
        "repository:read:" + repository.getId(),
        new IndexedCustomProperty(customProperty)
      );
    }

    @Test
    void shouldReindexAllIfVersionDiffers() {
      when(forIndex.get(IndexedCustomProperty.class))
        .thenReturn(Optional.of(new IndexLog(IndexedCustomProperty.VERSION - 1)));
      when(index.delete()).thenReturn(deleter);
      when(repositoryManager.getAll()).thenReturn(List.of(repository));
      when(customPropertiesService.get(repository)).thenReturn(List.of(customProperty));

      task.update(index);

      verify(deleter).all();
      verify(index).store(
        Id.of(IndexedCustomProperty.class, customProperty.getKey()).and(Repository.class, repository.getId()),
        "repository:read:" + repository.getId(),
        new IndexedCustomProperty(customProperty)
      );
    }
  }
}
