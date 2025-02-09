import { ButtonProps } from "@axelor/ui";
import { ComponentProps } from "react";

import { IconProps } from "@/components/icon";
import { request } from "./client";
import { i18n } from "./i18n";
import { SelectorType } from "./meta.types";
export interface ClientInfo {
  name: string;
  icon?: string;
  title?: string;
}

export type SignInButtonType = {
  type?: "button" | "link";
  title?: string;
  variant?: ButtonProps["variant"];
  icon?: IconProps["icon"];
  link?: ComponentProps<"a">["href"];
  order?: number;
};

export interface SessionInfo {
  application: {
    name?: string;
    author?: string;
    description?: string;
    copyright?: string;
    theme?: string;
    lang?: string;
    version?: string;
    home?: string;
    help?: string;
    mode?: string;
    aopVersion?: string;
    aopBuildDate?: string;
    aopGitHash?: string;
    pollingInterval?: number;
    swaggerUI?: {
      enabled?: boolean;
      allowTryItOut?: boolean;
    };
    signIn?: {
      title?: string;
      footer?: string;
      fields?: {
        [key in "username" | "password" | "tenant"]: {
          showTitle?: boolean;
          title?: string;
          placeholder?: string;
          icon?: string;
        };
      };
      buttons?: {
        [key in "submit" | string]: SignInButtonType;
      };
    };
    resetPasswordEnabled?: boolean;
  };
  authentication?: {
    callbackUrl?: string;
    clients?: ClientInfo[];
    defaultClient?: string;
    exclusive?: boolean;
    currentClient?: string;
    tenants?: Record<string, string>;
    tenant?: string;
  };
  user?: {
    id: number;
    login: string;
    name: string;
    nameField?: string;
    lang?: string | null;
    image?: string | null;
    action?: string | null;
    singleTab?: boolean;
    noHelp?: boolean;
    theme?: string | null;
    group?: string | null;
    navigator?: string | null;
    technical?: boolean;
    viewCustomizationPermission?: number;
    canViewCollaboration?: boolean;
  };
  view?: {
    singleTab?: boolean;
    maxTabs?: number;
    form?: {
      checkVersion?: boolean;
    };
    grid?: {
      selection?: SelectorType;
    };
    advancedSearch?: {
      exportFull?: boolean;
      share?: boolean;
    };
    allowCustomization?: boolean;
    collaboration?: {
      enabled?: boolean;
    };
  };
  api?: {
    pagination?: {
      maxPerPage?: number;
      defaultPerPage?: number;
    };
  };
  data?: {
    upload?: {
      maxSize?: number;
    };
  };
  features?: {
    dmsSpreadsheet?: boolean;
    studio?: boolean;
  };
  route?: {
    path: string;
    state: Record<string, unknown>;
  };
}

export type SessionListener = (info: SessionInfo | null) => void;

async function init() {
  const url = "ws/public/app/info";
  const resp = await request({ url });

  if (!resp.ok) {
    return Promise.reject(resp.status);
  }

  const data = await resp.json();

  await i18n.load();

  return data as SessionInfo;
}

export class Session {
  #info: SessionInfo | null = null;
  #infoPromise: Promise<SessionInfo> | null = null;
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
      newPassword?: string;
    },
    options?: {
      params?: URLSearchParams;
      tenant?: string;
    },
  ): Promise<SessionInfo> {
    const params = options?.params;
    const tenant = options?.tenant;
    const url = "callback" + (params ? `?${params}` : "");
    const headers = tenant ? { "X-Tenant-ID": String(tenant) } : undefined;
    const response = await request({
      url,
      method: "POST",
      headers,
      body: args,
    });

    const { status, ok } = response;

    if (!ok) {
      return Promise.reject(status);
    }

    const data = await response.json();
    const { route } = data;

    if (route?.path && this.#info) {
      return {
        ...this.#info,
        route,
      };
    }

    this.#infoPromise = init();
    return this.#load();
  }

  async logout() {
    window.location.href = "logout";
  }
}

export const session = new Session();
