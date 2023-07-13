import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";

import { request } from "@/services/client/client";
import { LoadingCache } from "@/utils/cache";

import { useAsync } from "../use-async";
import { useAppTheme } from "./use-app-theme";

const cache = new LoadingCache<Promise<ThemeOptions>>();

const load = async (theme: "light" | "dark") => {
  const url = theme === "dark" ? `js/theme/dark.json` : `js/theme/light.json`;
  return request({ url }).then((resp) => resp.json());
};

export function useAppThemeOption() {
  const theme = useAppTheme();
  const { state, data } = useAsync(
    async () => cache.get(theme, () => load(theme)),
    [theme]
  );

  return {
    theme,
    loading: state === "loading",
    options: data,
  };
}
