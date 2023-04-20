import { SetStateAction, atom, useAtomValue, useSetAtom } from "jotai";
import { atomFamily, selectAtom, useAtomCallback } from "jotai/utils";
import { memo, useCallback, useMemo, useState } from "react";

import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useEditor, useSelector } from "@/hooks/use-relation";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { ViewData } from "@/services/client/meta";
import {
  Editor,
  FormView,
  Panel,
  Property,
  Schema,
} from "@/services/client/meta.types";

import { Form, useFormHandlers } from "./form";
import { FieldContainer } from "./form-field";
import { GridLayout } from "./form-layouts";
import { FormState, ValueAtom, WidgetAtom, WidgetProps } from "./types";
import { processView } from "./utils";

import { isEqual } from "lodash";
import styles from "./form-editors.module.scss";

export type FieldEditorProps = WidgetProps & { valueAtom: ValueAtom<any> };
export type FormEditorProps = FieldEditorProps & {
  editor: FormView;
  fields: Record<string, Property>;
};

function processEditor(schema: Schema) {
  const editor: Editor = schema.editor;
  const widgetAttrs = editor.widgetAttrs ?? {};
  const fields = editor.fields ?? schema.fields;

  const applyAttrs = (item: Schema) => {
    const result = { ...item };
    const field = fields?.[item.name!];

    result.showTitle = item.showTitle ?? widgetAttrs.showTitles !== "false";
    result.title = item.title ?? field?.title ?? field?.autoTitle ?? item.name;
    result.colSpan = item.colSpan ?? widgetAttrs.itemSpan;
    result.placeholder = item.placeholder ?? field?.placeholder ?? result.title;
    result.serverType = item.serverType ?? field?.type;

    if (result.selectionList) {
      result.widget = result.widget ?? "selection";
    }

    return result as Schema;
  };

  const items = editor.items?.map(applyAttrs) as Panel["items"];
  const hasColSpan = items?.some((x) => x.colSpan);
  const cols = hasColSpan ? 12 : items?.length;
  const colWidths = hasColSpan
    ? undefined
    : items?.map((x) => {
        const w = x.width ?? x.widgetAttrs?.width;
        return w ?? (x.widget === "toggle" ? "min-content" : "*");
      });

  const panel: Panel = {
    ...editor,
    type: "panel",
    items: hasColSpan ? items : items?.map((x) => ({ ...x, colSpan: 1 })),
    cols,
    colWidths,
    gap: editor.layout === "table" ? "0.25rem" : undefined,
    showTitle: false,
    showFrame: false,
  };

  const form: FormView = {
    type: "form",
    items: [panel],
    cols: 1,
  };

  return { form, fields };
}

export function FieldEditor(props: FieldEditorProps) {
  const { schema } = props;

  const fieldsAtom = useMemo(
    () => selectAtom(props.formAtom, (o) => o.fields),
    [props.formAtom]
  );

  const formFields = useAtomValue(fieldsAtom);

  const { form, fields } = useMemo(
    () => processEditor({ ...schema, fields: schema.fields ?? formFields }),
    [formFields, schema]
  );

  // reference field?
  if (schema.serverType?.endsWith("_TO_ONE")) {
    return <ReferenceEditor {...props} editor={form} fields={fields} />;
  }
  // collection field?
  if (schema.serverType?.endsWith("_TO_MANY")) {
    return <CollectionEditor {...props} editor={form} fields={fields} />;
  }

  return <SimpleEditor {...props} editor={form} fields={fields} />;
}

function SimpleEditor({
  editor,
  fields,
  formAtom,
  widgetAtom,
  readonly,
  ...rest
}: FormEditorProps) {
  const showTitle = rest.schema.showTitle ?? true;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;

  const schema = useMemo(() => processView(editor, fields), [editor, fields]);

  return (
    <FieldContainer readonly={readonly}>
      {showTitle && <label>{title}</label>}
      <GridLayout schema={schema} formAtom={formAtom} readonly={readonly} />
    </FieldContainer>
  );
}

