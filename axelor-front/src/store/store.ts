import { useSyncExternalStore } from "react";

function isFunction(value: any): value is Function {
  return typeof value === "function";
}

export type Listener<T> = (state: T, prevState: T) => void;
export type Updater<T> = (state: T) => T;
export type Value<T> = T | Updater<T>;

export type Get<T> = () => T;
export type Set<T> = (value: Value<T>) => void;

export type Subscribe<T> = (listener: Listener<T>) => Unsubscribe;
export type Unsubscribe = () => void;

export type Store<T> = {
  get: Get<T>;
  set: Set<T>;
  subscribe: Subscribe<T>;
};

export function createStore<T>(initialState: T): Store<T> {
  let state: T = initialState;
  let listeners = new Set<Listener<T>>();

  const subscribe: Subscribe<T> = (listener) => {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  };

  const get: Get<T> = () => state;
  const set: Set<T> = (value) => {
    const prevState = state;
    const nextState = isFunction(value) ? value(prevState) : value;
    if (nextState !== prevState) {
      state = nextState;
      listeners.forEach((fn) => fn(state, prevState));
    }
  };

  return {
    get,
    set,
    subscribe,
  };
}

export function useStore<T>(store: Store<T>) {
  return useSyncExternalStore(store.subscribe, store.get);
}
