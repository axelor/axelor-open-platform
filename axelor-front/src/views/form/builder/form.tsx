import { atom } from "jotai";
import { useMemo } from "react";

import { DataRecord } from "@/services/client/data.types";

import { GridLayout } from "./form-layouts";
import { FormProps, FormState, WidgetState } from "./types";
import { processView } from "./utils";

export function Form({
  meta,
  className,
  readonly,
  layout: Layout = GridLayout,
}: FormProps) {
  const { fields = {}, view } = meta;

  const schema = useMemo(() => processView(view, fields), [view, fields]);

  const formAtom = useMemo(() => {
    const states: Record<string, WidgetState> = {};
    const record: DataRecord = {};
    return atom<FormState>({ record, states, fields: fields });
  }, [fields]);

  return (
    <Layout
      className={className}
      readonly={readonly}
      schema={schema}
      formAtom={formAtom}
    />
  );
}
