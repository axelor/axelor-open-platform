import { Box, clsx } from "@axelor/ui";
import { ScopeProvider } from "bunshi/react";
import { SetStateAction, atom, useAtom, useAtomValue, useSetAtom } from "jotai";
import { atomFamily, selectAtom, useAtomCallback } from "jotai/utils";
import filter from "lodash/filter";
import cloneDeep from "lodash/cloneDeep";
import getObjValue from "lodash/get";
import isEqual from "lodash/isEqual";
import isNumber from "lodash/isNumber";
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
  JsonField,
  Panel,
  Property,
  Schema,
} from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names.ts";
import { MetaScope, useViewTab } from "@/view-containers/views/scope";

import { Layout as FormViewLayout, useGetErrors } from "../form";
import { createFormAtom, formDirtyUpdater } from "./atoms";
import { Form, useFormHandlers, usePermission } from "./form";
import { FieldControl } from "./form-field";
import { GridLayout } from "./form-layouts";
import {
  useAfterActions,
  useCanDirty,
  useFormReady,
  useFormScope,
} from "./scope";
import {
  FieldProps,
  FormLayout,
  FormState,
  ValueAtom,
  WidgetAtom,
  WidgetErrors,
  WidgetState,
} from "./types";
import {
  SERVER_TYPES,
  getFieldServerType,
  getWidget,
  nextId,
  processView,
} from "./utils";

import styles from "./form-editors.module.scss";

export type FieldEditorProps = FieldProps<any>;
export type FormEditorProps = FieldEditorProps & {
  editor: FormView;
  fields: Record<string, Property>;
};

/**
 * For the given json field, check if the context field value match with the record data.
 *
 * @param field the json field
 * @param record the record to match against
 */
function isContextFieldMatch(field: JsonField, record: DataRecord): boolean {
  if (field.contextField) {
    return (
      String(field.contextFieldValue) === String(record[field.contextField]?.id)
    );
  }
  return true;
}

/**
 * Filter the given editor based on available editorFields. The editorFields are the
 * filtered contextual fields based on the record.
 *
 * @param editor the editor
 * @param editorFields the available editorFields
 */
function filterJsonEditorItems(
  editor: FormView,
  editorFields: Record<string, JsonField>,
) {
  const newEditor: FormView = cloneDeep(editor);

  function walkInItems(items: any) {
    const newItems: any[] = [];
    if (!items || items.length === 0) {
      return items;
    }
    items.forEach((item: any) => {
      if (item.name && !Object.keys(editorFields).includes(item.name)) {
        return;
      }
      if (item.items) {
        item.items = walkInItems(item.items);
      }
      newItems.push(item);
    });
    return newItems;
  }

  newEditor.items = walkInItems(newEditor.items);

  return newEditor;
}

