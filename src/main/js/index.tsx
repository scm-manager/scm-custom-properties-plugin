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
import { ConfigurationBinder as cfgBinder } from "@scm-manager/ui-components";
import { Navigate } from "react-router-dom";
import { getRepositoryBaseUrl, useRepositoryFromParams } from "@scm-manager/ui-core";
import { Repository } from "@scm-manager/ui-types";
import CustomPropertiesOverview from "./overview/CustomPropertiesOverview";
import CustomPropertiesNavLink from "./overview/CustomPropertiesNavLink";
import CustomPropertyHitRenderer from "./search/CustomPropertyHitRenderer";
import MissingPropertyBanner from "./banner/MissingPropertyBanner";
import GlobalConfiguration from "./config/GlobalConfiguration";
import NamespaceConfiguration from "./config/NamespaceConfiguration";

const configLinkName = "customPropertiesConfig";
const navLinkLabel = "scm-custom-properties-plugin.navLink";

export const CustomPropertiesPredicate = ({ repository }: { repository: Repository }) => {
  return !!repository?._embedded?.customProperties;
};

binder.bind<extensionPoints.RepositoryNavigation>(
  "repository.navigation",
  CustomPropertiesNavLink,
  CustomPropertiesPredicate,
);

const CustomPropertiesOverviewRoute = () => {
  const repository = useRepositoryFromParams();
  if (!repository._embedded?.customProperties) {
    return <Navigate to={getRepositoryBaseUrl(repository)} replace />;
  }
  return <CustomPropertiesOverview repository={repository} />;
};

const CustomPropertiesRepositoryRoutes: extensionPoints.RepositoryRoute["type"] = () => [
  { path: "custom-properties", element: <CustomPropertiesOverviewRoute /> },
];
binder.bind<extensionPoints.RepositoryRoute>("repository.route", CustomPropertiesRepositoryRoutes);

binder.bind("repository.banner", MissingPropertyBanner, CustomPropertiesPredicate);

binder.bind("search.hit.indexedCustomProperty.renderer", CustomPropertyHitRenderer);

cfgBinder.bindAdminSetting("/custom-properties", navLinkLabel, configLinkName, GlobalConfiguration);
cfgBinder.bindNamespaceSetting("/custom-properties", navLinkLabel, configLinkName, NamespaceConfiguration);
