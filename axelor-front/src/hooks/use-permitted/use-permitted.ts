import { DataSource } from "@/services/client/data";
import { DataContext } from "@/services/client/data.types";
import { useCallback, useMemo } from "react";

export function usePermitted(target: string) {
  const dataSource = useMemo(() => new DataSource(target), [target]);

  const getPermAction = useCallback(
    (record?: DataContext | null, readonly?: boolean) => {
      if (readonly) return "read";
      return (record?.id ?? 0) > 0 ? "write" : "create";
    },
    [],
  );

  const isPermitted = useCallback(
    async (record?: DataContext | null, readonly?: boolean, silent?: boolean) =>
      await dataSource.isPermitted(
        getPermAction(record, readonly),
        record?.id,
        silent,
      ),
    [dataSource, getPermAction],
  );

  return isPermitted;
}
