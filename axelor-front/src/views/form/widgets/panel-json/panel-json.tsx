import { useMemo } from "react";

import { Schema } from "@/services/client/meta.types";

import { GridLayout, WidgetProps } from "../../builder";
import { toTopLevelItem } from "@/services/client/meta-utils";

function processEditor(schema: Schema) {
  const items = schema.items?.map((item) =>
    toTopLevelItem({
      colSpan: 12,
      ...item,
    }),
  );

  return { ...schema, items, layout: undefined, flexbox: undefined };
}

function processItem(schema: Schema) {
  const editor = processEditor(schema.editor);
  return { ...schema, editor };
}

export function PanelJson(props: WidgetProps) {
  const { formAtom, widgetAtom, readonly } = props;

  const schema = useMemo(() => {
    const schema = props.schema;
    const items = schema.items?.map(processItem);
    return {
      ...schema,
      items,
    };
  }, [props.schema]);

  return (
    <GridLayout
      readonly={readonly}
      formAtom={formAtom}
      parentAtom={widgetAtom}
      schema={schema}
    />
  );
}
