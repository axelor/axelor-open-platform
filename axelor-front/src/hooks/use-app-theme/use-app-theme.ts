import { useMediaQuery } from "../use-media-query";
import { useSession } from "../use-session";

export function useAppTheme() {
  const { data: info } = useSession();
  const dark = useMediaQuery("(prefers-color-scheme: dark)");

  const preferred = dark ? "dark" : "light";
  const userTheme = info?.user?.theme ?? info?.application.theme;
  return userTheme === "auto" ? preferred : (userTheme ?? "light");
}
