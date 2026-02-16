import { SocketChannel } from "@/services/client/socket";
import { Store, createStore } from "@/store";

import { Message } from "./message/types";

type Room = {
  model: string;
  recordId: number;
  messages: Message[];
};

type MessageData = {
  command: "MESSAGES" | "DELETED";
  model: string;
  recordId: number;
  messages: Message[];
};

type MessageListener = (messages: Message[]) => void;
type DeleteListener = (messageIds: number[]) => void;

function getKey(model: string, recordId: number) {
  return `${model}:${recordId}`;
}

const MailChannelService = () => {
  const roomStores: Record<string, Store<Room> | undefined> = {};
  const roomJoinCounts: Record<string, number> = {};
  const roomDeleteListeners: Record<string, Set<DeleteListener>> = {};

  function onopen() {
    for (const store of Object.values(roomStores)) {
      const room = store?.get();
      if (room) {
        channel.send({
          command: "JOIN",
          model: room.model,
          recordId: room.recordId,
        });
      }
    }
  }

  const channel = new SocketChannel("mail", { onopen });
  let unsubscribeCallback: (() => void) | null = null;

  const channelCallback = (data: unknown) => {
    const { command, model, recordId, messages } = data as MessageData;
    const key = getKey(model, recordId);

    if (command === "MESSAGES") {
      const roomStore = roomStores[key];
      if (roomStore && messages?.length) {
        roomStore.set((prev) => ({
          ...prev,
          messages,
        }));
      }
    } else if (command === "DELETED" && messages?.length) {
      const listeners = roomDeleteListeners[key];
      if (listeners?.size) {
        const ids = messages.map((m) => m.id!);
        for (const listener of listeners) {
          listener(ids);
        }
      }
    }
  };

  let subscribeCount = 0;

  function subscribe() {
    subscribeCount++;
    if (!unsubscribeCallback) {
      unsubscribeCallback = channel.subscribe(channelCallback);
    }
    return () => {
      if (--subscribeCount <= 0) {
        subscribeCount = 0;
        // Defer unsubscribe to a microtask so that any synchronous
        // room LEFT cleanups (from other useEffect cleanups in the
        // same unmount cycle) are sent before the channel unsubscribes.
        queueMicrotask(() => {
          if (subscribeCount <= 0 && unsubscribeCallback) {
            unsubscribeCallback();
            unsubscribeCallback = null;
          }
        });
      }
    };
  }

  function joinRoom(
    model: string,
    recordId: number,
    listener: MessageListener,
    deleteListener?: DeleteListener,
  ) {
    const key = getKey(model, recordId);
    let roomStore = roomStores[key];

    if (roomStore == null) {
      roomStore = roomStores[key] = createStore<Room>({
        model,
        recordId,
        messages: [],
      });
      roomJoinCounts[key] = 1;
    } else {
      ++roomJoinCounts[key];
    }

    const unsubscribeStore = roomStore.subscribe((room) => {
      if (room.messages.length) {
        listener(room.messages);
      }
    });

    if (deleteListener) {
      if (!roomDeleteListeners[key]) {
        roomDeleteListeners[key] = new Set();
      }
      roomDeleteListeners[key].add(deleteListener);
    }

    channel.send({
      command: "JOIN",
      model,
      recordId,
    });

    return () => {
      unsubscribeStore();
      if (deleteListener) {
        roomDeleteListeners[key]?.delete(deleteListener);
        if (!roomDeleteListeners[key]?.size) {
          delete roomDeleteListeners[key];
        }
      }
      if (--roomJoinCounts[key] <= 0) {
        channel.send({ command: "LEFT", model, recordId });
        delete roomStores[key];
        delete roomJoinCounts[key];
      }
    };
  }

  return { subscribe, joinRoom };
};

type MailChannelServiceType = ReturnType<typeof MailChannelService>;

let serviceInstance: MailChannelServiceType | null = null;

export function getMailChannelService() {
  if (!serviceInstance) {
    serviceInstance = MailChannelService();
  }
  return serviceInstance;
}
