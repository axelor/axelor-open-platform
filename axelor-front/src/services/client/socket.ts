import { session } from "./session";

const url = new URL("websocket", window.location.href)
  .toString()
  .replace(/^http/, "ws");

export type SocketDataType = "SUB" | "UNS" | "MSG";

export type SocketData = {
  type: SocketDataType;
  channel: string;
  data: any;
};

export type SocketListener = (data: SocketData) => void;

export class Socket {
  #ws: WebSocket | null = null;
  #connecting: Promise<void> = Promise.resolve();
  #listeners: Record<string, Set<SocketListener>> = {};

  async init() {
    if (!this.#ws) {
      this.#ws = new WebSocket(url);
      this.#ws.onerror = () => {
        this.#ws = null;
      };
      this.#ws.onmessage = (event) => {
        const msg = JSON.parse(event.data) as SocketData;
        if (msg?.channel) {
          const listeners = this.#listeners[msg.channel];
          listeners.forEach((fn) => fn(msg.data));
        }
      };
      this.#ws.onclose = () => {
        this.#ws = null;
      };
      this.#connecting = new Promise((resolve) => {
        this.#ws!.onopen = () => {
          resolve();
        };
      });
    }

    if (this.#ws?.readyState === WebSocket.OPEN) {
      return;
    }

    return this.#connecting;
  }

  async send(type: SocketDataType, channel: string, data?: object) {
    await this.init();

    if (session.info) {
      this.#ws?.send(
        JSON.stringify({ type: type, channel: channel, data: data })
      );
    }
  }

  subscribe(channel: string, listener: SocketListener) {
    const listenerSet =
      this.#listeners[channel] ||
      (this.#listeners[channel] = new Set<SocketListener>());

    listenerSet.add(listener);

    if (listenerSet.size === 1) {
      this.send("SUB", channel);
    }
    return () => {
      listenerSet.delete(listener);
      if (listenerSet.size === 0) {
        this.send("UNS", channel);
      }
    };
  }
}

export const socket = new Socket();

export class SocketChannel {
  #channel: string;

  constructor(channel: string) {
    this.#channel = channel;
  }

  send(data?: object) {
    socket.send("MSG", this.#channel, data);
  }

  subscribe(listener: SocketListener) {
    return socket.subscribe(this.#channel, listener);
  }
}
