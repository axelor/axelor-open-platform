import { LoadingCache } from "@/utils/cache";

import { request, RequestOptions } from "./client";
import { DataContext } from "./data.types";
import {
  actionView as fetchAction,
  fields as fetchFields,
  view as fetchView,
  type MetaData,
  type ViewData,
} from "./meta";
import { findViewFields, processView, processWidgets } from "./meta-utils";
import { FormView, type ActionView, type ViewType } from "./meta.types";
import { reject } from "./reject";

const cache = new LoadingCache<Promise<any>>();

const makeKey = (...args: any[]) => args.map((x) => x || "").join(":");

export async function findActionView(
  name: string,
  context?: DataContext,
  options?: RequestOptions,
): Promise<ActionView> {
  return fetchAction(name, context, options).then((view) =>
    view ? { ...view, name } : view,
  );
}

export async function findView<T extends ViewType>({
  type,
  name,
  model,
  resource,
  context,
  ...props
}: {
  type: string;
  name?: string;
  model?: string;
  resource?: string;
  context?: DataContext;
}): Promise<ViewData<T>> {
  return cache.get(makeKey("view", model, type, name ?? resource), async () => {
    if (type === "html") {
      return Promise.resolve({ view: { name: name ?? resource, type } });
    }

    if (type === "chart") {
      return Promise.resolve({ view: { name, model, type } });
    }

    // for custom form view like dms spreadsheet/html view
    if ((props as FormView).items) {
      const { fields = {}, ...viewProps } = props as ViewData<FormView>;
      return { model, view: { name, model, type, ...viewProps }, fields };
    }

    const data = await fetchView({ type: type as any, name, model, context });
    const { related } = findViewFields(data.view);

    data.related = { ...data.related, ...related };

    // process the meta data
    processView(data, data.view);
    processWidgets(data.view);

    return data;
  });
}

export async function findFields(
  model: string,
  jsonModel?: string,
): Promise<MetaData> {
  return cache.get(makeKey("meta", model, jsonModel), () =>
    fetchFields(model, jsonModel),
  );
}

export async function saveView(data: any) {
  const resp = await request({
    url: "ws/meta/view/save",
    method: "POST",
    body: { data },
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    if (status === 0) {
      const { model, type, name } = data;
      const key = makeKey("view", model, type, name);
      cache.delete(key);
      return data;
    }
    return reject(data);
  }

  return Promise.reject(resp.status);
}
