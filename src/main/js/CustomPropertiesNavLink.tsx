import React from "react";

import { SecondaryNavigationItem } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";

type NavLinkProps = { url: string };

export default function CustomPropertiesNavLink({ url }: Readonly<NavLinkProps>) {
  const [t] = useTranslation("plugins");
  return (
    <SecondaryNavigationItem
      label={t("scm-custom-properties-plugin.navLink")}
      to={`${url}/properties/custom`}
      icon="fas fa-sticky-note"
    ></SecondaryNavigationItem>
  );
}