function processEditor(schema: Schema) {
  const editor: Editor = schema.editor;
  const widgetAttrs = editor.widgetAttrs ?? {};
  const fields = editor.fields ?? schema.fields;

  const autoTitle = (item: Schema) => {
    if (item.showTitle && item.type !== "panel") {
      return item.autoTitle;
    }
  };

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
    result.title = item.title ?? field?.title ?? autoTitle(result) ?? "";

    if (!result.showTitle && !result.items && !result.inGridEditor) {
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
    if (item.type == "field" || item.type == "panel-related") {
      // Determine server type only for field. For json fields,
      // this is determined later (see in JsonEditor)
      result.serverType = getFieldServerType(item, field);
    }

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

  const hasColSpan = editor.layout !== "table";

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
    name,
    orderBy,
    searchLimit,
    formView: formViewName,
    gridView: gridViewName,
    perms,
  } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title, domain, required, canRemove: _canRemove = true } = attrs;

  const model = schema.target!;

  const showEditor = useEditor();
  const showSelector = useSelector();
  const { hasButton } = usePermission(schema, widgetAtom, perms);

  const hasValue = useAtomValue(
    useMemo(() => atom((get) => Boolean(get(valueAtom))), [valueAtom]),
  );

  const canNew = hasButton("new") && attrs.canNew;
  const canRemove = hasValue && _canRemove;
  const canEdit = hasValue && hasButton("edit") && attrs.canEdit;
  const canSelect = hasButton("select");
  const canView = hasValue && hasButton("view");

  const [shouldSyncVersion, syncVersion] = useAtom(
    useMemo(
      () =>
        atom(
          (get) => {
            const value = get(valueAtom);
            const { $version, version } = value || {};
            return isNumber($version) && version === undefined;
          },
          (get, set) => {
            const value = get(valueAtom);
            value &&
              set(valueAtom, {
                ...value,
                version: value.version ?? value.$version,
              });
          },
        ),
      [valueAtom],
    ),
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

  const handleEdit = useAtomCallback(
    useCallback(
      (get, set, readonly: boolean = false, record?: DataRecord) => {
        showEditor({
          model,
          title: title ?? "",
          onSelect: (record) => {
            set(valueAtom, record, true);
          },
          record: record ?? get(valueAtom),
          readonly,
          viewName: formViewName,
        });
      },
      [model, formViewName, showEditor, title, valueAtom],
    ),
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
          multiple: false,
          ...(canNew && {
            onCreate: () => handleEdit(false, { id: null }),
          }),
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
        canNew,
        showSelector,
        handleEdit,
        valueAtom,
      ],
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
    name,
    widgetAtom,
    valueAtom,
    required,
    multiple: false,
    canShowNew: !readonly,
  });

  const titleActions = !readonly && (
    <div className={styles.actions}>
      {canEdit && canShowIcon("edit") && (
        <Box d="flex" alignItems="center" title={i18n.get("Edit")}>
          <MaterialIcon icon="edit" onClick={() => handleEdit(false)} />
        </Box>
      )}
      {canView && !canEdit && canShowIcon("view") && (
        <Box d="flex" alignItems="center" title={i18n.get("View")}>
          <MaterialIcon icon="description" onClick={() => handleEdit(true)} />
        </Box>
      )}
      {canSelect && canShowIcon("select") && (
        <Box d="flex" alignItems="center" title={i18n.get("Select")}>
          <MaterialIcon icon="search" onClick={handleSelect} />
        </Box>
      )}
      {canRemove && canShowIcon("clear") && (
        <Box d="flex" alignItems="center" title={i18n.get("Clear")}>
          <MaterialIcon icon="cancel" onClick={handleDelete} />
        </Box>
      )}
    </div>
  );

  useEffect(() => {
    if (shouldSyncVersion) {
      syncVersion();
    }
  }, [shouldSyncVersion, syncVersion]);

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
          readonly={readonly || !hasButton("edit")}
          setInvalid={setInvalid}
        />
      ))}
    </FieldControl>
  );
}

const IS_INITIAL = Symbol();

