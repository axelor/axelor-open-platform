import { request } from "@/services/client/client";

export async function quick() {
  const resp = await request({
    url: `ws/action/menu/quick`,
    method: "GET",
  });
  if (resp.ok) {
    const { status, data = [] } = await resp.json();
    return status === 0 ? data : Promise.reject(500);
  }
  return Promise.reject(resp.status);
}
