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

import { Link, Namespace } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import { NavLink } from "@scm-manager/ui-components";
import React from "react";
import { Route } from "react-router";
import NamespaceConfiguration from "./NamespaceConfiguration";
import NamespaceConfigurationEditor from "./NamespaceConfigurationEditor";
import { binder } from "@scm-manager/ui-extensions";

const resolveNamespaceConfigLink = (namespace: string) => `/namespace/${namespace}/settings/custom-properties`;
const configLinkName = "customPropertiesConfig";

const ConfigPredicate = ({ namespace }: { namespace: Namespace }) =>
  namespace && namespace._links && namespace._links[configLinkName];

const NamespaceConfigNavLink = ({ namespace }: { namespace: Namespace }) => {
  const [t] = useTranslation("plugins");
  const url = resolveNamespaceConfigLink(namespace.namespace);

  return (
    <NavLink
      to={url}
      label={t("scm-custom-properties-plugin.navLink")}
      activeWhenMatch={(route) => !!route.location?.pathname.startsWith(url)}
    />
  );
};

const NamespaceConfigRoutes = ({ namespace }: { namespace: Namespace }) => {
  return (
    <>
      {/*@ts-expect-error will be irrelevant with react 19 upgrade */}
      <Route
        path={"/namespace/:namespace/settings/custom-properties"}
        exact
        render={() => (
          <NamespaceConfiguration link={(namespace._links[configLinkName] as Link).href} namespace={namespace} />
        )}
      />
      {/*@ts-expect-error will be irrelevant with react 19 upgrade */}
      <Route
        path={"/namespace/:namespace/settings/custom-properties/edit/:field/:selector?"}
        render={() => (
          <NamespaceConfigurationEditor link={(namespace._links[configLinkName] as Link).href} namespace={namespace} />
        )}
      />
    </>
  );
};

const bindNamespaceConfig = () => {
  binder.bind("namespace.route", NamespaceConfigRoutes, ConfigPredicate);
  binder.bind("namespace.setting", NamespaceConfigNavLink, ConfigPredicate, configLinkName);
};

export default bindNamespaceConfig;
