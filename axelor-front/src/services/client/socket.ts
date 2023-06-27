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

type EventName = "onopen" | "onclose";
type Callback = (event: Event) => void;

export class Socket {
  #ws: WebSocket | null = null;
  #connecting: Promise<void> = Promise.resolve();
  #listeners: Record<string, Set<SocketListener>> = {};
  #callbacks: { [K in EventName]?: Callback[] } = {};

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
      this.#ws.onclose = (event) => {
        this.#notifyCallbacks("onclose", event);
        this.#ws = null;
      };
      this.#connecting = new Promise((resolve) => {
        this.#ws!.onopen = (event) => {
          this.#notifyCallbacks("onopen", event);
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

  subscribe(
    channel: string,
    listener: SocketListener,
    callbacks?: { [K in EventName]?: Callback }
  ) {
    const uncallbacks = Object.entries(callbacks ?? {}).map(([key, value]) =>
      this.#registerCallback(key as EventName, value)
    );

    const listenerSet =
      this.#listeners[channel] ||
      (this.#listeners[channel] = new Set<SocketListener>());

    listenerSet.add(listener);

    if (listenerSet.size === 1) {
      this.send("SUB", channel);
    }
    return () => {
      uncallbacks.forEach((uncallback) => uncallback());
      listenerSet.delete(listener);
      if (listenerSet.size === 0) {
        this.send("UNS", channel);
      }
    };
  }

  #notifyCallbacks(eventName: EventName, event: Event) {
    this.#callbacks[eventName]?.forEach((callback) => callback(event));
  }

  #registerCallback(eventName: EventName, callback: Callback) {
    let callbacks = this.#callbacks[eventName];
    if (callbacks == null) {
      callbacks = this.#callbacks[eventName] = [];
    }
    return this.#addCallback(callbacks, callback);
  }

  #addCallback(callbacks: Callback[], callback: Callback) {
    const index = callbacks.length;
    callbacks.push(callback);
    return () => callbacks.splice(index, 1);
  }
}

export const socket = new Socket();

export class SocketChannel {
  #channel: string;
  #callbacks?: { [K in EventName]?: Callback };

  constructor(channel: string, callbacks?: { [K in EventName]?: Callback }) {
    this.#channel = channel;
    this.#callbacks = callbacks;
  }

  send(data?: object) {
    socket.send("MSG", this.#channel, data);
  }

  subscribe(listener: SocketListener) {
    return socket.subscribe(this.#channel, listener, this.#callbacks);
  }
}
