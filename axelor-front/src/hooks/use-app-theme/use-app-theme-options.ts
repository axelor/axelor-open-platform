import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";
import cloneDeep from "lodash/cloneDeep";
import merge from "lodash/merge";

import { validateThemeOptions } from "@/components/theme-builder/utils";
import { request } from "@/services/client/client";
import { LoadingCache } from "@/utils/cache";

import { useAsync } from "../use-async";
import darkTheme from "./themes/dark.json";
import defaultTheme from "./themes/default.json";
import { useAppTheme } from "./use-app-theme";

const cache = new LoadingCache<Promise<ThemeOptions>>();

const get = async (url: string) => {
  const res = await request({ url });
  return res.status === 200 ? res.json() : Promise.reject(res.status);
};

export function loadThemeOptions(theme: string) {
  return cache.get(theme, async () => {
    let themeContent: ThemeOptions = {};
    let baseTheme = defaultTheme;
    try {
      themeContent = await get(
        `ws/public/app/theme?name=${encodeURIComponent(theme)}`,
      );
    } catch {
      // ignore
    }
    if (theme === "dark" || themeContent?.palette?.mode === "dark") {
      baseTheme = merge(cloneDeep(defaultTheme), darkTheme);
    }
    return validateThemeOptions(merge(cloneDeep(baseTheme), themeContent));
  });
}

export function useAppThemeOption() {
  const theme = useAppTheme();
  const { state, data } = useAsync(() => loadThemeOptions(theme), [theme]);
  return {
    theme,
    loading: state === "loading",
    options: data,
  };
}
