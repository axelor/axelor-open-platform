import { SetStateAction, atom, useAtomValue, useSetAtom } from "jotai";
import { atomFamily, selectAtom, useAtomCallback } from "jotai/utils";
import { memo, useCallback, useEffect, useMemo, useState } from "react";

import { MaterialIcon } from "@axelor/ui/icons/material-icon";

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

import { Form, useFormHandlers, usePermission } from "./form";
import { FieldControl } from "./form-field";
import { GridLayout } from "./form-layouts";
import { FieldProps, FormState, WidgetAtom } from "./types";
import { getDefaultValues, nextId, processView } from "./utils";

import { DataStore } from "@/services/client/data-store";
import { toKebabCase, toSnakeCase } from "@/utils/names";
import { isEqual } from "lodash";
import { isDefaultRecord, useGetErrors } from "../form";
import styles from "./form-editors.module.scss";

export type FieldEditorProps = FieldProps<any>;
export type FormEditorProps = FieldEditorProps & {
  editor: FormView;
  fields: Record<string, Property>;
};

function processEditor(schema: Schema) {
  const editor: Editor = schema.editor;
  const widgetAttrs = editor.widgetAttrs ?? {};
  const fields = editor.fields ?? schema.fields;
  const flexbox = editor.flexbox ?? false;

  const applyTitle = (item: Schema) => {
    const field = fields?.[item.name!];
    const result = { ...field, ...item };
    result.showTitle = item.showTitle ?? widgetAttrs.showTitles !== "false";
    result.title = item.title ?? field?.title ?? field?.autoTitle;
    result.placeholder = item.placeholder ?? field?.placeholder ?? result.title;

    if (result.items) {
      result.items = result.items.map((item: Schema) =>
        applyTitle({ ...item })
      );
    }
    return result;
  };

  const applyAttrs = (item: Schema) => {
    const result = applyTitle(item);
    const field = fields?.[item.name!];

    result.colSpan = item.colSpan ?? widgetAttrs.itemSpan;
    result.serverType = item.serverType ?? field?.type;

    if (result.selectionList) {
      result.widget = result.widget ?? "selection";
    }

    return result as Schema;
  };

  const items = editor.items?.map((item) =>
    applyAttrs({ ...item })
  ) as Panel["items"];
  const hasColSpan = flexbox || items?.some((x) => x.colSpan);
  const cols = hasColSpan ? 12 : items?.length;
  const colWidths = hasColSpan
    ? undefined
    : items
        ?.map((x) => {
          const w = x.width ?? x.widgetAttrs?.width;
          return w ?? (x.widget === "toggle" ? "min-content" : "*");
        })
        .join(",");

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
    () =>
      schema.json
        ? atom(schema.jsonFields)
        : selectAtom(props.formAtom, (o) => o.fields),
    [props.formAtom, schema.json, schema.jsonFields]
  );

  const formFields = useAtomValue(fieldsAtom);

  const { form, fields } = useMemo(
    () => processEditor({ ...schema, fields: schema.fields ?? formFields }),
    [formFields, schema]
  );

  // json field?
  if (schema.json) {
    return <JsonEditor {...props} editor={form} fields={fields} />;
  }

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

function SimpleEditor({ editor, fields, ...props }: FormEditorProps) {
  const { formAtom, widgetAtom, readonly } = props;
  const schema = useMemo(() => processView(editor, fields), [editor, fields]);

  return (
    <FieldControl {...props}>
      <GridLayout
        schema={schema}
        formAtom={formAtom}
        parentAtom={widgetAtom}
        readonly={readonly}
      />
    </FieldControl>
  );
}