function useItemsFamily({
  name: fieldName,
  widgetAtom,
  valueAtom,
  exclusive,
  required,
  multiple = true,
  canShowNew = true,
}: {
  name: string;
  widgetAtom: WidgetAtom;
  valueAtom: ValueAtom<DataRecord | DataRecord[]>;
  exclusive?: string;
  multiple?: boolean;
  required?: boolean;
  canShowNew?: boolean;
}) {
  const isInitial = useCallback(
    (item: DataRecord | null) => item && Reflect.get(item, IS_INITIAL),
    [],
  );

  const canDirty = useCanDirty();

  const isClean = useCallback(
    (item: DataRecord | null) => {
      if (!item) return true;
      const values = filter(item, function (value, name) {
        return (
          (name !== "id" || value > 0) &&
          canDirty(`${fieldName}.${name}`) &&
          value != null
        );
      });
      return values.length === 0;
    },
    [canDirty, fieldName],
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
        const items = makeArray(value).map((item) => {
          if (item.id) {
            return item;
          }
          return { ...item, id: nextId() };
        });
        if (items.length === 0 && canShowNew && initialItem) {
          return [initialItem];
        }
        return items;
      },
      (
        get,
        set,
        value: DataRecord[],
        fireOnChange?: boolean,
        markDirty?: boolean,
      ) => {
        const items = makeArray(value);
        if (items.length === 1 && isInitial(items[0]) && isClean(items[0])) {
          return;
        }
        const cleanInitial = (val: DataRecord | null) =>
          isInitial(val) ? { ...val, [IS_INITIAL]: false } : val;

        // once updated, it should clean up IS_INITIAL
        const next = multiple
          ? items?.map(cleanInitial)
          : (cleanInitial(items[0]) ?? null);

        set(valueAtom, next, fireOnChange, markDirty);
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
          (
            get,
            set,
            value: DataRecord | null,
            fireOnChange?: boolean,
            markDirty?: boolean,
          ) => {
            if (isInitial(value) && isClean(value)) return;
            let items = get(itemsAtom);
            const found = items.find((x) => x.id === value?.id);
            if (found) {
              items = items.map((x) =>
                x.id === value?.id ? value : x,
              ) as DataRecord[];
            }
            if (exclusive && found && value?.[exclusive]) {
              items = items.map((item) => ({
                ...item,
                [exclusive]: item.id === value?.id ? value[exclusive] : false,
              }));
            }
            const next = multiple ? items : items.slice(0, 1);
            set(itemsAtom, next, fireOnChange, markDirty);
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
          item ??
          (items.length === 0 && !required
            ? { id: nextId(), [IS_INITIAL]: true }
            : { id: nextId() });

        if (isInitial(record)) {
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
      [
        isClean,
        isInitial,
        itemsAtom,
        itemsFamily,
        makeArray,
        multiple,
        required,
      ],
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
        set(itemsAtom, next);

        if (next.length === 0 && canShowNew) {
          addItem();
        }
      },
      [itemsAtom, itemsFamily, canShowNew, addItem],
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

  const items = useAtomValue(itemsAtom);
  const isCleanInitial = !!initialItem && isClean(initialItem);

  const ensureValid = useAtomCallback(
    useCallback(
      (get) => {
        if (initialItem || items?.length === 0) {
          return setInvalid(widgetAtom, false);
        }
        const currItems = get(itemsAtom);
        const invalid = currItems.map((x) => errors[x.id!]).some((x) => x);
        setInvalid(widgetAtom, invalid || initialInvalid);
      },
      [
        errors,
        initialInvalid,
        initialItem,
        itemsAtom,
        setInvalid,
        widgetAtom,
        items?.length,
      ],
    ),
  );

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
  const { name, perms } = schema;
  const model = schema.target!;

  const exclusive = useMemo(() => {
    const panel: Schema = editor.items?.[0] ?? {};
    const items = panel.items ?? [];
    return items.find((x) => x.exclusive)?.name;
  }, [editor]);

  const { hasButton } = usePermission(schema, widgetAtom, perms);

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
    name,
    widgetAtom,
    valueAtom,
    exclusive,
    canShowNew,
  });

  const handleAdd = useCallback(() => addItem({ id: nextId() }), [addItem]);
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
  setInvalid,
  ...props
}: FormEditorProps & {
  model: string;
  remove: (record: DataRecord) => void;
  setInvalid: (value: DataRecord, invalid: boolean) => void;
}) {
  const { schema, widgetAtom, valueAtom, readonly } = props;
  const { perms } = schema;

  const { hasButton } = usePermission(schema, widgetAtom, perms);
  const canRemove = !readonly && hasButton("remove");

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
      {canRemove && (
        <div className={styles.actions}>
          <MaterialIcon icon="close" onClick={handleRemove} />
        </div>
      )}
    </div>
  );
});

