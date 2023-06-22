import { request } from "@/services/client/client";
import { DataContext, DataRecord } from "@/services/client/data.types";

const cache: Record<string, DataRecord[]> = {};

export async function fetchMenus(parent?: string) {
  const url = `ws/search/menu${parent ? `?parent=${parent}` : ""}`;
  if (cache[url]) return cache[url];

  const resp = await request({
    url,
    method: "GET",
  });

  if (resp.ok) {
    const { status, data } = await resp.json();
    return (cache[url] = status === 0 ? data ?? [] : []);
  }

  return [];
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
