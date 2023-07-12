import { set } from "lodash";
import { request } from "./client";
import { SelectorType } from "./meta.types";

export interface ClientInfo {
  name: string;
  icon?: string;
  title?: string;
}

interface CommonAppInfo {
  type: "public" | "session";
  app: {
    name?: string;
    description?: string;
    copyright?: string;
    theme?: string;
    logo?: string;
    icon?: string;
    lang?: string;
    version?: string;
    author?: string;
    sdk?: string;
    help?: string;
    home?: string;
    mode?: string;
  };
  auth?: {
    callbackUrl?: string;
    clients?: ClientInfo[];
    defaultClient?: string;
    exclusive?: boolean;
    currentClient?: string;
  };
  api?: {
    pagination?: {
      defaultPerPage: number;
      maxPerPage: number;
    };
    upload?: {
      maxSize: number;
    };
  };
  view?: {
    customizationPermission: number;
    customization: boolean;
    advanceSearch?: {
      share?: boolean;
      exportFull?: boolean;
    };
    grid?: {
      selection?: SelectorType;
    };
    menubar?: {
      location: string;
    };
    singleTab?: boolean;
    maxTabs?: number;
    collaboration?: {
      enabled: boolean;
      canView: boolean;
    };
  };
}

export interface PublicInfo extends CommonAppInfo {
  type: "public";
}

export interface SessionInfo extends CommonAppInfo {
  type: "session";
  user: {
    id: number;
    login: string;
    name: string;
    nameField?: string;
    lang?: string | null;
    image?: string | null;
    group?: string | null;
    action?: string | null;
    navigator?: string | null;
    theme?: string | null;
    noHelp: boolean;
    singleTab: boolean;
    technical: boolean;
  };
}

export type AppInfo = PublicInfo | SessionInfo;

const INFO_MAPPINGS = {
  application: "app",
  authentication: "auth",
  "application.author": "app.author",
  "application.copyright": "app.copyright",
  "application.description": "app.description",
  "application.help": "app.help",
  "application.home": "app.home",
  "application.theme": "app.theme",
  "application.logo": "app.logo",
  "application.icon": "app.icon",
  "application.mode": "app.mode",
  "application.name": "app.name",
  "application.sdk": "app.sdk",
  "application.version": "app.version",
  "auth.central.client": "auth.currentClient",
  "user.action": "user.action",
  "user.group": "user.group",
  "user.id": "user.id",
  "user.theme": "user.theme",
  "user.lang": "user.lang",
  "user.login": "user.login",
  "user.image": "user.image",
  "user.name": "user.name",
  "user.nameField": "user.nameField",
  "user.navigator": "user.navigator",
  "user.noHelp": "user.noHelp",
  "user.singleTab": "user.singleTab",
  "user.technical": "user.technical",
  "user.viewCustomizationPermission": "view.customizationPermission",
  "user.canViewCollaboration": "view.collaboration.canView",
  "api.pagination.default-per-page": "api.pagination.defaultPerPage",
  "api.pagination.max-per-page": "api.pagination.maxPerPage",
  "data.upload.max-size": "api.upload.maxSize",
  "view.allow-customization": "view.customization",
  "view.grid.selection": "view.grid.selection",
  "view.menubar.location": "view.menubar.location",
  "view.adv-search.share": "view.advanceSearch.share",
  "view.adv-search.export-full": "view.advanceSearch.exportFull",
  "view.single-tab": "view.singleTab",
  "view.max-tabs": "view.maxTabs",
  "view.collaboration.enabled": "view.collaboration.enabled",
};

export type AppInfoListener = (info: AppInfo | null) => void;

async function init() {
  const url = "ws/public/app/info";
  const resp = await request({ url });

  if (!resp.ok) {
    return Promise.reject(resp.status);
  }

  const data = await resp.json();
  const info: any = {};

  if (data["data.upload.max-size"]) {
    data["data.upload.max-size"] = parseInt(data["data.upload.max-size"]);
  }

  if (data["view.max-tabs"]) {
    data["view.max-tabs"] = parseInt(data["view.max-tabs"]);
  }

  for (let [key, target] of Object.entries(INFO_MAPPINGS)) {
    if (key in data) {
      set(info, target, data[key]);
    }
  }

  info.type = info.user ? "session" : "public";

  return info as AppInfo;
}

export class Session {
  #info: AppInfo | null = null;
  #infoPromise: Promise<AppInfo> | null = null;
  #listeners = new Set<AppInfoListener>();

  #notify() {
    this.#listeners.forEach((fn) => fn(this.#info));
  }

  subscribe(listener: AppInfoListener) {
    this.#listeners.add(listener);
    return () => {
      this.#listeners.delete(listener);
    };
  }

  get info() {
    return this.#info?.type === "session" ? this.#info : null;
  }

  get appInfo() {
    return this.#info;
  }

  async init() {
    return this.#info ?? (await this.#load());
  }

  async #load() {
    this.#infoPromise = this.#infoPromise ?? init();
    this.#info = await this.#infoPromise;
    this.#notify();
    return this.#info;
  }

  async login(
    args: {
      username: string;
      password: string;
    },
    params?: URLSearchParams
  ): Promise<SessionInfo> {
    const url = "callback" + (params ? `?${params}` : "");
    const { status, ok } = await request({
      url,
      method: "POST",
      body: args,
    });

    if (ok) {
      this.#infoPromise = init();
      const info = await this.#load();
      if (info.type === "session") {
        return info;
      }
    }

    return Promise.reject(status);
  }

  async logout() {
    const response = await request({ url: "logout" });
    const { status } = response;
    const redirectUrl: string | null =
      status === 200 ? (await response.json()).redirectUrl : null;

    this.#info = null;
    this.#infoPromise = null;
    this.#notify();

    return { status, redirectUrl };
  }
}

export const session = new Session();
