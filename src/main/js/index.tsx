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

import React from "react";
import { binder, extensionPoints } from "@scm-manager/ui-extensions";
import { Route } from "react-router";
import { Repository } from "@scm-manager/ui-types";
import CustomPropertiesOverview from "./overview/CustomPropertiesOverview";
import CustomPropertiesNavLink from "./overview/CustomPropertiesNavLink";
import CustomPropertiesEditor from "./overview/CustomPropertiesEditor";
import CustomPropertyHitRenderer from "./search/CustomPropertyHitRenderer";
import bindGlobalConfig from "./config/GlobalConfigRouteSetup";
import bindNamespaceConfig from "./config/NamespaceConfigRouteSetup";
import MissingPropertyBanner from "./banner/MissingPropertyBanner";

const CustomPropertiesPredicate = ({ repository }: { repository: Repository }) => {
  return repository._embedded?.customProperties;
};

binder.bind<extensionPoints.RepositoryNavigation>(
  "repository.navigation",
  CustomPropertiesNavLink,
  CustomPropertiesPredicate,
);

const CustomPropertiesRoute = ({ url, repository }: { url: string; repository: Repository }) => {
  return (
    //@ts-expect-error will be irrelevant with react 19 upgrade
    <Route
      path={`${url}/custom-properties`}
      exact
      render={() => {
        return <CustomPropertiesOverview repository={repository} />;
      }}
    />
  );
};
binder.bind("repository.route", CustomPropertiesRoute, CustomPropertiesPredicate);

const CustomPropertiesEditorRoute = ({ url, repository }: { url: string; repository: Repository }) => {
  return (
    //@ts-expect-error will be irrelevant with react 19 upgrade
    <Route
      path={`${url}/custom-properties/modify`}
      exact
      render={() => <CustomPropertiesEditor repository={repository} />}
    />
  );
};
binder.bind("repository.route", CustomPropertiesEditorRoute, CustomPropertiesPredicate);

binder.bind("repository.banner", MissingPropertyBanner);

binder.bind("search.hit.indexedCustomProperty.renderer", CustomPropertyHitRenderer);

bindGlobalConfig();
bindNamespaceConfig();
