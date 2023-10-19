import clsx from "clsx";
import { SetStateAction, atom, useAtomValue, useSetAtom } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { atomFamily, selectAtom, useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react";

import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useEditor, useSelector } from "@/hooks/use-relation";
import { DataStore } from "@/services/client/data-store";
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
import { toKebabCase, toSnakeCase } from "@/utils/names";
import { MetaScope } from "@/view-containers/views/scope";

import { useGetErrors } from "../form";
import { createFormAtom } from "./atoms";
import { Form, useFormHandlers, usePermission } from "./form";
import { FieldControl } from "./form-field";
import { GridLayout } from "./form-layouts";
import { useAfterActions } from "./scope";
import {
  FieldProps,
  FormState,
  ValueAtom,
  WidgetAtom,
  WidgetState,
} from "./types";
import { nextId, processView } from "./utils";

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

    if (
      result.type !== "panel" &&
      result.type !== "separator" &&
      result.type !== "button" &&
      result.type !== "label"
    ) {
      result.showTitle = item.showTitle ?? widgetAttrs.showTitles !== "false";
    }
    result.title =
      item.title ?? field?.title ?? (result.showTitle ? field?.autoTitle : "");

    if (!result.showTitle && !result.items) {
      result.placeholder =
        result.placeholder ??
        field?.placeholder ??
        result.title ??
        field?.title;
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

    if (result.items) {
      result.items = result.items.map((item: Schema) =>
        applyAttrs({ ...item }),
      );
    }

    return result as Schema;
  };

  const items = editor.items?.map((item) =>
    applyAttrs({ ...item }),
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
    [props.formAtom, schema.json, schema.jsonFields],
  );

  const formFields = useAtomValue(fieldsAtom);

  const { form, fields } = useMemo(
    () => processEditor({ ...schema, fields: schema.fields ?? formFields }),
    [formFields, schema],
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
  const {
    orderBy,
    searchLimit,
    formView: formViewName,
    gridView: gridViewName,
  } = schema;
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
    useMemo(() => atom((get) => Boolean(get(valueAtom))), [valueAtom]),
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
    [icons],
  );

  const handleSelect = useAtomCallback(
    useCallback(
      async (get, set) => {
        showSelector({
          model,
          domain,
          orderBy,
          context: get(formAtom).record,
          limit: searchLimit,
          viewName: gridViewName,
          onSelect: (records) => {
            set(valueAtom, records[0], true);
          },
        });
      },
      [
        domain,
        formAtom,
        gridViewName,
        model,
        orderBy,
        searchLimit,
        showSelector,
        valueAtom,
      ],
    ),
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
          viewName: formViewName,
        });
      },
      [model, formViewName, showEditor, title, valueAtom],
    ),
  );

  const handleDelete = useAtomCallback(
    useCallback(
      (get, set) => {
        set(valueAtom, null, true);
      },
      [valueAtom],
    ),
  );

  const { itemsFamily, items, isCleanInitial, setInvalid } = useItemsFamily({
    widgetAtom,
    valueAtom,
    required,
    multiple: false,
    canShowNew: !readonly,
  });

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
    <FieldControl
      {...props}
      titleActions={titleActions}
      className={clsx({
        [styles.noErrors]: isCleanInitial,
      })}
    >
      {items.map((item) => (
        <RecordEditor
          key={item.id}
          schema={schema}
          editor={editor}
          fields={fields}
          formAtom={formAtom}
          widgetAtom={widgetAtom}
          valueAtom={itemsFamily(item)}
          model={model}
          readonly={readonly}
          setInvalid={setInvalid}
        />
      ))}
    </FieldControl>
  );
}

const IS_INITIAL = Symbol();

