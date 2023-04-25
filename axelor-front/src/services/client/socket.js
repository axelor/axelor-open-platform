import { session } from "./session";

let url = new URL("websocket", window.location.href)
  .toString()
  .replace(/^http/, "ws");

let ws = null;
let connectPromise = null;

let listeners = {
  onopen: [],
  onclose: [],
  onerror: [],
  onmessage: {},
};

let registerCallback = (callbacks, callback) => {
  let index = callbacks.length;
  callbacks.push(callback);
  return () => callbacks.splice(index, 1);
};

let registerOnOpen = (listener) => registerCallback(listeners.onopen, listener);
let registerOnClose = (listener) =>
  registerCallback(listeners.onclose, listener);
// let registerOnError = listener => registerCallback(listeners.onerror, listener);

function Deferred() {
  // update 062115 for typeof
  if (typeof Promise != "undefined" && Promise.defer) {
    return Promise.defer();
  } else {
    var deferred = {};
    var promise = new Promise(function (resolve, reject) {
      deferred.resolve = resolve;
      deferred.reject = reject;
    });
    deferred.promise = promise;
    return deferred;
  }
}

let registerOnMessage = (channel) => (listener) => {
  let callbacks =
    listeners.onmessage[channel] || (listeners.onmessage[channel] = []);
  return registerCallback(callbacks, listener);
};

let notify = (callbacks) => (message) =>
  (callbacks || []).forEach((cb) => cb(message));

export function connect() {
  if (connectPromise) {
    return connectPromise;
  }

  let deferred = Deferred();
  let promise = deferred.promise;
  let cleanUp = () => (connectPromise = null);

  connectPromise = promise;
  connectPromise.then(cleanUp, cleanUp);

  let resolve = () => connectPromise && deferred.resolve();
  let reject = () => connectPromise && deferred.reject();

  if (!session.info) {
    reject();
    return promise;
  }

  if (ws && ws.readyState === WebSocket.OPEN) {
    resolve();
    return promise;
  }

  ws = new WebSocket(url);

  ws.onopen = (event) => {
    notify(listeners.onopen)(event);
    resolve();
  };

  ws.onclose = (event) => {
    notify(listeners.onclose)(event);
    reject();
  };

  ws.onerror = (event) => {
    notify(listeners.onerror)(event);
    reject();
  };

  ws.onmessage = (event) => {
    let message = JSON.parse(event.data);
    notify(listeners.onmessage[message.channel])(message.data);
  };

  return promise;
}

function send(type, channel, data) {
  return connect().then(() =>
    ws.send(JSON.stringify({ type: type, channel: channel, data: data }))
  );
}

export function Socket(name, opts = {}) {
  let openback = opts.onopen;
  let closeback = opts.onclose;

  let unopen = null;
  let unclose = null;

  let onUnsubscribed = (uncallback) => {
    if (unopen) unopen();
    if (unclose) unclose();
    uncallback && uncallback();
  };

  let onSubscribed = (callback) => {
    if (openback) unopen = unopen || registerOnOpen(openback);
    if (closeback) unclose = unclose || registerOnClose(closeback);
    return registerOnMessage(name)(callback);
  };

  return {
    subscribe(callback) {
      let uncallback = null;
      send("SUB", name).then(() => (uncallback = onSubscribed(callback)));
      return () =>
        send("UNS", name).then(
          () => onUnsubscribed(uncallback),
          () => onUnsubscribed(uncallback)
        );
    },
    send(data) {
      return send("MSG", name, data);
    },
    get state() {
      return ws ? ws.readyState : WebSocket.CLOSED;
    },
  };
}
