import {
  actionView as fetchAction,
  fields as fetchFields,
  view as fetchView,
  type MetaData,
  type ViewData,
} from "./meta";
import { processView, processWidgets } from "./meta-utils";
import { type ActionView, type ViewType } from "./meta.types";

type CacheEntry = {
  value: Promise<any>;
  accessAt: number;
  accessCount: number;
};

const cache = new Map<string, CacheEntry>();

const minHits = 5;
const minTime = 1000 * 60 * 60; // miliseconds

const evictLeastUsed = () => {
  const now = Date.now();
  const leastUsed: string[] = [];

  for (const [key, entry] of cache.entries()) {
    const diff = now - entry.accessAt;
    const hits = entry.accessCount;
    if (hits < minHits && diff > minTime) {
      leastUsed.push(key);
    }
  }

  for (const key of leastUsed) {
    cache.delete(key);
  }
};

const getOrLoad = (names: any[], load: () => Promise<any>) => {
  const key = names.join(":");
  const entry = cache.get(key);
  if (entry) {
    entry.accessAt = Date.now();
    entry.accessCount += 1;
    return entry.value;
  }

  const value = load();
  cache.set(key, {
    accessAt: Date.now(),
    accessCount: 1,
    value,
  });

  // evict old and least used entries
  evictLeastUsed();

  return value;
};

export async function findActionView(name: string): Promise<ActionView> {
  return getOrLoad(["action", name], () =>
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
  return getOrLoad(["views", model, type, name], () =>
    fetchView({ type: type as any, name, model }).then((data) => {
      // process the meta data
      processView(data, data.view);
      processWidgets(data.view);
      return data;
    })
  );
}

export async function findFields(model: string): Promise<MetaData> {
  return getOrLoad(["fields", model], () => fetchFields(model));
}