function ReferenceEditor({
  schema,
  editor,
  fields,
  formAtom,
  widgetAtom,
  valueAtom,
  readonly,
}: FormEditorProps) {
  const showTitle = schema.showTitle ?? true;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;

  const model = schema.target!;

  const showEditor = useEditor();
  const showSelector = useSelector();

  const handleSelect = useAtomCallback(
    useCallback(
      async (get, set) => {
        showSelector({
          model,
          title: `Select ${title}`,
          onSelect: (records) => {
            set(valueAtom, records[0], true);
          },
        });
      },
      [model, showSelector, title, valueAtom]
    )
  );

  const handleEdit = useAtomCallback(
    useCallback(
      (get, set) => {
        showEditor({
          model,
          title: title ?? "",
          onSelect: (record) => {
            set(valueAtom, record, true);
          },
          record: get(valueAtom),
          readonly,
        });
      },
      [model, readonly, showEditor, title, valueAtom]
    )
  );

  const handleDelete = useAtomCallback(
    useCallback(
      (get, set) => {
        set(valueAtom, null, true);
      },
      [valueAtom]
    )
  );

  const setInvalid = useSetAtom(setInvalidAtom);
  const handleInvalid = useCallback(
    (value: any, invalid: boolean) => {
      setInvalid(widgetAtom, invalid);
    },
    [setInvalid, widgetAtom]
  );

  return (
    <FieldContainer readonly={readonly}>
      <div className={styles.header}>
        <div className={styles.title}>
          {showTitle && <label>{title}</label>}
        </div>
        {readonly && (
          <div className={styles.actions}>
            <MaterialIcon icon="edit" onClick={handleEdit} />
          </div>
        )}
        {readonly || (
          <div className={styles.actions}>
            <MaterialIcon icon="edit" onClick={handleEdit} />
            <MaterialIcon icon="search" onClick={handleSelect} />
            <MaterialIcon icon="delete" onClick={handleDelete} />
          </div>
        )}
      </div>
      <RecordEditor
        schema={schema}
        editor={editor}
        fields={fields}
        formAtom={formAtom}
        widgetAtom={widgetAtom}
        valueAtom={valueAtom}
        model={model}
        readonly={readonly}
        setInvalid={handleInvalid}
      />
    </FieldContainer>
  );
}

const nextId = (() => {
  let id = 0;
  return () => --id;
})();

function CollectionEditor({
  schema,
  editor,
  fields,
  formAtom,
  widgetAtom,
  valueAtom,
  readonly,
}: FormEditorProps) {
  const showTitle = schema.showTitle ?? true;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;

  const model = schema.target!;

  const exclusive = useMemo(() => {
    const panel: Schema = editor.items?.[0] ?? {};
    const items = panel.items ?? [];
    return items.find((x) => x.exclusive)?.name;
  }, [editor]);

  const itemsAtom = useMemo(() => {
    return atom(
      (get) => (get(valueAtom) ?? []) as DataRecord[],
      (get, set, update: SetStateAction<DataRecord[]>) => {
        set(valueAtom, update);
      }
    );
  }, [valueAtom]);

  const itemsFamily = useMemo(() => {
    return atomFamily(
      (record: DataRecord) =>
        atom(
          (get) => get(valueAtom)?.find((x: DataRecord) => x.id === record.id),
          (get, set, value: DataRecord) => {
            set(valueAtom, (items: DataRecord[] = []) => {
              let result = items;
              let found = items.find((x) => x.id === value.id);
              if (found) {
                result = items.map((x) => (x.id === value.id ? value : x));
              }
              if (exclusive && found && value[exclusive]) {
                result = result.map((item) => ({
                  ...item,
                  [exclusive]: item.id === value.id ? value[exclusive] : false,
                }));
              }
              return result;
            });
          }
        ),
      (a, b) => a.id === b.id
    );
  }, [valueAtom, exclusive]);

  const items = useAtomValue(itemsAtom);

  const addNew = useAtomCallback(
    useCallback(
      (get, set) => {
        const record = { id: nextId() };
        itemsFamily(record);
        set(itemsAtom, (items = []) => [...items, record]);
      },
      [itemsAtom, itemsFamily]
    )
  );

  const remove = useAtomCallback(
    useCallback(
      (get, set, record: DataRecord) => {
        itemsFamily.remove(record);
        set(itemsAtom, (items) => items.filter((x) => x.id !== record.id));
      },
      [itemsAtom, itemsFamily]
    )
  );

  const [errors, setErrors] = useState<Record<string, boolean>>({});
  const setInvalid = useSetAtom(setInvalidAtom);
  const handleInvalid = useCallback((value: DataRecord, invalid: boolean) => {
    setErrors((errors) => ({ ...errors, [value.id!]: invalid }));
  }, []);

  useAsyncEffect(async () => {
    const invalid = items.map((x) => errors[x.id!]).some((x) => x);
    setInvalid(widgetAtom, invalid);
  });

  return (
    <div className={styles.collection}>
      <div className={styles.header}>
        <div className={styles.title}>
          {showTitle && <label>{title}</label>}
        </div>
      </div>
      <div className={styles.items}>
        {items.map((item) => (
          <ItemEditor
            key={item.id}
            schema={schema}
            model={model}
            editor={editor}
            fields={fields}
            formAtom={formAtom}
            widgetAtom={widgetAtom}
            valueAtom={itemsFamily(item)}
            remove={remove}
            readonly={readonly}
            setInvalid={handleInvalid}
          />
        ))}
      </div>
      {readonly || (
        <div className={styles.actions}>
          <MaterialIcon icon="add" onClick={addNew} />
        </div>
      )}
    </div>
  );
}

