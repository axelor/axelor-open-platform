import { i18n } from "@/services/client/i18n";
import { useSession } from "../use-session";
import { useAppThemeOption } from "../use-app-theme";

const NAME = "Axelor";
const DESCRIPTION = "Axelor Enterprise Application";
export const COPYRIGHT = `© 2005–${new Date().getFullYear()} Axelor. ${i18n.get(
  "All Rights Reserved",
)}.`;

export function useAppSettings() {
  const { data: info, state } = useSession();
  const themeMode = useAppThemeMode();

  const isReady = state === "hasData";
  const name = info?.application.name ?? NAME;
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
}

function useAppThemeMode() {
  const { theme, options } = useAppThemeOption();
  return theme === "dark" ? "dark" : (options?.palette?.mode ?? "light");
}
