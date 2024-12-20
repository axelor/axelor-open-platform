import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";

import { request } from "@/services/client/client";
import { LoadingCache } from "@/utils/cache";

import { useAsync } from "../use-async";
import { useAppTheme } from "./use-app-theme";

const cache = new LoadingCache<Promise<ThemeOptions>>();

type ThemeName = "light" | "dark";

const get = async (url: string) => {
  const res = await request({ url });
  return res.status === 200 ? res.json() : Promise.reject(res.status);
};

const load = async (theme: ThemeName) => {
  try {
    return await get(`ws/public/app/theme?name=${theme}`);
  } catch {
    return await get(`js/theme/${theme}.json`);
  }
};

export function useAppThemeOption() {
  const theme = useAppTheme();
  const { state, data } = useAsync(
    async () => cache.get(theme, () => load(theme)),
    [theme],
  );

  return {
    theme,
    loading: state === "loading",
    options: data,
  };
}
