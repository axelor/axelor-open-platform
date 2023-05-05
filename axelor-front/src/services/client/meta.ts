import { request } from "./client";
import { Criteria, DataContext, DataRecord } from "./data.types";
import {
  ActionView,
  ChartView,
  HelpOverride,
  JsonField,
  MenuItem,
  Perms,
  Property,
  SavedFilter,
  ViewTypes,
} from "./meta.types";
import { reject } from "./reject";

export type MenuType = "all" | "quick" | "fav";

export async function menus(type: MenuType): Promise<MenuItem[]> {
  const url = `ws/action/menu/${type}`;
  const resp = await request({ url });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? data : reject(data);
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
    const { status, data } = await resp.json();
    if (status === 0) {
      const [{ view }] = data || [{ view: null }];
      return view;
    }
    return reject(data);
  }

  return Promise.reject(resp.status);
}

export async function filters(name: string): Promise<SavedFilter[]> {
  const url = "ws/action/com.axelor.meta.web.MetaFilterController:findFilters";
  const model = "com.axelor.meta.db.MetaFilter";
  const resp = await request({
    url,
    method: "POST",
    body: {
      data: {
        context: {
          filterView: name,
        },
        model,
      },
      model,
    },
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? data : reject(data);
  }

  return Promise.reject(resp.status);
}

export async function saveFilter(filter: SavedFilter) {
  const url = "ws/action/com.axelor.meta.web.MetaFilterController:saveFilter";
  const model = "com.axelor.meta.db.MetaFilter";
  const resp = await request({
    url,
    method: "POST",
    body: {
      data: {
        context: filter,
        model,
      },
      model,
    },
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? data : reject(data);
  }

  return Promise.reject(resp.status);
}

export async function removeFilter(filter: SavedFilter) {
  const url = "ws/action/com.axelor.meta.web.MetaFilterController:removeFilter";
  const model = "com.axelor.meta.db.MetaFilter";
  const resp = await request({
    url,
    method: "POST",
    body: {
      data: {
        context: { name: filter.name, filterView: filter.filterView },
        model,
      },
      model,
    },
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? data : reject(data);
  }

  return Promise.reject(resp.status);
}

export interface MetaData {
  model: string;
  fields: Record<string, Property>; // incoming is array, processed to object;
  jsonFields?: Record<string, Record<string, JsonField>>;
  perms?: Perms;
}

export interface ViewData<T> extends Partial<MetaData> {
  view: T;
  jsonAttrs?: JsonField[];
  helps?: Record<string, Record<string, HelpOverride>>;
  related?: Record<string, string[]>; // additional fields after view processing
}

export async function fields(model: string): Promise<MetaData> {
  const resp = await request({
    url: `ws/meta/fields/${model}`,
    method: "GET",
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    if (status === 0 && data?.fields) {
      data.fields = data.fields.reduce(
        (acc: Record<string, Property>, field: Property) => {
          acc[field.name] = field;
          return acc;
        },
        {} as Record<string, Property>
      );
    }
    return status === 0 ? data : reject(data);
  }

  return Promise.reject(resp.status);
}

export async function view<
  T extends keyof ViewTypes,
  V = ViewTypes[T]
>(options: {
  type: T;
  name?: string;
  model?: string;
  context?: Record<string, any>;
}): Promise<ViewData<V>> {
  const { type, name, model, context } = options;
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
    return status === 0 ? { model, ...data } : reject(data);
  }

  return Promise.reject(resp.status);
}

export async function chart<T = ChartView>(
  name: string,
  data?: DataContext,
  dataset = false
): Promise<T> {
  const resp = await request({
    url: `ws/meta/chart/${name}`,
    method: "POST",
    body: {
      data,
      ...(dataset && { fields: ["dataset"] }),
    },
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? ((dataset ? data.dataset : data) as T) : reject(data);
  }

  return Promise.reject(resp.status);
}

export async function custom(
  name: string,
  data?: DataContext
): Promise<{
  data: DataRecord[];
  first?: DataRecord;
}> {
  const resp = await request({
    url: `ws/meta/custom/${name}`,
    method: "POST",
    body: {
      data,
    },
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0
      ? {
          data: data?.dataset || [],
          first: data?.dataset?.[0],
        }
      : reject(data);
  }

  return Promise.reject(resp.status);
}

export type ActionOptions = {
  action: string;
  model: string;
  data?: Criteria & {
    context?: DataContext;
    _domain?: string;
    _domainContext?: DataContext;
    _archived?: boolean;
  };
};

export type ActionResult = {
  pending?: string;
  exportFile?: string;
  signal?: string;
  signalData?: any;
  info?: {
    title?: string;
    message: string;
    confirmBtnTitle?: string;
  };
  error?: {
    title?: string;
    message: string;
    action?: string;
    confirmBtnTitle?: string;
  };
  alert?: {
    title?: string;
    message: string;
    action?: string;
    confirmBtnTitle?: string;
    cancelBtnTitle?: string;
  };
  notify?:
    | { title: string; message: string }
    | { title: string; message: string }[];
  errors?: Record<string, string>;
  values?: DataRecord;
  attrs?: Record<string, Record<string, any>>;
  reload?: boolean;
  validate?: boolean;
  close?: boolean;
  canClose?: boolean;
  save?: boolean;
  new?: boolean;
  view?: ActionView;
  report?: boolean;
  reportLink?: string;
  reportFile?: string;
  reportFormat?: "pdf" | "html";
  attached?: {
    id: number;
    fileName: string;
  };
};

function prepareActionResult(data: any): ActionResult[] {
  if (Array.isArray(data)) {
    return data.map((item) => {
      const { "signal-data": signalData, ...rest } = item;
      return {
        ...rest,
        signalData,
      };
    });
  }
  return [];
}

export async function action(options: ActionOptions): Promise<ActionResult[]> {
  const url = "ws/action";
  const resp = await request({
    url,
    method: "POST",
    body: options,
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? prepareActionResult(data) : reject(data);
  }

  return Promise.reject(resp.status);
}
