import { Message, MessageFlag } from "./message/types";
import { request } from "@/services/client/client";

type QueryParam = Record<string, any>;

const paramsToQueryString = (params?: QueryParam) =>
  Object.keys(params || {})
    .filter((k) => (params || {})[k] !== undefined)
    .filter((k) => (params || {})[k] !== null)
    .map((k) => `${k}=${(params || {})[k]}`)
    .join("&");

export const DataSource = (() => {
  const messagesPath = "ws/messages";

  async function getMessages(params: QueryParam) {
    const resp = await request({
      url: `${messagesPath}?${paramsToQueryString(params)}`,
      method: "GET",
    });
    if (resp.ok) {
      const { status, ...result } = await resp.json();
      return status === 0 ? result : Promise.reject(500);
    }
    return Promise.reject(resp.status);
  }

  function getRecordMessages(
    relatedId: number,
    relatedModel: string,
    options?: {
      type?: string;
      limit?: number;
      offset?: number;
    },
  ) {
    const { type, limit, offset } = options ?? {};
    return getMessages({ type, relatedId, relatedModel, limit, offset });
  }

  function getFolderMessages(folder: string, limit?: number, offset?: number) {
    return getMessages({ folder, limit, offset });
  }

  async function addMessage(model: string, id: number, data: Message) {
    const resp = await request({
      url: `ws/rest/${model}/${id}/message`,
      method: "POST",
      body: {
        data,
      },
    });
    if (resp.ok) {
      const { status, data } = await resp.json();
      return status === 0 ? data : Promise.reject(500);
    }
  }

  async function deleteMessage(id: number) {
    const resp = await request({
      url: `${messagesPath}/${id}`,
      method: "DELETE",
    });
    if (resp.ok) {
      const { status } = await resp.json();
      return status === 0 ? true : false;
    }
    return Promise.reject(500);
  }

  async function getReplies(id: number | string) {
    const resp = await request({
      url: `${messagesPath}/${id}/replies`,
      method: "GET",
    });
    if (resp.ok) {
      const { status, ...result } = await resp.json();
      return status === 0 ? result : Promise.reject(500);
    }
    return Promise.reject(resp.status);
  }

  async function postFlags(records: MessageFlag[]) {
    const resp = await request({
      url: `${messagesPath}/flag`,
      method: "POST",
      body: {
        records,
      },
    });
    if (resp.ok) {
      const {
        status,
        data,
      }: { status: number; data?: MessageFlag[] } = await resp.json();
      if (status === 0) {
        return data;
      }
    }
    return Promise.reject(500);
  }

  return {
    messages: getRecordMessages,
    folder: getFolderMessages,
    add: addMessage,
    remove: deleteMessage,
    replies: getReplies,
    flags: postFlags,
  };
})();
