import { atom, getDefaultStore, useAtom } from "jotai";
import { SetStateAction } from "react";

const countAtom = atom(0);
const blockAtom = atom(
  (get) => get(countAtom) > 0,
  (get, set, state: SetStateAction<boolean>) => {
    const block = typeof state === "function" ? state(get(blockAtom)) : state;
    set(countAtom, (count) => (block ? count + 1 : count - 1));
  },
);

function wait(ms?: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function block<T>(
  task: () => T,
  ms?: number,
): Promise<Awaited<T>> {
  const store = getDefaultStore();
  try {
    store.set(blockAtom, true);
    await wait(ms);
    return await task();
  } finally {
    store.set(blockAtom, false);
  }
}

export function useHttpBlock() {
  return useAtom(blockAtom);
}
