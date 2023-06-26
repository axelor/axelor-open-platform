import { request } from "@/services/client/client";
import { LoadingCache } from "@/utils/cache";
import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";
import merge from "lodash/merge";
import { useAppTheme } from ".";
import { useAsync } from "../use-async";

const cache = new LoadingCache<Promise<ThemeOptions>>();

const load = async (theme: "light" | "dark") => {
  const files =
    theme === "dark"
      ? [`theme/light.json`, `theme/dark.json`]
      : [`theme/light.json`];
  return Promise.all(
    files.map((url) => request({ url }).then((resp) => resp.json()))
  ).then((themes) => merge({}, ...themes));
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
