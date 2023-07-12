import { useMediaQuery } from "../use-media-query";
import { useSession } from "../use-session";

export function useAppTheme() {
  const { data: info, appData: appInfo } = useSession();
  const dark = useMediaQuery("(prefers-color-scheme: dark)");

  const preferred = dark ? "dark" : "light";
  const userTheme = info?.user?.theme ?? appInfo?.app.theme;
  const appTheme = userTheme === "auto" ? preferred : userTheme ?? preferred;

  return appTheme as "light" | "dark";
}
