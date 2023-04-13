import { useAtomValue } from "jotai";
import { useMemo } from "react";

import { ViewData } from "@/services/client/meta";
import { Editor, Schema } from "@/services/client/meta.types";

import { Form, useFormHandlers } from "./form";
import { FieldContainer } from "./form-field";
import { GridLayout } from "./form-layouts";
import { ValueAtom, WidgetProps } from "./types";
import { processView } from "./utils";

export type EditorProps = WidgetProps & { valueAtom: ValueAtom<any> };

function processEditor(schema: Schema) {
  const editor: Editor = schema.editor;
  const widgetAttrs = editor.widgetAttrs ?? {};
  const fields = editor.fields ?? schema.fields;

  const applyAttrs = (item: Schema) => {
    const result = { ...item };
    const field = fields?.[item.name!];

    result.showTitle = item.showTitle ?? widgetAttrs.showTitles !== "false";
    result.title = item.title ?? field?.title ?? field?.autoTitle;
    result.colSpan = item.colSpan ?? widgetAttrs.itemSpan;
    result.placeholder = item.placeholder ?? field?.placeholder ?? result.title;
    result.serverType = item.serverType ?? field?.type;

    return result as Schema;
  };

  const items = editor.items?.map(applyAttrs);
  const hasColSpan = items?.some((x) => x.colSpan);
  const cols = hasColSpan ? 12 : items?.length;
  const colWidths = hasColSpan
    ? undefined
    : items?.map((x) => {
        const w = x.width ?? x.widgetAttrs?.width;
        return w ?? (x.widget === "toggle" ? 38 : "*");
      });

  return {
    ...editor,
    items: hasColSpan ? items : items?.map((x) => ({ ...x, colSpan: 1 })),
    cols,
    colWidths,
    gap: editor.layout === "table" ? "0.25rem" : undefined,
  } as Editor;
}

export function FieldEditor(props: EditorProps) {
  const { schema } = props;
  const { fields } = useAtomValue(props.formAtom);

  const editor = useMemo(
    () => processEditor({ ...schema, fields: schema.fields ?? fields }),
    [fields, schema]
  );

  // reference field?
  if (schema.serverType?.endsWith("_TO_ONE")) {
    return <ReferenceEditor {...props} editor={editor} />;
  }
  // collection field?
  if (schema.serverType?.endsWith("_TO_MANY")) {
    return <CollectionEditor {...props} editor={editor} />;
  }

  return <SimpleEditor {...props} editor={editor} />;
}

function SimpleEditor({
  editor,
  formAtom,
  widgetAtom,
  ...rest
}: EditorProps & { editor: Editor }) {
  const showTitle = rest.schema.showTitle ?? true;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;

  const formState = useAtomValue(formAtom);
  const fields = useMemo(
    () => editor.fields ?? formState.fields ?? {},
    [editor.fields, formState.fields]
  );
  const schema = useMemo(() => processView(editor, fields), [editor, fields]);

  return (
    <FieldContainer>
      {showTitle && <label>{title}</label>}
      <GridLayout schema={schema} formAtom={formAtom} />
    </FieldContainer>
  );
}

function ReferenceEditor({
  schema,
  editor,
  formAtom,
  widgetAtom,
  valueAtom,
}: EditorProps & { editor: Editor }) {
  return null;
}

function CollectionEditor({
  schema,
  editor,
  formAtom,
  widgetAtom,
  valueAtom,
}: EditorProps & { editor: Editor }) {
  return null;
}