function ReferenceEditor({ editor, fields, ...props }: FormEditorProps) {
  const { schema, formAtom, widgetAtom, valueAtom, readonly } = props;
  const { attrs } = useAtomValue(widgetAtom);
  const {
    title,
    domain,
    required,
    canEdit,
    canView = true,
    canSelect = true,
  } = attrs;

  const model = schema.target!;

  const showEditor = useEditor();
  const showSelector = useSelector();

  const hasValue = useAtomValue(
    useMemo(() => atom((get) => Boolean(get(valueAtom))), [valueAtom])
  );

  const icons: boolean | string[] = useMemo(() => {
    const showIcons = String(schema.showIcons || "");
    if (!showIcons || showIcons === "true") return true;
    if (showIcons === "false") return false;
    return showIcons.split(",");
  }, [schema]);

  const canShowIcon = useCallback(
    (icon: string) => {
      if (!icons) return false;
      return icons === true || icons?.includes?.(icon);
    },
    [icons]
  );

  const handleSelect = useAtomCallback(
    useCallback(
      async (get, set) => {
        showSelector({
          model,
          title: i18n.get("Select {0}", title ?? ""),
          domain,
          context: get(formAtom).record,
          onSelect: (records) => {
            set(valueAtom, records[0], true);
          },
        });
      },
      [domain, formAtom, model, showSelector, title, valueAtom]
    )
  );

  const handleEdit = useAtomCallback(
    useCallback(
      (get, set, readonly: boolean = false) => {
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
      [model, showEditor, title, valueAtom]
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
      if (required || value) {
        setInvalid(widgetAtom, invalid);
      }
    },
    [required, setInvalid, widgetAtom]
  );

  const titleActions = !readonly && (
    <div className={styles.actions}>
      {canEdit && hasValue && canShowIcon("edit") && (
        <MaterialIcon icon="edit" onClick={() => handleEdit(false)} />
      )}
      {canView && !canEdit && hasValue && canShowIcon("view") && (
        <MaterialIcon icon="description" onClick={() => handleEdit(true)} />
      )}
      {canSelect && canShowIcon("select") && (
        <MaterialIcon icon="search" onClick={handleSelect} />
      )}
      {hasValue && canShowIcon("clear") && (
        <MaterialIcon icon="delete" onClick={handleDelete} />
      )}
    </div>
  );

  return (
    <FieldControl {...props} titleActions={titleActions}>
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
    </FieldControl>
  );
}

function CollectionEditor({ editor, fields, ...props }: FormEditorProps) {
  const { schema, formAtom, widgetAtom, valueAtom, readonly } = props;
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

  const { hasButton } = usePermission(schema, widgetAtom);
  const canNew = !readonly && hasButton("new");

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
    <FieldControl {...props}>
      <div className={styles.collection}>
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
        {canNew && (
          <div className={styles.actions}>
            <MaterialIcon icon="add" onClick={addNew} />
          </div>
        )}
      </div>
    </FieldControl>
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

const EMPTY_RECORD = Object.freeze({});

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

  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, EMPTY_RECORD, parent);

  const [loaded, setLoaded] = useState<DataRecord>({});

  const editorAtom = useMemo(() => {
    return atom(
      (get) => {
        const value = get(valueAtom) || EMPTY_RECORD;
        const state = get(formAtom);
        const dirty = get(parent).dirty;
        const record = loaded.id && loaded.id === value.id ? loaded : value;
        return {
          ...state,
          dirty,
          record: {
            ...record,
            ...value,
          },
        };
      },
      (get, set, update: SetStateAction<FormState>) => {
        const state =
          typeof update === "function" ? update(get(formAtom)) : update;
        const { record } = state;

        set(formAtom, state);
        if (state.dirty) {
          set(valueAtom, isEqual(record, EMPTY_RECORD) ? null : record);
        }
      }
    );
  }, [formAtom, loaded, parent, valueAtom]);

  const getErrors = useGetErrors();

  const invalidAtom = useMemo(
    () => selectAtom(editorAtom, (state) => getErrors(state) !== null),
    [editorAtom, getErrors]
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

  const ds = useMemo(() => new DataStore(model), [model]);
  const value = useAtomValue(valueAtom);
  const load = useAtomCallback(
    useCallback(
      async (get, set) => {
        const id = value?.id ?? 0;
        if (id <= 0) return;
        const names = Object.keys(fields ?? {});
        const missing = names.some((x) => !Object.hasOwn(value, x));
        if (missing) {
          const rec = await ds.read(id, { fields: names });
          setLoaded(rec);
        }
      },
      [ds, fields, value]
    )
  );

  useAsyncEffect(async () => invalidCheck(), [invalidCheck]);
  useAsyncEffect(async () => load(), [load]);

  const parentState = useAtomValue(parent);
  const parentRecord = parentState.record;

  const setJsonDefaults = useAtomCallback(
    useCallback(
      (get, set) => {
        if (
          editor.json &&
          !isDefaultRecord(parentRecord) &&
          (parentRecord.id ?? 0) <= 0
        ) {
          set(valueAtom, { ...getDefaultValues(fields), ...get(valueAtom) });
        }
      },
      [editor.json, parentRecord, fields, valueAtom]
    )
  );

  useEffect(setJsonDefaults, [setJsonDefaults]);

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

function JsonEditor({
  schema,
  editor,
  fields,
  formAtom,
  widgetAtom,
  valueAtom,
  readonly,
}: FormEditorProps) {
  const modelAtom = useMemo(
    () => selectAtom(formAtom, (x) => x.model),
    [formAtom]
  );
  const model = useAtomValue(modelAtom);
  const jsonModel = schema.jsonModel;

  const jsonAtom = useMemo(() => {
    return atom(
      (get) => {
        const value = get(valueAtom) || "{}";
        return JSON.parse(value);
      },
      (get, set, update: SetStateAction<any>) => {
        const state =
          typeof update === "function" ? update(get(valueAtom)) : update;
        const record = state ? compactJson(state) : state;
        set(valueAtom, state ? JSON.stringify(record) : null);

        if (jsonModel) {
          const formState = get(formAtom);
          if (formState.record.jsonModel !== jsonModel) {
            set(formAtom, {
              ...formState,
              record: { ...formState.record, jsonModel },
            });
          }
        }
      }
    );
  }, [formAtom, jsonModel, valueAtom]);

  const jsonEditor = useMemo(
    () => ({ ...processJsonView(editor), json: true } as FormView),
    [editor]
  );

  const setInvalid = useSetAtom(setInvalidAtom);
  const handleInvalid = useCallback(
    (value: any, invalid: boolean) => {
      setInvalid(widgetAtom, invalid);
    },
    [setInvalid, widgetAtom]
  );

  return (
    <RecordEditor
      model={model}
      schema={schema}
      editor={jsonEditor}
      fields={fields}
      formAtom={formAtom}
      widgetAtom={widgetAtom}
      valueAtom={jsonAtom}
      setInvalid={handleInvalid}
      readonly={readonly}
    />
  );
}

function processJsonView(schema: Schema) {
  const result = { ...schema };
  if (schema.serverType) {
    result.type = "field";
    result.widget = toKebabCase(schema.serverType);
    result.serverType = toSnakeCase(schema.serverType).toUpperCase();
  }

  if (Array.isArray(result.items)) {
    result.items = result.items.map(processJsonView);
  }

  return result;
}

function compactJson(record: DataRecord) {
  const rec: DataRecord = {};
  Object.entries(record).forEach(([k, v]) => {
    if (k.indexOf("$") === 0 || v === null || v === undefined) return;
    if (typeof v === "string" && v.trim() === "") return;
    if (Array.isArray(v)) {
      if (v.length === 0) return;
      v = v.map(function (x) {
        return x.id ? { id: x.id } : x;
      });
    }
    rec[k] = v;
  });
  return rec;
}
