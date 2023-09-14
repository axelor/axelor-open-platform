import { request } from "@/services/client/client";
import { Message, MessageAuthor } from "../mail-messages/message/types";

export type Follower = {
  id: number;
  email?: null | string;
  $author?: MessageAuthor;
  $authorModel?: string;
};

export type FollowerData = Message & {
  recipients?: { address: string; personal: string }[];
  selected?: boolean;
};

export async function getFollowers(
  model: string,
  id: number
): Promise<Follower[]> {
  const resp = await request({
    url: `ws/rest/${model}/${id}/followers`,
    method: "GET",
  });
  if (resp.ok) {
    const { status, data = [] } = await resp.json();
    return status === 0 ? data : Promise.reject(500);
  }
  return Promise.reject(resp.status);
}

export async function follow(model: string, id: number, data?: FollowerData) {
  const resp = await request({
    url: `ws/rest/${model}/${id}/follow`,
    method: "POST",
    body: {
      data,
    },
  });
  if (resp.ok) {
    const { status, data = [] } = await resp.json();
    return status === 0 ? data : Promise.reject(500);
  }
}

export async function unfollow(model: string, id: number, records: number[]) {
  const resp = await request({
    url: `ws/rest/${model}/${id}/unfollow`,
    method: "POST",
    body: {
      records,
    },
  });
  if (resp.ok) {
    const { status, data = [] } = await resp.json();
    return status === 0 ? data : Promise.reject(500);
  }
}
