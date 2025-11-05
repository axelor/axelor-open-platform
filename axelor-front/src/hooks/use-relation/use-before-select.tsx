import deepEqual from "lodash/isEqual";
import { useCallback, useRef } from "react";

import { DataContext } from "@/services/client/data.types";
import type { ActionResult } from "@/services/client/meta";
import { Schema } from "@/services/client/meta.types";
import { useFormScope } from "@/views/form/builder/scope";

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

  const lastCtxRef = useRef<DataContext | undefined>(undefined);
  const actionPromiseRef = useRef<Promise<void | ActionResult[]>>(undefined);

  const handleBeforeSelect = useCallback(
    async (currentDomain?: string, force = false) => {
      const ctx = getContext?.();
      if (onSelectAction && (force || !deepEqual(lastCtxRef.current, ctx))) {
        const opts = ctx ? { context: ctx } : undefined;
        actionPromiseRef.current ??= actionExecutor.execute(
          onSelectAction,
          opts,
        );

        try {
          const res = await actionPromiseRef.current;
          if (res && res.length > 0) {
            const attrs = res[0].attrs || {};
            const domain = attrs[schema.name!]?.domain;
            if (domain !== undefined) {
              return domain;
            }
          }
        } finally {
          actionPromiseRef.current = undefined;
          lastCtxRef.current = ctx;
        }
      }
      return currentDomain;
    },
    [actionExecutor, getContext, onSelectAction, schema.name],
  );

  const reset = useCallback(() => {
    lastCtxRef.current = undefined;
  }, []);

  return [
    handleBeforeSelect,
    {
      onMenuOpen: reset,
      onMenuClose: reset,
    },
  ];
}
