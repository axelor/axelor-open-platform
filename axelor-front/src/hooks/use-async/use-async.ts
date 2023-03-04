import { DependencyList, useState } from "react";
import { useAsyncEffect } from "../use-async-effect";

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

  useAsyncEffect(async () => {
    setState("loading");
    try {
      const res = await func();
      setData(res);
      setState("hasData");
    } catch (e) {
      setError(e);
      setState("hasError");
    }
  }, deps);

  return {
    state: state ?? "loading",
    data,
    error,
  };
}
