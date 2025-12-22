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

import { Link, Links } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import { NavLink } from "@scm-manager/ui-components";
import { Route } from "react-router";
import GlobalConfiguration from "./GlobalConfiguration";
import GlobalConfigurationEditor from "./GlobalConfigurationEditor";
import { binder } from "@scm-manager/ui-extensions";
import React from "react";
import GlobalMissingProperties from "./GlobalMissingProperties";

const globalConfigLink = "/admin/settings/custom-properties";
const configLinkName = "customPropertiesConfig";

const ConfigPredicate = ({ links }: { links: Links }) => links[configLinkName];

const GlobalConfigNavLink = () => {
  const [t] = useTranslation("plugins");
  return (
    <NavLink
      to={globalConfigLink}
      label={t("scm-custom-properties-plugin.navLink")}
      activeWhenMatch={(route) => !!route.location?.pathname.startsWith(globalConfigLink)}
    />
  );
};

const GlobalConfigRoutes = ({ links }: { links: Links }) => {
  return (
    <>
      {/*@ts-expect-error will be irrelevant with react 19 upgrade */}
      <Route
        path={globalConfigLink}
        exact
        render={() => <GlobalConfiguration link={(links[configLinkName] as Link).href} />}
      />
      {/*@ts-expect-error will be irrelevant with react 19 upgrade */}
      <Route
        path={`${globalConfigLink}/edit/:field/:selector?`}
        render={() => <GlobalConfigurationEditor link={(links[configLinkName] as Link).href} />}
      />
      {/*@ts-expect-error will be irrelevant with react 19 upgrade */}
      <Route
        path={`${globalConfigLink}/missing-mandatory-properties/:propertyKey`}
        render={() => <GlobalMissingProperties />}
      />
    </>
  );
};

const bindGlobalConfig = () => {
  binder.bind("admin.route", GlobalConfigRoutes, ConfigPredicate);
  binder.bind("admin.setting", GlobalConfigNavLink, ConfigPredicate, configLinkName);
};

export default bindGlobalConfig;
