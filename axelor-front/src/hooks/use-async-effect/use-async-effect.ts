import { DependencyList, EffectCallback, useEffect, useRef } from "react";

type AsyncEffectCallbackResult = ReturnType<EffectCallback>;
type AsyncEffectCallback = (
  signal: AbortSignal,
) => Promise<AsyncEffectCallbackResult>;

export function useAsyncEffect(
  effect: AsyncEffectCallback,
  deps?: DependencyList,
) {
  const abortRef = useRef<AbortController>(undefined);

  useEffect(() => {
    abortRef.current = new AbortController();
    return () => {
      abortRef.current?.abort();
    };
  }, []);

  useEffect(() => {
    let canceled = false;
    let result: AsyncEffectCallbackResult;

    const load = async () => {
      const signal = abortRef.current!.signal;
      if (canceled || signal.aborted) {
        return;
      }
      result = await effect(signal);
    };

    queueMicrotask(load);

    return () => {
      canceled = true;
      if (result) {
        result();
      }
    };
  }, deps); // eslint-disable-line react-hooks/exhaustive-deps
}
