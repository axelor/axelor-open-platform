import { DataStore } from "@/services/client/data-store";
import { useEffect, useState } from "react";

export function useDataStore<T>(
  store: DataStore,
  selector: (store: DataStore) => T
): T {
  const [state, setState] = useState<T>(selector(store));
  useEffect(() => {
    return store.subscribe(() => {
      setState(() => selector(store));
    });
  }, [store, selector]);
  return state;
}
