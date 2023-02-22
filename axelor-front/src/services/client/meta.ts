import { request } from "./client";
import { Criteria, DataContext } from "./data.types";
import {
  ActionView,
  JsonField,
  MenuItem,
  Perms,
  Property,
  SavedFilter,
  ViewTypes,
} from "./meta.types";

export async function menus(): Promise<MenuItem[]> {
  const url = "ws/action/menu/all";
  const resp = await request({ url });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? data : Promise.reject(500);
  }

  return Promise.reject(resp.status);
}

export async function actionView(name: string): Promise<ActionView> {
  const url = `ws/action/${name}`;
  const resp = await request({
    url,
    method: "POST",
    body: {
      model: "com.axelor.meta.db.MetaAction",
    },
  });

  if (resp.ok) {
    const { status, data: [{ view }] = [{ view: null }] } = await resp.json();
    return status === 0 ? view : Promise.reject(500);
  }

  return Promise.reject(resp.status);
}

export async function filters(name: string): Promise<SavedFilter[]> {
  const url = "ws/action/com.axelor.meta.web.MetaFilterController:findFilters";
  const resp = await request({
    url,
    method: "POST",
    body: {
      context: {
        filterView: name,
      },
      model: "com.axelor.meta.db.MetaFilter",
    },
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? data : Promise.reject(500);
  }

  return Promise.reject(resp.status);
}

export interface MetaData {
  model: string;
  fields: Record<string, Property>;
  jsonFields?: Record<string, Record<string, JsonField>>;
  perms?: Perms;
}

export interface ViewData<T> extends MetaData {
  view: T;
}

export async function fields(model: string): Promise<MetaData> {
  const resp = await request({
    url: `ws/meta/fields/${model}`,
    method: "GET",
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? data : Promise.reject(500);
  }

  return Promise.reject(resp.status);
}

export async function view<T extends keyof ViewTypes, V = ViewTypes[T]>(
  model: string,
  options: { type: T; name?: string; context?: Record<string, any> }
): Promise<ViewData<V>> {
  const { type, name, context } = options;
  const resp = await request({
    url: "ws/meta/view",
    method: "POST",
    body: {
      model,
      data: {
        name,
        type,
        context,
      },
    },
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? data : Promise.reject(500);
  }

  return Promise.reject(resp.status);
}

export type ActionOptions = {
  action: string;
  model: string;
  data?: Criteria & {
    context?: DataContext;
    _domain: string;
    _domainContext?: DataContext;
    _archived?: boolean;
    _signal?: string;
  };
};

export async function action(options: ActionOptions) {
  const url = "ws/action";
  const resp = await request({
    url,
    body: options,
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? data : Promise.reject(500);
  }

  return Promise.reject(resp.status);
}
