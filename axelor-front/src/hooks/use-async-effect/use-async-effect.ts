import { DependencyList, EffectCallback, useEffect, useRef } from "react";

type AsyncEffectCallbackResult = ReturnType<EffectCallback>;
type AsyncEffectCallback = (
  signal: AbortSignal
) => Promise<AsyncEffectCallbackResult>;

export function useAsyncEffect(
  effect: AsyncEffectCallback,
  deps?: DependencyList
) {
  const abortRef = useRef<AbortController>();

  useEffect(() => {
    abortRef.current = new AbortController();
    return () => {
      abortRef.current?.abort();
    };
  }, []);

  useEffect(() => {
    let canceled = false;
    let signal = abortRef.current!.signal;
    let result: AsyncEffectCallbackResult;
    let load = async () => {
      await Promise.resolve(); // wait for clean up in strict mode
      if (canceled || signal.aborted) {
        return;
      }
      result = await effect(signal);
    };

    load();

    return () => {
      canceled = true;
      if (result) {
        result();
      }
    };
  }, deps); // eslint-disable-line react-hooks/exhaustive-deps
}