const ItemEditor = memo(function ItemEditor({
  remove,
  readonly,
  setInvalid,
  ...props
}: FormEditorProps & {
  model: string;
  remove: (record: DataRecord) => void;
  setInvalid: (value: DataRecord, invalid: boolean) => void;
}) {
  const valueAtom = props.valueAtom;
  const handleRemove = useAtomCallback(
    useCallback(
      (get) => {
        remove(get(valueAtom));
      },
      [remove, valueAtom]
    )
  );
  return (
    <div className={styles.item}>
      <RecordEditor {...props} readonly={readonly} setInvalid={setInvalid} />
      {readonly || (
        <div className={styles.actions}>
          <MaterialIcon icon="close" onClick={handleRemove} />
        </div>
      )}
    </div>
  );
});

const setInvalidAtom = atom(
  null,
  (get, set, widgetAtom: WidgetAtom, invalid: boolean) => {
    const prev = get(widgetAtom);
    const errors = invalid
      ? {
          invalid: i18n.get("{0} is invalid", prev.attrs.title),
        }
      : {};
    if (isEqual(errors, prev.errors ?? {})) return;
    set(widgetAtom, { ...prev, errors });
  }
);

const RecordEditor = memo(function RecordEditor({
  model,
  editor,
  fields,
  formAtom: parent,
  widgetAtom,
  valueAtom,
  readonly,
  setInvalid,
}: FormEditorProps & {
  model: string;
  setInvalid: (value: DataRecord, invalid: boolean) => void;
}) {
  const meta: ViewData<any> = useMemo(
    () => ({
      model,
      fields,
      view: editor,
    }),
    [editor, fields, model]
  );

  const record = useMemo(() => ({}), []);
  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, record, parent);

  const editorAtom = useMemo(() => {
    return atom(
      (get) => {
        const record = get(valueAtom) || {};
        const state = get(formAtom);
        return {
          ...state,
          record,
        };
      },
      (get, set, update: SetStateAction<FormState>) => {
        const state =
          typeof update === "function" ? update(get(formAtom)) : update;
        const { record } = state;

        set(formAtom, state);
        set(valueAtom, record);
      }
    );
  }, [formAtom, valueAtom]);

  const invalidAtom = useMemo(
    () =>
      selectAtom(editorAtom, (state) =>
        Object.values(state.states).some(
          (x) => x.errors && Object.keys(x.errors).length > 0
        )
      ),
    [editorAtom]
  );

  const invalid = useAtomValue(invalidAtom);
  const invalidCheck = useAtomCallback(
    useCallback(
      (get, set) => {
        setInvalid(get(valueAtom), invalid);
      },
      [invalid, setInvalid, valueAtom]
    )
  );

  useAsyncEffect(async () => invalidCheck(), [invalidCheck]);

  return (
    <Form
      schema={editor}
      recordHandler={recordHandler}
      actionExecutor={actionExecutor}
      actionHandler={actionHandler}
      fields={fields}
      formAtom={editorAtom}
      widgetAtom={widgetAtom}
      readonly={readonly}
    />
  );
});
