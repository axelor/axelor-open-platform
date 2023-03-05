import { DependencyList, useRef, useState } from "react";
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
  const [error, setError] = useState<any>();
  const dataRef = useRef<T>();

  useAsyncEffect(async () => {
    setState("loading");
    try {
      const res = await func();
      dataRef.current = res;
      setState("hasData");
    } catch (e) {
      setError(e);
      setState("hasError");
    }
  }, deps);

  return {
    state: state ?? "loading",
    data: dataRef.current,
    error,
  };
}
