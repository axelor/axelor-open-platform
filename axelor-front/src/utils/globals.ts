import uniqueId from "lodash/uniqueId";
import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";

import { alerts } from "@/components/alerts";
import { dialogs } from "@/components/dialogs";
import {
  openTab_internal as openTab,
  useActiveTab_internal as useActiveTab,
} from "@/hooks/use-tabs";
import { i18n } from "@/services/client/i18n";
import { ActionView } from "@/services/client/meta.types";
import { session, SessionInfo } from "@/services/client/session";
import { loadThemeOptions } from "@/hooks/use-app-theme";
import { getAppLang } from "@/hooks/use-app-lang";

const openView: typeof openTab = (view, options) => {
  if (view && typeof view !== "string" && !view.name) {
    return openTab({ ...view, name: uniqueId("$act") }, options);
  }
  return openTab(view, options);
};

const $openHtmlTab = (url: string, title: string) => {
  return openView({
    title,
    viewType: "html",
    views: [{ type: "html", name: url }],
  } as ActionView);
};

type AppData = {
  theme: string;
  options: ThemeOptions;
  info: SessionInfo | null;
  dir: string;
  lang: string;
};

type AppDataListener = (data?: AppData) => void;

class AxelorApp {
  #listeners: Set<AppDataListener>;
  i18n = i18n;
  alerts = alerts;
  dialogs = dialogs;
  openView = openView;
  $openHtmlTab = $openHtmlTab;
  useActiveTab = useActiveTab;

  constructor() {
    this.#listeners = new Set<AppDataListener>();
  }

  async loadTheme() {
    const info = session.info;
    const dark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    const preferred = dark ? "dark" : "light";
    const userTheme = info?.user?.theme ?? info?.application.theme;
    const theme = userTheme === "auto" ? preferred : (userTheme ?? "light");

    const options = await loadThemeOptions(theme);
    return { theme, options };
  }

  async getAppData() {
    const info = session.info;
    const { lang, dir } = getAppLang(info);
    const { theme, options } = await this.loadTheme();

    return {
      theme,
      options,
      info,
      dir,
      lang,
    };
  }

  onAppDataChanged(fn: AppDataListener) {
    this.#listeners.add(fn);
    return () => {
      this.#listeners.delete(fn);
    };
  }

  async notifyListeners() {
    if (this.#listeners.size === 0) return;

    const data = await this.getAppData();
    for (const fn of this.#listeners) {
      fn(data);
    }
  }
}

export const axelor = new AxelorApp();

// expose the module as a global variable
(window as any).axelor = axelor;
