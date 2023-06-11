import { useMediaQuery } from "../use-media-query";
import { useSession } from "../use-session";

export function useAppTheme() {
  const session = useSession();
  const info = session.data;
  const dark = useMediaQuery("(prefers-color-scheme: dark)");

  const preferred = dark ? "dark" : "light";
  const userTheme = info ? info.user.theme || info.app.theme : null;
  const appTheme = userTheme === "auto" ? preferred : userTheme || preferred;

  return appTheme as "light" | "dark";
}
