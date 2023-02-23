import { DependencyList, useCallback, useEffect, useState } from "react";

export function useAsync<T>(
  func: () => Promise<T>,
  deps?: DependencyList
): {
  state: "loading" | "hasData" | "hasError";
  data?: T;
  error?: any;
} {
  const [state, setState] = useState<"loading" | "hasData" | "hasError">();
  const [data, setData] = useState<T>();
  const [error, setError] = useState<any>();

  const load = useCallback(async () => {
    setState("loading");
    try {
      const res = await func();
      setData(res);
      setState("hasData");
    } catch (e) {
      setError(e);
      setState("hasError");
    }
  }, deps ?? []);

  useEffect(() => {
    if (state) return;
    load();
  }, [load]);

  return {
    state: state ?? "loading",
    data,
    error,
  };
}
