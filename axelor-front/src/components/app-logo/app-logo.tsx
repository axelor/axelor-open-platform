import { useMemo } from "react";

import { APPLICATION_NAME, useAppSettings } from "@/hooks/use-app-settings";

import defaultLogoDark from "@/assets/axelor-dark.svg";
import defaultIcon from "@/assets/axelor-icon.svg";
import defaultLogo from "@/assets/axelor.svg";

type AppLogoProps = Readonly<{
  className?: string;
  type?: "logo" | "sign-in/logo" | "icon";
}>;

export function AppLogo({ type = "logo", className }: AppLogoProps) {
  const { name, themeMode } = useAppSettings();

  const imgSrc = useMemo(
    () =>
      `ws/public/app/${type}${themeMode !== "light" ? `?mode=${themeMode}` : ""}`,
    [type, themeMode],
  );

  return (
    <img
      className={className}
      src={imgSrc}
      alt={name}
      onError={(e) => {
        if (type === "icon") {
          e.currentTarget.src = defaultIcon;
        } else {
          e.currentTarget.src =
            themeMode === "dark" ? defaultLogoDark : defaultLogo;
        }
      }}
    />
  );
}

export function AppSignInLogo({ className }: AppLogoProps) {
  return <AppLogo className={className} type="sign-in/logo" />;
}

export function AppIcon({ className }: AppLogoProps) {
  return <AppLogo className={className} type="icon" />;
}

export function DefaultAppLogo({ className }: AppLogoProps) {
  const { themeMode } = useAppSettings();

  return (
    <img
      className={className}
      src={themeMode === "dark" ? defaultLogoDark : defaultLogo}
      alt={APPLICATION_NAME}
    />
  );
}
