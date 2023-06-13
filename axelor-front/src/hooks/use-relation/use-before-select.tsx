import { Schema } from "@/services/client/meta.types";
import { useFormScope } from "@/views/form/builder/scope";
import { useCallback, useRef } from "react";

export function useBeforeSelect(schema: Schema): [
  (force?: boolean) => Promise<any>,
  {
    onMenuOpen?: () => void;
    onMenuClose?: () => void;
  }
] {
  const { onSelect: onSelectAction } = schema;
  const { actionExecutor } = useFormScope();

  const beforeSelectRef = useRef<string | null>(null);
  const handleBeforeSelect = useCallback(
    async (force = false) => {
      if ((force || beforeSelectRef.current === null) && onSelectAction) {
        const res = await actionExecutor.execute(onSelectAction);
        if (res && res.length > 0) {
          const attrs = res[0].attrs || {};
          const domain = attrs[schema.name!]?.domain ?? null;
          beforeSelectRef.current = domain;
          return domain;
        }
      }
    },
    [actionExecutor, onSelectAction, schema.name]
  );

  const reset = useCallback(() => {
    beforeSelectRef.current = null;
  }, []);

  return [
    handleBeforeSelect,
    {
      onMenuOpen: reset,
      onMenuClose: reset,
    },
  ];
}
