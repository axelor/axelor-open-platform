import { set } from "lodash";
import { request } from "./client";

export interface SessionInfo {
  api?: {
    pagination?: {
      defaultPerPage: number;
      maxPerPage: number;
    };
    upload?: {
      maxSize: number;
    };
  };
  app: {
    name: string;
    version: string;
    author: string;
    copyright: string;
    description: string;
    sdk: string | null;
    logo: string | null;
    help: string | null;
    home: string | null;
    mode: string | null;
  };
  user: {
    id: number;
    name: string;
    nameField?: string;
    login: string;
    lang?: string | null;
    group?: string | null;
    action?: string | null;
    navigator?: string | null;
    noHelp: boolean;
    singleTab: boolean;
    technical: boolean;
  };
  view?: {
    collaboration: boolean;
    customizationPermission: number;
    customization: boolean;
    advanceSearch?: {
      share?: boolean;
      exportFull?: boolean;
    };
    grid?: {
      editorButtons: boolean;
    };
    menubar?: {
      location: string;
    };
  };
}

const INFO_MAPPINGS = {
  "api.pagination.default-per-page": "api.pagination.defaultPerPage",
  "api.pagination.max-per-page": "api.pagination.maxPerPage",
  "application.author": "app.author",
  "application.copyright": "app.copyright",
  "application.description": "app.description",
  "application.help": "app.help",
  "application.home": "app.home",
  "application.logo": "app.logo",
  "application.mode": "app.mode",
  "application.name": "app.name",
  "application.sdk": "app.sdk",
  "application.version": "app.version",
  "data.upload.max-size": "api.upload.maxSize",
  "user.action": "user.action",
  "user.canViewCollaboration": "view.collaboration",
  "user.group": "user.group",
  "user.id": "user.id",
  "user.lang": "user.lang",
  "user.login": "user.login",
  "user.name": "user.name",
  "user.nameField": "user.nameField",
  "user.navigator": "user.navigator",
  "user.noHelp": "user.noHelp",
  "user.singleTab": "user.singleTab",
  "user.technical": "user.technical",
  "user.viewCustomizationPermission": "view.customizationPermission",
  "view.allow-customization": "view.customization",
  "view.grid.editor-buttons": "view.grid.editorButtons",
  "view.menubar.location": "view.menubar.location",
  "view.adv-search.share": "view.advanceSearch.share",
  "view.adv-search.export-full": "view.advanceSearch.exportFull",
};

export type SessionListener = (info: SessionInfo | null) => void;

export class Session {
  #info: SessionInfo | null = null;
  #listeners = new Set<SessionListener>();

  #notify() {
    this.#listeners.forEach((fn) => fn(this.#info));
  }

  subscribe(listener: SessionListener) {
    this.#listeners.add(listener);
    return () => {
      this.#listeners.delete(listener);
    };
  }

  get info() {
    return this.#info;
  }

  async init() {
    return this.#info || (await this.#load());
  }

  async #load() {
    const url = "ws/app/info";
    const resp = await request({ url });

    if (!resp.ok) {
      return Promise.reject(resp.status);
    }

    const data = await resp.json();
    const tmp: any = {};

    if (data["data.upload.max-size"]) {
      data["data.upload.max-size"] = parseInt(data["data.upload.max-size"]);
    }

    for (let [key, target] of Object.entries(INFO_MAPPINGS)) {
      if (key in data) {
        set(tmp, target, data[key]);
      }
    }

    this.#info = tmp as SessionInfo;
    this.#notify();

    return this.#info;
  }

  async login(args: {
    username: string;
    password: string;
  }): Promise<SessionInfo> {
    const { status, ok } = await request({
      url: "callback",
      method: "POST",
      body: args,
    });

    return ok ? await this.#load() : Promise.reject(status);
  }

  async logout() {
    const { status } = await request({ url: "logout" });

    this.#info = null;
    this.#notify();

    return status;
  }
}

export const session = new Session();
