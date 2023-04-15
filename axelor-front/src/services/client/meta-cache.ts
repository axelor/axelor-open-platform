import { LoadingCache } from "@/utils/cache";

import {
  actionView as fetchAction,
  fields as fetchFields,
  view as fetchView,
  type MetaData,
  type ViewData,
} from "./meta";
import { findViewFields, processView, processWidgets } from "./meta-utils";
import { type ActionView, type ViewType } from "./meta.types";

const cache = new LoadingCache<Promise<any>>();

const makeKey = (...args: any[]) => args.map((x) => x || "").join(":");

export async function findActionView(name: string): Promise<ActionView> {
  return cache.get(makeKey("action", name), () =>
    fetchAction(name).then((view) => ({ ...view, name }))
  );
}

export async function findView<T extends ViewType>({
  type,
  name,
  model,
}: {
  type: string;
  name?: string;
  model?: string;
}): Promise<ViewData<T>> {
  return cache.get(makeKey("view", model, type, name), () => {
    if (type === "chart") {
      return Promise.resolve({ view: { name, model, type } });
    }
    return fetchView({ type: type as any, name, model }).then((data) => {
      const { related } = findViewFields(data.view);
      data.related = { ...data.related, ...related };
      // process the meta data
      processView(data, data.view);
      processWidgets(data.view);
      return data;
    });
  });
}

export async function findFields(model: string): Promise<MetaData> {
  return cache.get(makeKey("meta", model), () => fetchFields(model));
}
