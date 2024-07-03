import { useMemo } from "react";
import { createScriptContext } from "@/hooks/use-parser/context";
import { parseExpression } from "@/hooks/use-parser/utils";
import { Field } from "@/services/client/meta.types";
import { useViewAction } from "@/view-containers/views/scope";
import { DataRecord } from "@/services/client/data.types";

export function useButtonProps(field: Field, record: DataRecord) {
  const { context } = useViewAction();
  return useMemo(() => {
    const { showIf, hideIf, readonlyIf } = field;
    const ctx = createScriptContext({ ...context, ...record });

    let { hidden, readonly } = field;

    if (showIf) {
      hidden = !parseExpression(showIf)(ctx);
    } else if (hideIf) {
      hidden = !!parseExpression(hideIf)(ctx);
    }

    if (readonlyIf) {
      readonly = !!parseExpression(readonlyIf)(ctx);
    }

    return { hidden, readonly };
  }, [field, record, context]);
}
