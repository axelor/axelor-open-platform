import { request } from "@/services/client/client";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { LoadingCache } from "@/utils/cache";

const cache = new LoadingCache<Promise<DataRecord[]>>();

export async function fetchMenus(parent?: string) {
  const queryString = parent ? `?parent=${parent}` : "";
  const url = `ws/search/menu${queryString}`;

  return await cache.get(url, async () => {
    const resp = await request({ url, method: "GET" });

    if (resp.ok) {
      const { status, data } = await resp.json();
      return status === 0 ? data ?? [] : [];
    }

    return [];
  });
}

export async function searchData({
  data,
  limit,
}: {
  data: DataContext;
  limit?: number;
}) {
  const resp = await request({
    url: "ws/search",
    method: "POST",
    body: {
      data,
      limit,
    },
  });

  if (resp.ok) {
    const { status, data = [] } = await resp.json();
    return status === 0 ? data : [];
  }

  return Promise.reject(500);
}
