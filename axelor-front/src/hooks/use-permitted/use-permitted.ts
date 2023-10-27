import { AccessType, DataSource } from "@/services/client/data";
import { DataContext } from "@/services/client/data.types";
import { Perms } from "@/services/client/meta.types";
import { useCallback, useMemo } from "react";

export function usePermitted(target: string, perms?: Perms) {
  const dataSource = useMemo(() => new DataSource(target), [target]);

  const getPermAction = useCallback(
    (record?: DataContext | null, readonly?: boolean): AccessType => {
      if (readonly) return "read";
      return (record?.id ?? 0) > 0 ? "write" : "create";
    },
    [],
  );

  const isPermitted = useCallback(
    async (
      record?: DataContext | null,
      readonly?: boolean,
      silent?: boolean,
    ) => {
      const action = getPermAction(record, readonly);

      if (action === "create") {
        return perms?.create !== false;
      }

      return await dataSource.isPermitted(action, record?.id, silent);
    },
    [dataSource, getPermAction, perms],
  );

  return isPermitted;
}