function useItemsFamily({
  widgetAtom,
  valueAtom,
  exclusive,
  required,
  multiple = true,
  canShowNew = true,
}: {
  widgetAtom: WidgetAtom;
  valueAtom: ValueAtom<DataRecord | DataRecord[]>;
  exclusive?: string;
  multiple?: boolean;
  required?: boolean;
  canShowNew?: boolean;
}) {
  const isInitial = useCallback(
    (item: DataRecord) => item && Reflect.get(item, IS_INITIAL),
    [],
  );

  const isClean = useCallback(
    (item: DataRecord) => item && Object.keys(item).length === 1,
    [],
  );

  const makeArray = useCallback((value: unknown): DataRecord[] => {
    if (Array.isArray(value)) return value;
    if (value) return [value];
    return [];
  }, []);

  const [errors, setErrors] = useState<Record<string, boolean>>({});
  const [initialInvalid, setInitialInvalid] = useState<boolean>(false);
  const [initialItem, setInitialItem] = useState<DataRecord>();

  const setInvalid = useSetAtom(setInvalidAtom);
  const handleInvalid = useCallback(
    (value: DataRecord, invalid: boolean) => {
      if (value && isInitial(value) && isClean(value)) {
        setInitialInvalid(invalid);
        return;
      }

      if (value) {
        const invalid_ =
          invalid || (value.id && value.id <= 0 && required && isClean(value));
        setInitialInvalid(false);
        setErrors((errors) => ({ ...errors, [value.id!]: invalid_ }));
      }
    },
    [isClean, isInitial, required],
  );

  const itemsAtom = useMemo(() => {
    return atom(
      (get) => {
        const value = get(valueAtom);
        const items = makeArray(value);
        if (items.length === 0 && canShowNew && initialItem) {
          return [initialItem];
        }
        return items;
      },
      (get, set, value: DataRecord[]) => {
        const items = makeArray(value);
        if (items.length === 1 && isInitial(items[0]) && isClean(items[0])) {
          return;
        }
        const next = multiple ? items : items[0] ?? null;
        set(valueAtom, next);
        setInitialItem(undefined);
      },
    );
  }, [
    canShowNew,
    isClean,
    isInitial,
    makeArray,
    multiple,
    initialItem,
    valueAtom,
  ]);

  const itemsFamily = useMemo(() => {
    return atomFamily(
      (record: DataRecord) =>
        atom(
          (get) => {
            const items = get(itemsAtom);
            return items.find((x: DataRecord) => x.id === record.id);
          },
          (get, set, value: DataRecord) => {
            if (isInitial(value) && isClean(value)) return;
            let items = get(itemsAtom);
            const found = items.find((x) => x.id === value.id);
            if (found) {
              items = items.map((x) => (x.id === value.id ? value : x));
            }
            if (exclusive && found && value[exclusive]) {
              items = items.map((item) => ({
                ...item,
                [exclusive]: item.id === value.id ? value[exclusive] : false,
              }));
            }
            const next = multiple ? items : items.slice(0, 1);
            set(itemsAtom, next);
          },
        ),
      (a, b) => a.id === b.id,
    );
  }, [itemsAtom, isInitial, isClean, exclusive, multiple]);

  const addItem = useAtomCallback(
    useCallback(
      (get, set, item?: DataRecord) => {
        const items = get(itemsAtom);
        const last = items.length ? items[items.length - 1] : null;
        if (last && isClean(last)) {
          return;
        }

        const record =
          item ?? (items.length === 0 && !required)
            ? { id: nextId(), [IS_INITIAL]: true }
            : { id: nextId() };

        if (!item && record[IS_INITIAL]) {
          itemsFamily(record);
          setInitialItem(record);
          return;
        }

        if (multiple) {
          itemsFamily(record);
          set(itemsAtom, [...items, record]);
        } else {
          items.forEach((item) => itemsFamily.remove(item));
          itemsFamily(record);
          set(itemsAtom, makeArray(record));
        }
      },
      [isClean, itemsAtom, itemsFamily, makeArray, multiple, required],
    ),
  );

  const syncItem = useAtomCallback(
    useCallback(
      (get, set, item: DataRecord) => {
        itemsFamily.remove(item);
        itemsFamily(item);
        const items = get(itemsAtom);
        const next = items.map((x) => (x.id === item.id ? item : x));
        set(itemsAtom, next);
      },
      [itemsAtom, itemsFamily],
    ),
  );

  const removeItem = useAtomCallback(
    useCallback(
      (get, set, record: DataRecord) => {
        itemsFamily.remove(record);
        const items = get(itemsAtom);
        const next = items.filter((x) => x.id !== record.id);
        if (next.length === 0 && canShowNew) {
          next.push({ id: nextId() });
          itemsFamily(next[0]);
        }
        set(itemsAtom, next);
      },
      [canShowNew, itemsAtom, itemsFamily],
    ),
  );

  const ensureNew = useAfterActions(
    useAtomCallback(
      useCallback(
        async (get) => {
          const items = get(itemsAtom);
          if (items.length === 0) {
            addItem();
          }
        },
        [addItem, itemsAtom],
      ),
    ),
  );

  const ensureValid = useAtomCallback(
    useCallback(
      (get) => {
        if (initialItem) return;
        const items = get(itemsAtom);
        const invalid = items.map((x) => errors[x.id!]).some((x) => x);
        setInvalid(widgetAtom, invalid || initialInvalid);
      },
      [errors, initialInvalid, initialItem, itemsAtom, setInvalid, widgetAtom],
    ),
  );

  const items = useAtomValue(itemsAtom);
  const isCleanInitial = !!initialItem && isClean(initialItem);

  useEffect(() => ensureValid(), [ensureValid]);
  useEffect(() => {
    if (items.length === 0 && canShowNew) ensureNew();
  }, [canShowNew, ensureNew, items.length]);

  return {
    itemsFamily,
    itemsAtom,
    items,
    isCleanInitial,
    addItem,
    syncItem,
    removeItem,
    setInvalid: handleInvalid,
  };
}

