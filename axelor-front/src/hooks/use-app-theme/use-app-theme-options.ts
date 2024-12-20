import merge from "lodash/merge";
import cloneDeep from "lodash/cloneDeep";
import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";

import { validateThemeOptions } from "@/components/theme-builder/utils";
import { request } from "@/services/client/client";
import { LoadingCache } from "@/utils/cache";

import { useAsync } from "../use-async";
import { useAppTheme } from "./use-app-theme";
import defaultTheme from "./themes/default.json";

const cache = new LoadingCache<Promise<ThemeOptions>>();

const get = async (url: string) => {
  const res = await request({ url });
  return res.status === 200 ? res.json() : Promise.reject(res.status);
};

const load = async (theme: string) => {
  try {
    return await get(`ws/public/app/theme?name=${theme}`);
  } catch {
    return theme === "light" ? {} : await get(`js/theme/${theme}.json`);
  }
};

export function useAppThemeOption() {
  const theme = useAppTheme();
  const { state, data } = useAsync(async () => {
    const options = await cache.get(theme, async () => {
      let themeContent: ThemeOptions = {};
      try {
        themeContent = await load(theme);
      } catch {
        // ignore
      }
      return merge(cloneDeep(defaultTheme), themeContent);
    });
    return validateThemeOptions(options);
  }, [theme]);

  return {
    theme,
    loading: state === "loading",
    options: data,
  };
}
