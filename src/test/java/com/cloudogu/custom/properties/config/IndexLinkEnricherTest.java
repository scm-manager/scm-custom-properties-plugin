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

package com.cloudogu.custom.properties.config;

import com.google.inject.util.Providers;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;

import java.net.URI;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware("Trainer Red")
class IndexLinkEnricherTest {

  @Mock
  private HalEnricherContext context;
  @Mock
  private HalAppender appender;
  private IndexLinkEnricher enricher;

  @BeforeEach
  void setUp() {
    ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    scmPathInfoStore.set(() -> URI.create("https://scm-test.de/scm/api/"));
    enricher = new IndexLinkEnricher(Providers.of(scmPathInfoStore));
  }

  @Test
  void shouldNotAppendConfigLinkBecausePermissionIsMissing() {
    enricher.enrich(context, appender);
    verifyNoInteractions(appender);
  }

  @Test
  @SubjectAware(permissions = {"configuration:read,write:customProperties"})
  void shouldAppendConfigLink() {
    enricher.enrich(context, appender);
    verify(appender).appendLink("customPropertiesConfig", "https://scm-test.de/scm/api/v2/custom-properties/global-configuration");
    verify(appender).appendLink("missingMandatoryProperties", "https://scm-test.de/scm/api/v2/mandatory-properties/missing-properties");
  }
}