function CollectionEditor({ editor, fields, ...props }: FormEditorProps) {
  const { schema, formAtom, widgetAtom, valueAtom, readonly } = props;
  const model = schema.target!;

  const exclusive = useMemo(() => {
    const panel: Schema = editor.items?.[0] ?? {};
    const items = panel.items ?? [];
    return items.find((x) => x.exclusive)?.name;
  }, [editor]);

  const { hasButton } = usePermission(schema, widgetAtom);

  const canNew = !readonly && hasButton("new");
  const canShowNew = canNew && schema.editor.showOnNew !== false;

  const {
    itemsFamily,
    items,
    isCleanInitial,
    addItem,
    removeItem,
    setInvalid,
  } = useItemsFamily({
    widgetAtom,
    valueAtom,
    exclusive,
    canShowNew,
  });

  const handleAdd = useCallback(() => addItem(), [addItem]);
  const colSpan = Math.max(1, schema.editor.colSpan ?? 12);

  return (
    <FieldControl {...props}>
      <div
        data-editor-span={colSpan}
        className={clsx(styles.collection, {
          [styles.noErrors]: isCleanInitial,
        })}
      >
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
              remove={removeItem}
              readonly={readonly}
              setInvalid={setInvalid}
            />
          ))}
        </div>
        {canNew && (
          <div className={styles.actions}>
            <MaterialIcon icon="add" onClick={handleAdd} />
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
      [remove, valueAtom],
    ),
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
  },
);

const EMPTY_RECORD = Object.freeze({});

const findState = (
  schema: Schema,
  currentState: FormState,
  parentState: FormState,
) => {
  const name = schema.name!;
  const json = !!schema.json;
  const jsonFields = schema.jsonFields ?? {};
  const prefix = `${name}.`;

  const isJsonField = (key: string) => {
    return json && name === "attrs" && key in jsonFields;
  };

  const makeKey = (key: string) => {
    return key.startsWith(prefix) ? key.substring(prefix.length) : key;
  };

  const currentStates = currentState.statesByName;
  const parentStates = parentState.statesByName;

  const states = Object.entries(parentStates)
    .filter(([key]) => key.startsWith(prefix) || isJsonField(key))
    .reduce(
      (acc, [key, value]) => {
        return { ...acc, [makeKey(key)]: value };
      },
      {} as Record<string, WidgetState>,
    );

  const merged = Object.entries(states).reduce(
    (acc, [key, value]) => {
      const current = currentStates[key];
      const target: WidgetState = {
        ...current,
        ...value,
        attrs: {
          ...current?.attrs,
          ...value?.attrs,
        },
      };
      return { ...acc, [key]: target };
    },
    {} as Record<string, WidgetState>,
  );

  const result = {
    ...currentState,
    statesByName: {
      ...currentStates,
      ...merged,
    },
  };

  return result;
};

