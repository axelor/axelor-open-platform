import { DataContext } from "@/services/client/data.types";
import { Schema } from "@/services/client/meta.types";
import { useFormScope } from "@/views/form/builder/scope";
import { useCallback, useRef } from "react";

export function useBeforeSelect(
  schema: Schema,
  getContext?: () => DataContext | undefined,
): [
  (currentDomain?: string, force?: boolean) => Promise<any>,
  {
    onMenuOpen?: () => void;
    onMenuClose?: () => void;
  },
] {
  const { onSelect: onSelectAction } = schema;
  const { actionExecutor } = useFormScope();

  const beforeSelectRef = useRef<string | null>(null);
  const handleBeforeSelect = useCallback(
    async (currentDomain?: string, force = false) => {
      if ((force || beforeSelectRef.current === null) && onSelectAction) {
        const ctx = getContext?.();
        const opts = ctx ? { context: ctx } : undefined;
        const res = await actionExecutor.execute(onSelectAction, opts);
        if (res && res.length > 0) {
          const attrs = res[0].attrs || {};
          const domain = attrs[schema.name!]?.domain;
          if (domain !== undefined) {
            return (beforeSelectRef.current = domain);
          }
        }
      }
      return currentDomain;
    },
    [actionExecutor, getContext, onSelectAction, schema.name],
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
