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

import com.github.legman.Subscribe;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryImportEvent;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.search.Id;
import sonia.scm.search.Index;
import sonia.scm.search.IndexLog;
import sonia.scm.search.IndexLogStore;
import sonia.scm.search.IndexTask;
import sonia.scm.search.ReindexRepositoryEvent;
import sonia.scm.search.SearchEngine;
import sonia.scm.search.SerializableIndexTask;

import java.util.Optional;

import static com.cloudogu.custom.properties.CustomPropertiesContext.MULTIPLE_CHOICE_VALUE_SEPARATOR;

@Extension
@Slf4j
public class CustomPropertiesIndexer implements ServletContextListener {

  private final SearchEngine searchEngine;

  @Inject
  public CustomPropertiesIndexer(SearchEngine searchEngine) {
    this.searchEngine = searchEngine;
  }

  private static String buildId(String key, String value) {
    return String.format("%s=%s", key, value);
  }

  private static void storeCustomProperty(Index<IndexedCustomProperty> index, Repository repository, CustomProperty customProperty) {
    String[] values = customProperty.getValue().split(MULTIPLE_CHOICE_VALUE_SEPARATOR);
    for (String value : values) {
      index.store(
        Id.of(IndexedCustomProperty.class, buildId(customProperty.getKey(), value)).and(Repository.class, repository.getId()),
        RepositoryPermissions.read(repository).asShiroString(),
        new IndexedCustomProperty(customProperty.getKey(), value)
      );
    }
  }

  private static void deleteCustomPropertyFromIndex(Index<IndexedCustomProperty> index, Repository repository, CustomProperty customProperty) {
    String[] values = customProperty.getValue().split(MULTIPLE_CHOICE_VALUE_SEPARATOR);
    for (String value : values) {
      index.delete().byId(
        Id.of(IndexedCustomProperty.class, buildId(customProperty.getKey(), value)).and(Repository.class, repository)
      );
    }
  }

  private static void indexRepository(Index<IndexedCustomProperty> index, CustomPropertiesService customPropertiesService, Repository repository) {
    for (CustomProperty customProperty : customPropertiesService.get(repository)) {
      if (!customProperty.isDefaultProperty()) {
        storeCustomProperty(index, repository, customProperty);
      }
    }
  }

  @Subscribe
  public void handleEvent(CustomPropertyCreateEvent event) {
    log.debug("Storing created custom property {} for repository {}", event.getProperty(), event.getRepository());
    handleStoring(event.getRepository(), event.getProperty());
  }

  private void handleStoring(Repository repository, CustomProperty customProperty) {
    searchEngine
      .forType(IndexedCustomProperty.class)
      .update(index -> storeCustomProperty(index, repository, customProperty));
  }

  @Subscribe
  public void handleEvent(CustomPropertyUpdateEvent event) {
    log.debug("Storing updated custom property {} for repository {}", event.getProperty(), event.getRepository());
    event.getPreviousProperty().ifPresent(
      previousProperty -> handleDeleting(event.getRepository(), previousProperty)
    );
    handleStoring(event.getRepository(), event.getProperty());
  }

  @Subscribe
  public void handleEvent(CustomPropertyDeleteEvent event) {
    log.debug("Removing deleted custom property {} for repository {}", event.getProperty(), event.getRepository());
    handleDeleting(event.getRepository(), event.getProperty());
  }

  private void handleDeleting(Repository repository, CustomProperty customProperty) {
    searchEngine
      .forType(IndexedCustomProperty.class)
      .update(
        index -> deleteCustomPropertyFromIndex(index, repository, customProperty)
      );
  }

  @Subscribe
  public void handleEvent(ReindexRepositoryEvent event) {
    log.debug("Reindexing custom properties for repository {}", event.getRepository());
    searchEngine.forType(IndexedCustomProperty.class).update(new ReindexRepositoryTask(event.getRepository()));
  }

  @Subscribe
  public void handleEvent(RepositoryImportEvent event) {
    if (!event.isFailed()) {
      log.debug("Index custom properties for imported repository {}", event.getItem());
      searchEngine.forType(IndexedCustomProperty.class).update(new IndexRepositoryTask(event.getItem()));
    }
  }

  @Override
  public void contextInitialized(ServletContextEvent event) {
    searchEngine.forType(IndexedCustomProperty.class).update(ReindexAllTask.class);
  }

  static final class IndexRepositoryTask implements SerializableIndexTask<IndexedCustomProperty> {

    private final Repository repository;
    private transient CustomPropertiesService customPropertiesService;

    IndexRepositoryTask(Repository repository) {
      this.repository = repository;
    }

    @Override
    public void update(Index<IndexedCustomProperty> index) {
      indexRepository(index, customPropertiesService, repository);
    }

    @Inject
    public void setCustomPropertiesService(CustomPropertiesService customPropertiesService) {
      this.customPropertiesService = customPropertiesService;
    }
  }

  static final class ReindexRepositoryTask implements SerializableIndexTask<IndexedCustomProperty> {

    private final Repository repository;
    private transient CustomPropertiesService customPropertiesService;

    ReindexRepositoryTask(Repository repository) {
      this.repository = repository;
    }

    @Override
    public void update(Index<IndexedCustomProperty> index) {
      index.delete().by(Repository.class, repository).execute();
      indexRepository(index, customPropertiesService, repository);
    }

    @Inject
    public void setCustomPropertiesService(CustomPropertiesService customPropertiesService) {
      this.customPropertiesService = customPropertiesService;
    }
  }

  @Slf4j
  static final class ReindexAllTask implements IndexTask<IndexedCustomProperty> {

    private final RepositoryManager repositoryManager;
    private final CustomPropertiesService customPropertiesService;
    private final IndexLogStore indexLogStore;

    @Inject
    ReindexAllTask(RepositoryManager repositoryManager, CustomPropertiesService customPropertiesService, IndexLogStore indexLogStore) {
      this.repositoryManager = repositoryManager;
      this.customPropertiesService = customPropertiesService;
      this.indexLogStore = indexLogStore;
    }

    @Override
    public void update(Index<IndexedCustomProperty> index) {
      Optional<IndexLog> indexLog = indexLogStore.defaultIndex().get(IndexedCustomProperty.class);
      if (indexLog.isEmpty() || indexLog.get().getVersion() != IndexedCustomProperty.VERSION) {
        log.debug(
          "Reindexing all custom properties, because of found index log {} and current version {}",
          indexLog,
          IndexedCustomProperty.VERSION
        );
        reindexAll(index);
      }
    }

    @Override
    public void afterUpdate() {
      indexLogStore.defaultIndex().log(IndexedCustomProperty.class, IndexedCustomProperty.VERSION);
    }

    private void reindexAll(Index<IndexedCustomProperty> index) {
      index.delete().all();
      for (Repository repository : repositoryManager.getAll()) {
        indexRepository(index, customPropertiesService, repository);
      }
    }
  }
}