const setInvalidAtom = atom(
  null,
  (
    get,
    set,
    widgetAtom: WidgetAtom,
    invalid: boolean,
    widgetErrors?: WidgetErrors | null,
  ) => {
    const state = get(widgetAtom);
    const errors = invalid
      ? widgetErrors || {
          invalid: i18n.get("{0} is invalid", state.attrs.title),
        }
      : {};
    if (isEqual(errors, state.errors ?? {})) return;
    set(widgetAtom, (prev) => ({ ...prev, errors }));
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

  const isJsonField = (key: string) => json && key in jsonFields;

  const makeKey = (key: string) => {
    return key.startsWith(prefix) ? key.substring(prefix.length) : key;
  };

  const currentStates = currentState.statesByName;
  const parentStates = parentState.statesByName;

  const states = Object.entries(parentStates)
    .filter(
      ([key]) =>
        key.startsWith(prefix) &&
        (!json || isJsonField(key.slice(prefix.length))),
    )
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
  layout,
}: FormEditorProps & {
  model: string;
  layout?: FormLayout;
  setInvalid: (value: DataRecord, invalid: boolean, errors?: string[]) => void;
}) {
  const meta: ViewData<FormView> = useMemo(
    () => ({
      model,
      fields,
      view: editor,
      schema,
    }),
    [schema, editor, fields, model],
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
    const getRecord = (_value: DataRecord) => {
      const value = _value || EMPTY_RECORD;
      const loadedRec = loaded.id && loaded.id === value.id ? loaded : value;
      return { ...loadedRec, ...value };
    };
    return atom(
      (get) => {
        const record = getRecord(get(valueAtom));

        const currentState = get(editorFormAtom);
        const parentState = get(parent);

        const state = findState(schema, currentState, parentState);
        const parentOriginal = getObjValue(parentState.original, schema.name);
        const original = {
          ...state.original,
          ...(Array.isArray(parentOriginal)
            ? parentOriginal.find((x) => x.id === record.id)
            : typeof parentOriginal === "string" && currentState.meta.view?.json
              ? JSON.parse(parentOriginal ?? "{}")
              : parentOriginal),
        };
        const dirty = parentState.dirty;

        return {
          ...state,
          original,
          dirty,
          record,
        };
      },
      (get, set, update: SetStateAction<FormState>) => {
        const state =
          typeof update === "function" ? update(get(editorFormAtom)) : update;
        const value = getRecord(get(valueAtom));

        let { record } = state;

        if (value?.id === record?.id) {
          const version = value?.version ?? value?.$version;
          if (
            (version || version === 0) &&
            version > (record.version ?? record.$version ?? 0)
          ) {
            record = { ...record, version, $version: version };
          }
        }

        set(editorFormAtom, state);

        // the update is intended for dirty state changed
        // no value changes occurs through this update
        // also need to sync dirty with parent form dirty state
        if (update === formDirtyUpdater) {
          return (
            state.dirty && set(parent, (state) => ({ ...state, dirty: true }))
          );
        }

        set(
          valueAtom,
          isEqual(record, EMPTY_RECORD) ? null : record,
          schema.json ? false : undefined,
          state.dirty,
        );
        // re-check validation
        checkInvalidRef.current?.();
      },
    );
  }, [editorFormAtom, loaded, parent, schema, valueAtom]);

  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, EMPTY_RECORD, {
      parent,
      formAtom: editorAtom,
    });

  const { actionHandler: parentHandler } = useFormScope();
  actionHandler.setSaveHandler(
    useCallback(
      async (record?: DataRecord) => parentHandler.save(record),
      [parentHandler],
    ),
  );
  actionHandler.setRefreshHandler(
    useCallback(
      async (target?: string) => parentHandler.refresh(target),
      [parentHandler],
    ),
  );
  actionHandler.setValidateHandler(
    useCallback(async () => parentHandler.validate(), [parentHandler]),
  );

  const ds = useMemo(() => new DataStore(model), [model]);
  const load = useAtomCallback(
    useCallback(
      async (get) => {
        const value = get(valueAtom);
        const id = value?.id ?? 0;
        if (id <= 0) return;
        const names = Object.keys(fields ?? {});
        const missing = names.some((x) => getObjValue(value, x) === undefined);
        if (missing) {
          const rec = await ds.read(id, { fields: names });
          const result = names
            .map((x) => x.split(".")[0])
            .reduce(
              (acc, x) => ({
                ...acc,
                [x]: rec[x],
              }),
              value,
            );
          setLoaded(result);
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
        let errors;

        if (invalid && editor.json) {
          const state = get(editorAtom);
          errors = getErrors(state)?.reduce(
            (errorList, error) =>
              errorList.concat(Object.values(error) as string[]),
            [] as string[],
          );
        }

        setInvalid(value, invalid, errors);
      },
      [invalid, setInvalid, editorAtom, editor.json, getErrors, valueAtom],
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

  const tab = useViewTab();
  const resetStates = useAtomCallback(
    useCallback(
      (get, set, event: Event) => {
        if (event instanceof CustomEvent && event.detail === tab.id) {
          set(editorFormAtom, (prev) => {
            return {
              ...prev,
              states: {},
              statesByName: {},
            };
          });
        }
      },
      [editorFormAtom, tab.id],
    ),
  );

  useEffect(() => {
    document.addEventListener("form:reset-states", resetStates);
    return () => {
      document.removeEventListener("form:reset-states", resetStates);
    };
  }, [resetStates]);

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
        layout={layout}
      />
    </ScopeProvider>
  );
});

function JsonEditor(props: FormEditorProps) {
  const { schema, formAtom, editor } = props;

  const fieldsAtom = useMemo(
    () => selectAtom(formAtom, (o) => o.fields),
    [formAtom],
  );

  const modelFields = useAtomValue(fieldsAtom);
  const jsonFields: Record<string, JsonField> = useMemo(
    () => schema.jsonFields ?? {},
    [schema.jsonFields],
  );

  const editorFieldsRef = useRef<Record<string, JsonField>>({});
  const editorFieldsAtom = useMemo(() => {
    return atom((get) => {
      if (
        Object.values(jsonFields)
          .filter((x) => x.contextField)
          .map((x) => x.contextField!)
          .filter((x) => modelFields[x])
          .filter((x, i, a) => a.indexOf(x) === i).length === 0
      ) {
        return jsonFields;
      }
      const record = get(formAtom).record;
      let lastPanelExcluded = false;
      const nextFields = Object.entries(jsonFields)
        .filter(([_, field]) => {
          if (field.type === "panel") {
            const res = isContextFieldMatch(field, record);
            lastPanelExcluded = !res;
            return res;
          } else if (lastPanelExcluded) {
            return false;
          }

          return isContextFieldMatch(field, record);
        })
        .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});

      // if fields changed, update the ref
      const currentValues = Object.values(editorFieldsRef.current);
      const nextValues = Object.values(nextFields);
      if (
        currentValues.length !== nextValues.length ||
        currentValues.some((x, i) => x !== nextValues[i])
      ) {
        editorFieldsRef.current = nextFields;
      }

      return editorFieldsRef.current;
    });
  }, [formAtom, jsonFields, modelFields]);

  const editorFields = useAtomValue(editorFieldsAtom);
  const jsonEditor = useMemo(
    () => filterJsonEditorItems(editor, editorFields),
    [editor, editorFields],
  );

  return <JsonEditorInner {...props} editor={jsonEditor} />;
}

