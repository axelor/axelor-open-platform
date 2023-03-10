import {
  actionView as fetchAction,
  fields as fetchFields,
  MetaData,
  view as fetchView,
  ViewData,
} from "./meta";
import { processView, processWidgets } from "./meta-utils";
import { ActionView, ViewType } from "./meta.types";

const CACHE: Record<string, Promise<any>> = {};

const makeKey = (type: string, ...args: any[]) => {
  const parts = [type, ...args].flat().filter(Boolean);
  return parts.join(":");
};

export async function findActionView(name: string): Promise<ActionView> {
  let key = makeKey("action", name);
  let res = CACHE[key];
  if (res) {
    return res;
  }
  res = CACHE[key] = fetchAction(name).then((view) => ({ ...view, name }));
  return res;
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
  let key = makeKey("view", type, name, model);
  let res = CACHE[key];
  if (res) {
    return res;
  }

  res = CACHE[key] = fetchView({ type: type as any, name, model }).then(
    (data) => {
      // process the meta data
      processView(data, data.view);
      processWidgets(data.view);
      return data;
    }
  );

  return res;
}

export async function findFields(model: string): Promise<MetaData> {
  const key = makeKey("fields", model);

  let res = CACHE[key];
  if (res) {
    return res;
  }

  res = CACHE[key] = fetchFields(model);
  return res;
}
