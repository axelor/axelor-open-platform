import React, { ComponentProps, useMemo } from "react";

import { ThemeContextValue } from "@axelor/ui";

import { APPLICATION_NAME, useAppSettings } from "@/hooks/use-app-settings";

import defaultLogoDark from "@/assets/axelor-dark.svg";
import defaultIcon from "@/assets/axelor-icon.svg";
import defaultLogo from "@/assets/axelor.svg";

type AppLogoProps = Readonly<{
  className?: ComponentProps<"img">["className"];
  type?: "logo" | "sign-in/logo" | "icon";
}>;

export const AppLogo = React.memo(function ({
  type = "logo",
  className,
}: AppLogoProps) {
  const { name, themeMode } = useAppSettings();

  const imgSrc = useMemo(() => {
    const params = themeMode !== "light" ? `?mode=${themeMode}` : "";
    return `ws/public/app/${type}${params}`;
  }, [type, themeMode]);

  return (
    <img
      className={className}
      src={imgSrc}
      alt={name}
      onError={(e) => {
        const defaultImgSrc =
          type === "icon" ? defaultIcon : getDefaultAppLogo(themeMode);
        // Prevent infinite loop in case default image also fails.
        if (!e.currentTarget.src.includes(defaultImgSrc)) {
          e.currentTarget.src = defaultImgSrc;
        }
      }}
    />
  );
});

export const AppSignInLogo = React.memo(function ({ className }: AppLogoProps) {
  return <AppLogo className={className} type="sign-in/logo" />;
});

export const AppIcon = React.memo(function ({ className }: AppLogoProps) {
  return <AppLogo className={className} type="icon" />;
});

export const DefaultAppLogo = React.memo(function ({
  className,
}: AppLogoProps) {
  const { themeMode } = useAppSettings();

  return (
    <img
      className={className}
      src={getDefaultAppLogo(themeMode)}
      alt={APPLICATION_NAME}
    />
  );
});

function getDefaultAppLogo(themeMode: ThemeContextValue["mode"]) {
  return themeMode === "dark" ? defaultLogoDark : defaultLogo;
}