function JsonEditorInner({
  schema,
  editor,
  fields,
  formAtom,
  widgetAtom,
  valueAtom,
  readonly,
}: FormEditorProps) {
  const formReady = useFormReady();

  const modelAtom = useMemo(
    () => selectAtom(formAtom, (x) => x.model),
    [formAtom],
  );
  const model = useAtomValue(modelAtom);
  const jsonModel = schema.jsonModel;
  const jsonFields: Record<string, JsonField> = schema.jsonFields ?? {};
  const jsonNameField = Object.values(jsonFields).find((x) => x.nameColumn);
  const jsonValueRef = useRef<DataRecord>();

  const jsonAtom = useMemo(() => {
    return atom(
      (get) => {
        const value = get(valueAtom) || "{}";
        const json = JSON.parse(value);
        const $record = get(formAtom).record;
        return {
          ...(isEqual(jsonValueRef.current, json)
            ? jsonValueRef.current
            : (jsonValueRef.current = json)),
          $record,
        };
      },
      (
        get,
        set,
        update: SetStateAction<DataRecord>,
        fireOnChange?: boolean,
        markDirty?: boolean,
      ) => {
        const state =
          typeof update === "function" ? update(get(valueAtom)) : update;
        const { $record: _record, ...value } = state ?? {};
        const jsonName = jsonNameField ? value?.[jsonNameField.name] : null;
        const prevJsonValue = get(valueAtom);
        const jsonValue = Object.keys(value || {}).length
          ? JSON.stringify(value)
          : ["{}", undefined].includes(prevJsonValue)
            ? prevJsonValue
            : null;

        set(valueAtom, jsonValue, fireOnChange, markDirty);

        if (jsonModel) {
          const formState = get(formAtom);
          if (
            formState.record.jsonModel !== jsonModel ||
            formState.record.name !== jsonName
          ) {
            set(formAtom, {
              ...formState,
              record: { ...formState.record, jsonModel, name: jsonName },
            });
          }
        }
      },
    );
  }, [formAtom, jsonModel, jsonNameField, valueAtom]);

  const jsonLayout = schema.jsonModel ? FormViewLayout : undefined;
  const jsonEditor = useMemo(() => {
    const view = { ...processJsonView(editor, schema.jsonFields), json: true };
    const first = editor.items?.[0] as Schema;
    // for custom model view, if first item is panel, consider
    // it as root schema to handle sidebar and tabs property.
    if (first?.type === "panel" && schema.jsonModel) {
      const { items = [] } = first;
      return { ...view, items } as FormView;
    }
    return view as FormView;
  }, [editor, schema.jsonFields, schema.jsonModel]);

  const setInvalid = useSetAtom(setInvalidAtom);
  const handleInvalid = useCallback(
    (value: DataRecord, invalid: boolean, errors?: string[]) => {
      if (formReady) {
        setInvalid(widgetAtom, invalid, errors?.length ? { errors } : null);
      }
    },
    [setInvalid, widgetAtom, formReady],
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
      layout={jsonLayout}
    />
  );
}

function processJsonView(schema: Schema, jsonFields: any) {
  const result = { ...schema, $json: true } as Schema;

  // json field are sent with schema.type mixing server type and type (field, panel, ...)
  const isField = SERVER_TYPES.includes(
    toKebabCase(schema.type!).toLowerCase(),
  );

  // Determine the server type (if this is a field) and the widget in this order.
  if (isField) {
    result.serverType = getFieldServerType(
      result,
      jsonFields[schema.name!] as Property,
    );
  }
  result.widget = getWidget(result, null);

  // reset type only for fields at the end
  if (isField) {
    result.type = "field";
  }

  if (Array.isArray(result.items)) {
    result.items = result.items.map((i) => processJsonView(i, jsonFields));
  }

  return result;
}
