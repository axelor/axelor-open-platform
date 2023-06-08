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
  const [{ state, data, error }, setState] = useState<{
    state?: "loading" | "hasData" | "hasError";
    data?: T;
    error?: any;
  }>({});

  useAsyncEffect(async () => {
    setState((draft) => ({ ...draft, state: "loading" }));
    try {
      const data = await func();
      setState({ state: "hasData", data });
    } catch (e) {
      setState((draft) => ({ ...draft, state: "hasError", error: e }));
    }
  }, deps);

  return {
    state: state ?? "loading",
    data,
    error,
  };
}
