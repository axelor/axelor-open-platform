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
      if (canceled || signal.aborted) {
        return;
      }
      result = await effect(signal);
    };

    const timer = setTimeout(load);

    return () => {
      clearTimeout(timer);
      canceled = true;
      if (result) {
        result();
      }
    };
  }, deps); // eslint-disable-line react-hooks/exhaustive-deps
}
