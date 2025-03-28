import { useMemo } from "react";

import { useTheme } from "@axelor/ui/core";

import { i18n } from "@/services/client/i18n";
import { useSession } from "../use-session";

export const APPLICATION_NAME = "Axelor";
const DESCRIPTION = "Axelor Enterprise Application";
export const COPYRIGHT = `© ${new Date().getFullYear()} Axelor. ${i18n.get(
  "All Rights Reserved",
)}.`;

export function useAppSettings() {
  const { data: info, state } = useSession();
  const { mode = "light", theme } = useTheme();

  return useMemo(() => {
    const themeMode = theme === "dark" ? theme : mode;
    const isReady = state === "hasData";
    const name = info?.application.name ?? APPLICATION_NAME;
    const description = info?.application.description ?? DESCRIPTION;
    const copyright =
      (info?.application.copyright?.replace("&copy;", "©") ?? "").trim() ||
      COPYRIGHT;

    return {
      isReady,
      name,
      description,
      copyright,
      themeMode,
    };
  }, [
    mode,
    state,
    theme,
    info?.application.copyright,
    info?.application.description,
    info?.application.name,
  ]);
}