const RecordEditor = memo(function RecordEditor({
  model,
  editor,
  fields,
  formAtom: parent,
  widgetAtom,
  valueAtom,
  readonly,
  setInvalid,
  schema,
}: FormEditorProps & {
  model: string;
  setInvalid: (value: DataRecord, invalid: boolean) => void;
}) {
  const meta: ViewData<FormView> = useMemo(
    () => ({
      model,
      fields,
      view: editor,
    }),
    [editor, fields, model],
  );

  const editorFormAtom = useMemo(
    () =>
      createFormAtom({
        meta,
        record: EMPTY_RECORD,
        parent,
      }),
    [meta, parent],
  );

  const [loaded, setLoaded] = useState<DataRecord>({});
  const checkInvalidRef = useRef<() => void>();

  const editorAtom = useMemo(() => {
    return atom(
      (get) => {
        const value = get(valueAtom) || EMPTY_RECORD;
        const record = loaded.id && loaded.id === value.id ? loaded : value;

        const currentState = get(editorFormAtom);
        const parentState = get(parent);

        const state = findState(schema, currentState, parentState);
        const dirty = parentState.dirty;

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
          typeof update === "function" ? update(get(editorFormAtom)) : update;
        const { record } = state;

        set(editorFormAtom, state);
        set(valueAtom, isEqual(record, EMPTY_RECORD) ? null : record);

        // re-check validation
        checkInvalidRef.current?.();
      },
    );
  }, [editorFormAtom, loaded, parent, schema, valueAtom]);

  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, EMPTY_RECORD, parent, undefined, editorAtom);

  const ds = useMemo(() => new DataStore(model), [model]);
  const load = useAtomCallback(
    useCallback(
      async (get) => {
        const value = get(valueAtom);
        const id = value?.id ?? 0;
        if (id <= 0) return;
        const names = Object.keys(fields ?? {});
        const missing = names.some((x) => !Object.hasOwn(value, x));
        if (missing) {
          const rec = await ds.read(id, { fields: names });
          setLoaded(rec);
        }
      },
      [ds, fields, valueAtom],
    ),
  );

  useAsyncEffect(async () => load(), [load]);

  const mountRef = useRef<boolean>();

  useEffect(() => {
    mountRef.current = true;
    return () => {
      mountRef.current = false;
    };
  });

  const getErrors = useGetErrors();

  const invalidAtom = useMemo(
    () => selectAtom(editorAtom, (state) => getErrors(state) !== null),
    [editorAtom, getErrors],
  );
  const invalid = useAtomValue(invalidAtom);

  const checkInvalid = useAtomCallback(
    useCallback(
      (get) => {
        const value = get(valueAtom);
        setInvalid(value, invalid);
      },
      [invalid, setInvalid, valueAtom],
    ),
  );

  checkInvalidRef.current = checkInvalid;

  const idRef = useRef<number>();
  const id = useAtomValue(
    useMemo(() => selectAtom(valueAtom, (x) => x.id ?? 0), [valueAtom]),
  );

  const executeNew = useAfterActions(
    useCallback(async () => {
      const { onNew } = schema.editor;
      await actionExecutor.waitFor(100);
      if (mountRef.current && id <= 0 && onNew) {
        actionExecutor.execute(onNew);
      }
    }, [actionExecutor, id, schema.editor]),
  );

  useEffect(() => {
    checkInvalid();
  }, [checkInvalid]);

  useEffect(() => {
    if (id === idRef.current) return;
    idRef.current = id;
    executeNew();
  }, [executeNew, id, schema.name]);

  return (
    <ScopeProvider scope={MetaScope} value={meta}>
      <Form
        schema={editor}
        recordHandler={recordHandler}
        actionExecutor={actionExecutor}
        actionHandler={actionHandler}
        fields={fields}
        formAtom={formAtom}
        widgetAtom={widgetAtom}
        readonly={readonly}
      />
    </ScopeProvider>
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
    [formAtom],
  );
  const model = useAtomValue(modelAtom);
  const jsonModel = schema.jsonModel;

  const jsonAtom = useMemo(() => {
    return atom(
      (get) => {
        const value = get(valueAtom) || "{}";
        const json = JSON.parse(value);
        const $record = get(formAtom).record;
        return { ...json, $record };
      },
      (get, set, update: SetStateAction<DataRecord>) => {
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
      },
    );
  }, [formAtom, jsonModel, valueAtom]);

  const jsonEditor = useMemo(
    () => ({ ...processJsonView(editor), json: true }) as FormView,
    [editor],
  );

  const setInvalid = useSetAtom(setInvalidAtom);
  const handleInvalid = useCallback(
    (value: DataRecord, invalid: boolean) => {
      setInvalid(widgetAtom, invalid);
    },
    [setInvalid, widgetAtom],
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
      readonly={readonly || schema.readonly}
    />
  );
}

function processJsonView(schema: Schema) {
  const result = { ...schema, $json: true } as Schema;

  if (schema.serverType) {
    result.type = "field";
    result.widget = toKebabCase(schema.widget || schema.serverType);
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
