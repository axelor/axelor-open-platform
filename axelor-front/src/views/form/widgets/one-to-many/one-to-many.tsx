import { ScopeProvider } from "bunshi/react";
import { produce } from "immer";
import { atom, useAtom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import getNested from "lodash/get";
import isEqual from "lodash/isEqual";
import pick from "lodash/pick";
import uniq from "lodash/uniq";
import uniqueId from "lodash/uniqueId";
import {
  ForwardedRef,
  HTMLAttributes,
  SetStateAction,
  SyntheticEvent,
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useReducer,
  useRef,
  useState,
} from "react";

import { Box, Panel, clsx } from "@axelor/ui";
import { GridColumnProps, GridRow } from "@axelor/ui/grid";

import { dialogs } from "@/components/dialogs";
import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { usePermitted } from "@/hooks/use-permitted";
import {
  EditorOptions,
  useBeforeSelect,
  useEditor,
  useEditorInTab,
  useSelector,
} from "@/hooks/use-relation";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { equals } from "@/services/client/data-utils";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { ActionResult, ViewData } from "@/services/client/meta";
import { findView } from "@/services/client/meta-cache";
import {
  Field,
  FormView,
  GridView,
  Property,
  Schema,
  View,
} from "@/services/client/meta.types";
import { focusAtom } from "@/utils/atoms";
import { download } from "@/utils/download";
import { toKebabCase } from "@/utils/names";
import { getExportFieldNames } from "@/view-containers/advance-search/utils";
import { ToolbarActions } from "@/view-containers/view-toolbar";
import { MetaScope, useUpdateViewDirty } from "@/view-containers/views/scope";
import { Grid as GridComponent, GridHandler } from "@/views/grid/builder";
import { useCustomizePopup } from "@/views/grid/builder/customize";
import { ExpandIcon } from "@/views/grid/builder/expandable";
import {
  CollectionTree,
  GridExpandableContext,
  useCollectionTree,
  useGridColumnNames,
  useGridExpandableContext,
  useIsRootCollectionTree,
} from "@/views/grid/builder/scope";
import { isValidSequence, useGridState } from "@/views/grid/builder/utils";

import {
  FieldError,
  FieldLabel,
  FieldProps,
  usePermission,
  usePrepareWidgetContext,
} from "../../builder";
import {
  useActionExecutor,
  useAfterActions,
  useFormActiveHandler,
  useFormRefresh,
  useFormScope,
} from "../../builder/scope";
import { getDefaultValues, nextId } from "../../builder/utils";
import { fetchRecord } from "../../form";
import { DetailsForm } from "./one-to-many.details";

import styles from "./one-to-many.module.scss";

const noop = () => {};

/**
 * Update dotted values from nested values.
 * If previous record is specified, delete dotted values that do no match.
 *
 * @param {DataRecord} record - The input data record
 * @param {DataRecord} [previousRecord] - The previous data record
 * @returns {DataRecord} - The resulting data record with nested fields converted to dotted fields
 */
function nestedToDotted(record: DataRecord, previousRecord?: DataRecord) {
  const result: DataRecord = { ...record };

  Object.keys(record).forEach((key) => {
    const path = key.split(".");
    if (path.length > 1) {
      const value = getNested(record, path);
      if (value !== undefined) {
        result[key] = value;
      } else if (previousRecord) {
        const parentPath = findNearestParentPath(record, path);
        if (parentPath.length) {
          const current = getNested(record, parentPath);
          const prev = getNested(previousRecord, parentPath);
          if (!current || current.id !== prev?.id) {
            result[key] = undefined;
          }
        }
      }
    }
  });

  return result;
}

function findNearestParentPath(record: DataRecord, path: string[] | string) {
  const keys = (Array.isArray(path) ? path : path.split(".")).slice(0, -1);
  let current = record;

  for (let i = 0; i < keys.length; ++i) {
    const key = keys[i];
    current = current[key];

    if (current == null || typeof current !== "object") {
      const offset = current === null ? 1 : 0;
      return keys.slice(0, i + offset);
    }
  }

  return keys;
}

function flattenItems(record: DataRecord, name: string) {
  return (record?.[name] || []).reduce(
    (arr: DataRecord[], item: DataRecord) => [
      ...arr,
      item,
      ...flattenItems(item, name),
    ],
    [],
  );
}

function isExpandableWidget(schema: Schema) {
  return ["tree-grid", "expandable"].includes(schema.widget ?? "");
}

export function OneToMany(props: FieldProps<DataRecord[]>) {
  const { schema } = props;
  const { target: model, gridView } = schema;
  const { actionExecutor } = useFormScope();
  const isRootCollection = useIsRootCollectionTree();

  const waitForActions = useCallback(async () => {
    await actionExecutor.waitFor();
    await actionExecutor.wait();
  }, [actionExecutor]);

  const { state, data } = useAsync(async () => {
    const { items, serverType } = schema;
    if ((items || []).length > 0) return;
    const { view, ...res } = await findView<GridView>({
      type: "grid",
      name: gridView,
      model,
    });
    return {
      ...res,
      view: view && {
        serverType,
        ...view,
        ...[
          "canMove",
          "editable",
          "selector",
          "rowHeight",
          "onNew",
          "orderBy",
          "groupBy",
        ].reduce(
          (obj, key) => ({
            ...obj,
            [key]: schema[key] ?? view[key as keyof GridView],
          }),
          {},
        ),
      },
    };
  }, [schema, model]);

  if (state !== "hasData") return null;

  const hasExpandable =
    isExpandableWidget(schema) ||
    (data?.view && isExpandableWidget(data?.view as Schema));
  const isRootTreeGrid = hasExpandable && isRootCollection;

  function render() {
    return (
      <OneToManyInner
        {...props}
        schema={schema}
        viewData={data}
        isRootTreeGrid={isRootTreeGrid}
      />
    );
  }

  if (hasExpandable) {
    return (
      <CollectionTree
        enabled={hasExpandable}
        {...(isRootTreeGrid && {
          waitForActions,
        })}
      >
        {render()}
      </CollectionTree>
    );
  }

  return render();
}

function OneToManyInner({
  schema,
  valueAtom: _valueAtom,
  widgetAtom,
  formAtom,
  viewData,
  isRootTreeGrid,
  ...props
}: FieldProps<DataRecord[]> & {
  viewData?: ViewData<GridView>;
  isRootTreeGrid?: boolean;
}) {
  const {
    widget,
    widgetAttrs,
    name,
    showBars = widgetAttrs?.showBars,
    groupBy = viewData?.view?.groupBy,
    toolbar = viewData?.view?.toolbar,
    menubar = viewData?.view?.menubar,
    target: model,
    fields,
    formView,
    summaryView = viewData?.view?.summaryView,
    gridView,
    searchLimit,
    canExport = widgetAttrs?.canExport,
    canCopy = widgetAttrs?.canCopy,
    height,
    perms,
  } = schema;

  // use ref to avoid onSearch call
  const shouldSearch = useRef(true);
  const selectedIdsRef = useRef<number[]>([]);
  const serverSelectedIdsRef = useRef<number[]>([]);
  const reorderRef = useRef(false);
  const recordsSyncRef = useRef(false);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const gridRef = useRef<GridHandler>(null);
  const saveIdRef = useRef<number | null>();

  const [records, setRecords] = useState<DataRecord[]>([]);
  const [detailRecord, setDetailRecord] = useState<DataRecord | null>(null);
  const [, forceUpdate] = useReducer(() => ({}), {});

  const { level: expandLevel = 0 } = useGridExpandableContext();
  const {
    items: itemsAtom,
    expand: expandAtom,
    getItem,
    enabled: isCollectionTree,
    waitForActions: waitForCollectionActions,
  } = useCollectionTree();

  const isExpandable = widget === "expandable";
  const isTreeGrid = widget === "tree-grid";
  const isSubTreeGrid = isTreeGrid && !isRootTreeGrid;
  const expandable = isExpandable || isTreeGrid;
  const isTreeLimitExceed = useMemo(() => {
    const treeLimit = isNaN(widgetAttrs?.treeLimit)
      ? null
      : +widgetAttrs.treeLimit;
    return isTreeGrid && (treeLimit ?? 0) > 0 && expandLevel >= treeLimit!;
  }, [isTreeGrid, widgetAttrs?.treeLimit, expandLevel]);

  const expandAll = useAtomValue(expandAtom);
  const expandFieldList = useMemo<string[]>(() => {
    const expandAll =
      (isTreeGrid && widgetAttrs?.treeField) || widgetAttrs?.expandAll;
    if (isCollectionTree && expandAll) {
      const value = expandAll;
      if (!["true", "false"].includes(value?.toLowerCase())) {
        return uniq(value.split(","));
      }
    }
    return [];
  }, [
    isCollectionTree,
    isTreeGrid,
    widgetAttrs?.treeField,
    widgetAttrs?.expandAll,
  ]);

  const widgetState = useMemo(
    () =>
      focusAtom(
        formAtom,
        ({ statesByName = {} }) => statesByName[name],
        ({ statesByName = {}, ...rest }, value) => ({
          ...rest,
          statesByName: { ...statesByName, [name]: value },
        }),
      ),
    [formAtom, name],
  );

  const setSelection = useSetAtom(
    useMemo(
      () =>
        atom(null, (get, set, selectedIds: number[]) => {
          const state = get(widgetState);
          set(widgetState, { ...state, selected: selectedIds });
        }),
      [widgetState],
    ),
  );

  const lastUpdatedValueRef = useRef<DataRecord[] | null>();
  const lastValueRef = useRef<DataRecord[] | null>();
  const lastItemsRef = useRef<DataRecord[]>([]);
  const getItems = useCallback((value: DataRecord[] | null | undefined) => {
    if (lastValueRef.current === value) return lastItemsRef.current;
    const items = value ?? [];
    const isNum = (x: unknown) => typeof x === "number";
    lastValueRef.current = value;
    lastItemsRef.current = items.map((x) =>
      isNum(x)
        ? ({ id: x } as unknown as DataRecord)
        : x.id === undefined || x.id === null
          ? {
              ...x,
              _dirty: true,
              ...(() => {
                const id = x.cid ?? nextId();
                return { id, cid: id };
              })(),
            }
          : x,
    );
    return lastItemsRef.current;
  }, []);

  const valueAtom = useMemo(
    () =>
      atom(
        (get) => get(_valueAtom),
        (
          get,
          set,
          setter: SetStateAction<DataRecord[]>,
          callOnChange: boolean = true,
          markDirty: boolean = true,
        ) => {
          const values =
            typeof setter === "function" ? setter(get(valueAtom)!) : setter;
          lastUpdatedValueRef.current = values;
          set(_valueAtom, values, callOnChange, markDirty);
        },
      ),
    [_valueAtom],
  );

  const [value, setValue] = useAtom(
    useMemo(
      () =>
        atom(
          (get) => getItems(get(valueAtom)),
          async (
            get,
            set,
            setter: SetStateAction<DataRecord[]>,
            callOnChange: boolean = true,
            markDirty: boolean = true,
            resetRecords: boolean = false,
          ) => {
            shouldSearch.current = false;
            const values = (
              typeof setter === "function"
                ? setter(getItems(get(valueAtom)!))
                : setter
            )?.map((record) => nestedToDotted(record));
            const valIds = (values || []).map((v) => v.id);

            setRecords((records) => {
              if (resetRecords) {
                return values
                  .map((v) => ({ ...records.find((r) => r.id === v.id), ...v }))
                  .filter((v) => v) as DataRecord[];
              }
              const recIds = records.map((r) => r.id);
              const deleteIds = recIds.filter((id) => !valIds.includes(id));
              const newRecords = (values || []).filter(
                (v) => !recIds.includes(v.id),
              );

              return records
                .filter((rec) => !deleteIds.includes(rec.id))
                .map((rec) => {
                  const val = rec.id
                    ? values.find((v) => v.id === rec.id)
                    : null;
                  return val ? nestedToDotted({ ...rec, ...val }, rec) : rec;
                })
                .concat(newRecords);
            });

            const result = await set(
              valueAtom,
              values,
              callOnChange,
              markDirty,
            );

            const hasValueChanged =
              (result as unknown as ActionResult[])?.filter?.(
                ({ values, attrs }) =>
                  values?.[schema.name] !== undefined ||
                  attrs?.[schema.name]?.value !== undefined,
              ).length > 0;

            /**
             * if same o2m field values is changed on onChange event of itself
             * then have to wait for values to get updated in state
             */
            if (hasValueChanged) {
              await new Promise((resolve) => {
                setTimeout(() => resolve(true), 500);
              });
            } else if (isRootTreeGrid && values) {
              // reset _changed and _original
              const updatedValues = produce(values, (draft) => {
                function process(obj: any) {
                  if (obj && Array.isArray(obj)) {
                    obj.map(process);
                    return;
                  }
                  if (obj && typeof obj === "object") {
                    if (obj._changed) {
                      delete obj._changed;
                      delete obj._original;
                    }
                    Object.entries(obj).forEach(([, v]) => process(v));
                  }
                }
                draft.map(process);
              });
              set(valueAtom, updatedValues, false, false);
            }
          },
        ),
      [getItems, valueAtom, schema.name, isRootTreeGrid],
    ),
  );

  const { hasButton } = usePermission(schema, widgetAtom, perms);

  const parentId = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record.id), [formAtom]),
  );
  const parentModel = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.model), [formAtom]),
  );

  const { attrs, columns: columnAttrs } = useAtomValue(widgetAtom);
  const { title, domain } = attrs;
  const readonly = props.readonly || attrs.readonly;

  const isManyToMany =
    toKebabCase(schema.serverType || widget) === "many-to-many";

  const gridViewData = useMemo(
    () => ({ ...(viewData?.view || schema), widget, widgetAttrs }) as GridView,
    [schema, viewData?.view, widget, widgetAttrs],
  );
  const gridViewFields = viewData?.fields || fields;

  const viewMeta = useMemo(() => {
    return {
      ...(viewData ?? {
        view: { ...schema, type: "grid" } as GridView,
        fields,
        model,
      }),
      widgetSchema: schema,
    };
  }, [fields, model, schema, viewData]);

  const getContext = usePrepareWidgetContext(schema, formAtom, widgetAtom);

  const showEditor = useEditor();
  const showEditorInTab = useEditorInTab(schema);
  const showSelector = useSelector();
  const [state, setState, gridAtom] = useGridState({
    params: { groupBy },
  });
  const onColumnCustomize = useCustomizePopup({
    view: viewData?.view,
    stateAtom: gridAtom,
  });
  const dataStore = useMemo(() => new DataStore(model), [model]);

  const { editRow, selectedRows, rows } = state;

  const canNew = !readonly && hasButton("new");
  const canEdit = !readonly && hasButton("edit");
  const canView = hasButton("view");
  const canDelete =
    !readonly &&
    (isManyToMany ? attrs.canRemove !== false : hasButton("remove"));
  const canSelect =
    !readonly && hasButton("select") && (isManyToMany || attrs.canSelect);
  const canDuplicate = canNew && canCopy && selectedRows?.length === 1;
  const _canMove = Boolean(schema.canMove ?? viewData?.view?.canMove);

  const orderBy = schema.orderBy ?? viewData?.view?.orderBy;
  const editable =
    !readonly &&
    (schema.editable ?? widgetAttrs?.editable ?? viewData?.view?.editable);
  const editableAndCanEdit = editable && canEdit;

  const formFields = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.fields), [formAtom]),
  );

  const [canMove, orderField] = useMemo(() => {
    if (_canMove !== true) return [false, null];

    // With dummy fields, it is allowed to have no orderBy.
    const orderField = orderBy?.split(/\s*,\s*/)?.[0] as string | undefined;
    if (!orderField) {
      return [!formFields[name], null];
    }

    const schemaFields = Array.isArray(fields)
      ? fields.reduce(
          (acc: Record<string, Property>, item: Property) => ({
            ...acc,
            [item.name]: item,
          }),
          {} as Record<string, Property>,
        )
      : fields;
    const allFields = { ...viewMeta.fields, ...schemaFields };
    const field = allFields[orderField];

    return [Boolean(field) && isValidSequence(field), orderField];
  }, [_canMove, orderBy, viewMeta.fields, fields, formFields, name]);

  const reorderItems = useCallback(
    (items: DataRecord[]) => {
      let result = [...items];

      result.sort((a, b) => {
        // Apply records order
        const indexA = records.findIndex((x) => x.id === a.id);
        const indexB = records.findIndex((x) => x.id === b.id);

        if (indexA >= 0 && indexB >= 0) {
          return indexA - indexB;
        } else if (indexA >= 0) {
          return -1;
        } else if (indexB >= 0) {
          return 1;
        }

        return 0;
      });

      if (orderField) {
        result = result.map((value, ind) => {
          const record = records?.find((x) => x.id === value.id);
          // Compute order field
          const {
            $version,
            version = $version,
            [orderField]: prevOrder = record?.[orderField],
            ...rest
          } = value;
          const nextOrder = ind + 1;

          return prevOrder === nextOrder
            ? value
            : {
                ...record,
                ...rest,
                version,
                [orderField]: nextOrder,
                _dirty: true,
              };
        });
      }

      return result;
    },
    [orderField, records],
  );

  const clearSelection = useCallback(() => {
    setState((draft) => {
      draft.selectedRows = null;
      draft.selectedCell = null;
    });
  }, [setState]);

  const shouldSyncSelect = useRef(true);
  const onRowSelectionChange = useAtomCallback(
    useCallback(
      (get, set, selection: number[]) => {
        const items = getItems(get(valueAtom));
        if (items.length === 0) return;

        const ids = selection
          .map((x) => state.rows[x]?.record?.id as number)
          .filter(Boolean);

        const cur = items
          .filter((x) => x.selected)
          .map((x) => x.id!)
          .filter(Boolean);

        const a1 = [...ids].sort();
        const b1 = [...cur].sort();

        if (isEqual(a1, b1)) {
          return;
        }

        const next = items.map((x) => ({
          ...x,
          selected:
            ids.includes(x.id!) || serverSelectedIdsRef.current.includes(x.id!),
        }));

        shouldSyncSelect.current = false;
        valueRef.current = next;
        set(valueAtom, next, false, false);
      },
      [getItems, state.rows, valueAtom],
    ),
  );

  const syncSelection = useCallback(async () => {
    const ids = serverSelectedIdsRef.current;

    setState((draft) => {
      draft.selectedRows = state.rows
        .map((row, i) => {
          if (row.record?.selected || ids.includes(row.record?.id)) {
            return i;
          }
          return undefined;
        })
        .filter((x) => x !== undefined) as number[];
    });

    serverSelectedIdsRef.current = [];
  }, [setState, state.rows]);

  useEffect(() => {
    if (shouldSyncSelect.current || serverSelectedIdsRef.current.length > 0) {
      syncSelection();
    }
    shouldSyncSelect.current = true;
  }, [syncSelection]);

  const rawValue = useAtomValue(valueAtom);
  useEffect(() => {
    if (!isEqual(rawValue, lastUpdatedValueRef.current)) {
      const selectedIds = getItems(rawValue)
        .filter((item) => item.selected)
        .map((item) => item.id!);
      serverSelectedIdsRef.current = selectedIds;
    }
  }, [getItems, rawValue]);

  const columnNames = useGridColumnNames({
    view: viewData?.view ?? schema,
    fields: viewData?.fields ?? fields,
  });

  const onSearch = useAtomCallback(
    useCallback(
      async (get, set, options?: SearchOptions) => {
        // avoid search for internal value changes
        if (!shouldSearch.current) {
          shouldSearch.current = true;
          return;
        }
        const items = getItems(get(valueAtom));
        let names = options?.fields ?? dataStore.options.fields ?? columnNames;

        if (expandFieldList.length) {
          names = uniq([...names, ...expandFieldList]);
        }
        const ids = items
          .filter(
            (v) => (v.id ?? 0) > 0 && v.version === undefined && !v._fetched,
          )
          .map((v) => v.id);

        let records: DataRecord[] = [];
        let page = dataStore.page;

        if (ids.length > 0) {
          const res = await dataStore.search({
            ...options,
            limit: -1,
            offset: 0,
            sortBy: orderBy?.split?.(/\s*,\s*/),
            fields: names,
            filter: {
              ...options?.filter,
              _archived: true,
              _domain: "self.id in (:_field_ids)",
              _domainContext: {
                _model: model,
                _field: name,
                _field_ids: ids as number[],
                _parent: {
                  id: parentId,
                  _model: parentModel,
                },
              },
            },
          });
          page = res.page;
          records = res.records;
        }

        const fetchedIds = records.map((r) => r.id);

        const unfetchedItems = items.reduce((obj, item, index) => {
          if (!fetchedIds.includes(item.id)) {
            obj[index] = item;
          }
          return obj;
        }, {});

        const newItems = records
          .map((record) => {
            const item = items.find((item) => item.id === record.id);
            return item
              ? nestedToDotted({ ...record, ...item }, record)
              : record;
          })
          .concat(Object.values(unfetchedItems));

        // reset orderby on search
        setState((draft) => {
          draft.orderBy = null;
        });

        let currNewEditRecord: DataRecord;
        if (isCollectionTree) {
          const state = get(gridAtom);
          const editRecord = state.rows?.[state.editRow?.[0] ?? -1]?.record;
          if (editRecord && (editRecord.id ?? 0) < 0 && !editRecord._dirty) {
            currNewEditRecord = editRecord;
          }
        }
        recordsSyncRef.current = true;
        setRecords((records) => {
          const newRecords = newItems.map((item, index) => {
            return unfetchedItems[index]
              ? nestedToDotted({ ...records[index], ...item, cid: item.cid })
              : item;
          });
          const newIds = newRecords.map((r) => r.id);
          const recIds = records.map((r) => r.id);
          const ids = [
            ...recIds.filter(
              (id) => newIds.includes(id) || id === currNewEditRecord?.id,
            ), // preserve existing record order
            ...newIds.filter((id) => !recIds.includes(id)), // append new record
          ];
          return ids
            .map((id) =>
              currNewEditRecord?.id === id
                ? currNewEditRecord
                : newRecords.find((r) => r.id === id),
            )
            .filter((rec) => rec) as DataRecord[];
        });

        return {
          page,
          records,
        } as SearchResult;
      },
      [
        getItems,
        valueAtom,
        gridAtom,
        dataStore,
        columnNames,
        orderBy,
        model,
        name,
        parentId,
        parentModel,
        setState,
        isCollectionTree,
        expandFieldList,
      ],
    ),
  );

  const onExport = useCallback(async () => {
    const { fileName } = await dataStore.export({
      ...(state.orderBy && {
        sortBy: state.orderBy?.map(
          (column) => `${column.order === "desc" ? "-" : ""}${column.name}`,
        ),
      }),
      fields: getExportFieldNames(state.columns),
    });
    download(
      `ws/rest/${dataStore.model}/export/${fileName}?fileName=${fileName}`,
      fileName,
    );
  }, [dataStore, state.columns, state.orderBy]);

  const valueRef = useRef<DataRecord[] | null>();
  const forceRefreshRef = useRef(false);
  const [shouldReorder, setShouldReorder] = useState(false);

  const formRecordId = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record.id), [formAtom]),
  );

  const resetValue = useCallback(() => {
    valueRef.current = null;
    forceRefreshRef.current = true;
  }, []);

  const setRefresh = useFormActiveHandler();

  // Reset value ref on form record change, so that we can distinguish
  // between setting initial value and value change.
  useEffect(resetValue, [formRecordId, resetValue]);

  useEffect(() => {
    const last = valueRef.current ?? [];
    const next = value ?? [];

    if (
      forceRefreshRef.current ||
      last.length !== next.length ||
      last.some((x) => {
        const y = next.find((d) => d.id === x.id);
        return y === undefined || !equals(x, y) || x.selected !== y.selected;
      })
    ) {
      forceRefreshRef.current = false;
      const prevValue = valueRef.current;
      valueRef.current = value;
      setRefresh(async () => {
        await onSearch(dataStore.options);
        if (prevValue && orderField) {
          setShouldReorder(true);
        }
      });
    }
  }, [dataStore.options, onSearch, orderField, value, setRefresh]);

  const reorder = useAtomCallback(
    useCallback(
      (get, set) => {
        if (!orderField) return;
        const prevItems = getItems(get(valueAtom));
        const nextItems = prevItems
          .sort((a, b) => {
            let orderA = a[orderField];
            if ((a.id ?? 0) <= 0 && !orderA) {
              orderA = Infinity;
            }

            let orderB = b[orderField];
            if ((b.id ?? 0) <= 0 && !orderB) {
              orderB = Infinity;
            }

            return orderA - orderB;
          })
          .map(
            (item, ind) =>
              ({
                ...records.find((r) => r.id === item.id),
                ...item,
                [orderField]: ind + 1,
              }) as DataRecord,
          );

        if (
          !isEqual(
            records.map((x) => x[orderField]),
            nextItems.map((x) => x[orderField]),
          )
        ) {
          valueRef.current = nextItems;
          set(valueAtom, nextItems);
          setRecords((prevRecords) =>
            nextItems.map((item) =>
              nestedToDotted({
                ...prevRecords.find((record) => record.id === item.id),
                ...item,
              }),
            ),
          );
        }
      },
      [getItems, orderField, valueAtom, records],
    ),
  );

  useEffect(() => {
    if (shouldReorder) {
      reorder();
      setShouldReorder(false);
    }
  }, [reorder, shouldReorder]);

  const handleSelect = useAtomCallback(
    useCallback(
      (
        get,
        set,
        records: DataRecord[],
        {
          select = true,
          change = true,
          dirty: _dirty = true,
        }: { select?: boolean; change?: boolean; dirty?: boolean } = {},
      ) => {
        const prevItems = getItems(get(valueAtom));

        const items = records.map((item) => {
          if (isManyToMany && item.id && item.id > 0) {
            const { version: $version, ...rest } = item;
            item = { ...rest, $version };
          }
          return !select || item.selected ? item : { ...item, selected: true };
        });

        const newItems = items.filter(
          (x) => !prevItems.some((y) => y.id === x.id),
        );

        const nextItems = reorderItems([
          ...prevItems.map((item) => {
            const record = items.find((r) => r.id === item.id);
            return record ? { ...item, ...record } : item;
          }),
          ...newItems.map((item) => {
            if (isManyToMany && (item.id ?? 0) > 0) {
              return { ...item, version: undefined };
            }
            return item;
          }),
        ]);

        const changed =
          change && (!isManyToMany || prevItems.length !== nextItems.length);
        const dirty =
          _dirty &&
          (prevItems.length !== nextItems.length ||
            nextItems.some((item) => item._dirty));

        return setValue(nextItems, changed, dirty);
      },
      [getItems, reorderItems, valueAtom, isManyToMany, setValue],
    ),
  );

  const getActionContext = useCallback(
    () => ({
      _viewType: "grid",
      _views: [{ type: "grid", name: gridView }],
      ...(selectedIdsRef.current?.length > 0 && {
        _ids: selectedIdsRef.current,
      }),
      _parent: getContext(),
    }),
    [getContext, gridView],
  );

  const actionView = useMemo(
    () =>
      viewData?.view ??
      ({
        name: gridView,
        model: model,
      } as View),
    [viewData?.view, gridView, model],
  );

  const parentScope = useFormScope();

  const onParentRefresh = useCallback(
    (target?: string) => parentScope.actionHandler.refresh(target),
    [parentScope.actionHandler],
  );
  const onParentSave = useCallback(
    (record?: DataRecord) => parentScope.actionHandler.save(record),
    [parentScope.actionHandler],
  );

  const actionExecutor = useActionExecutor(actionView, {
    formAtom: null,
    getContext: getActionContext,
    onRefresh: onParentRefresh,
    onSave: onParentSave,
  });

  const [beforeSelect] = useBeforeSelect(schema, getContext);

  const openEditor = useCallback(
    (
      options?: Partial<EditorOptions>,
      onSelect?: (record: DataRecord) => void,
      onSave?: (record: DataRecord) => void,
    ) => {
      const { record } = options || {};
      const { id } = record ?? {};
      if (showEditorInTab && (id ?? 0) > 0) {
        return showEditorInTab(record!, options?.readonly ?? false);
      }
      showEditor({
        title: title ?? "",
        model,
        record: { id: null },
        readonly: false,
        viewName: formView,
        context: {
          _parent: getContext(),
        },
        ...(isManyToMany
          ? { onSelect }
          : {
              onSave: (record) =>
                onSave?.({
                  ...record,
                  $id: id,
                  ...(isCollectionTree && {
                    _changed: true,
                    _original: options?.record || {},
                  }),
                }),
              checkDirty: false,
            }),
        ...options,
      });
    },
    [
      showEditor,
      showEditorInTab,
      title,
      model,
      formView,
      getContext,
      isManyToMany,
      isCollectionTree,
    ],
  );

  const onSave = useCallback(
    async (
      record: DataRecord,
      options: { select?: boolean; change?: boolean; dirty?: boolean } = {},
    ) => {
      const { $id, ...rest } = record;
      const id = record.id ?? $id ?? nextId();
      record = { ...rest, id, cid: id };

      setState((draft) => {
        if (draft.editRow) {
          const [rowIndex] = draft.editRow;
          draft.editRow = null;

          if (draft.rows[rowIndex]) {
            draft.rows[rowIndex].record = record;
          }
        }
      });
      await handleSelect([record], options);
      setDetailRecord((details) =>
        details?.id === record.id ? record : details,
      );
      return record;
    },
    [handleSelect, setState],
  );

  const onSaveRef = useRef(onSave);
  onSaveRef.current = onSave;

  const onO2MSave = useCallback((record: DataRecord) => {
    return onSaveRef.current(record, { select: false });
  }, []);

  const onO2MUpdate = useCallback((record: DataRecord) => {
    return onSaveRef.current(record, {
      select: false,
      change: true,
      dirty: false,
    });
  }, []);

  const onAdd = useCallback(() => {
    openEditor({}, (record) => handleSelect([record]), onSave);
  }, [openEditor, onSave, handleSelect]);

  const handleAddInGrid = useCallback(
    async (record?: DataRecord) => {
      const gridHandler = gridRef.current;
      if (isCollectionTree) {
        await gridHandler?.commit?.();
        await waitForCollectionActions?.();

        // clear sorting if applied
        setState((draft) => {
          draft.orderBy = null;
        });

        const newRecord = {
          id: nextId(),
          ...expandFieldList.reduce(
            (obj, field) => ({ ...obj, [field]: [] }),
            {} as DataRecord,
          ),
          ...getDefaultValues(gridViewFields, gridViewData.items),
        };
        setRecords(
          (records) =>
            (record
              ? records?.reduce(
                  (_records: DataRecord[], rec) =>
                    rec.id === record.id
                      ? [..._records, rec, newRecord]
                      : [..._records, rec],
                  [],
                )
              : [...(records || []), newRecord]) as DataRecord[],
        );
        setState((draft) => {
          const recIndex = record
            ? draft.rows.findIndex((r) => r.key === record.id)
            : -1;
          const lastIndex = draft.rows?.length ?? 0;
          draft.editRow = [recIndex > -1 ? recIndex + 1 : lastIndex, null];
        });
      } else {
        gridHandler?.onAdd?.();
      }
    },
    [
      expandFieldList,
      setRecords,
      setState,
      isCollectionTree,
      gridViewFields,
      gridViewData,
      waitForCollectionActions,
    ],
  );

  const onAddInGrid = useCallback(
    (e: any) => {
      e?.preventDefault?.();
      handleAddInGrid();
    },
    [handleAddInGrid],
  );

  const onAddInDetail = useCallback(() => {
    setDetailRecord({ id: nextId() });
  }, []);

  const onSelect = useCallback(async () => {
    const _domain = await beforeSelect(domain, true);
    const _domainContext = _domain ? getContext() : {};
    showSelector({
      model,
      multiple: true,
      viewName: gridView,
      orderBy: orderBy,
      domain: _domain,
      context: _domainContext,
      limit: searchLimit,
      ...(canNew && {
        onCreate: onAdd,
      }),
      onSelect: handleSelect,
    });
  }, [
    canNew,
    onAdd,
    showSelector,
    orderBy,
    model,
    gridView,
    domain,
    searchLimit,
    getContext,
    beforeSelect,
    handleSelect,
  ]);

  const isPermitted = usePermitted(model, perms);

  const onEdit = useCallback(
    async (record: DataRecord, readonly = false) => {
      if (!(await isPermitted(record, readonly))) {
        return;
      }
      openEditor(
        { record, readonly },
        (record) => handleSelect([record]),
        onSave,
      );
    },
    [isPermitted, openEditor, onSave, handleSelect],
  );

  const onView = useCallback(
    (record: DataRecord) => {
      onEdit(record, true);
    },
    [onEdit],
  );

  const onDelete = useCallback(
    async (records: GridRow["record"][]) => {
      const confirmed = await dialogs.confirm({
        content: i18n.get(
          "Do you really want to delete the selected record(s)?",
        ),
        yesTitle: i18n.get("Delete"),
      });
      if (confirmed) {
        const ids = records.map((r) => r.id);
        setValue((value) =>
          reorderItems((value || []).filter(({ id }) => !ids.includes(id))),
        );
        clearSelection();
      }
    },
    [setValue, clearSelection, reorderItems],
  );

  const updateViewDirty = useUpdateViewDirty(formAtom);

  const onCloseInDetail = useCallback(() => {
    setDetailRecord(null);
    gridRef.current?.form?.current?.onCancel?.();
    panelRef.current?.scrollIntoView?.({ behavior: "smooth" });
    updateViewDirty();
  }, [updateViewDirty]);

  const onRowReorder = useCallback(() => {
    reorderRef.current = true;
  }, []);

  const hasRowSelected = !!selectedRows?.length;
  const hasMasterDetails = toKebabCase(widget) === "master-detail";
  const editRecord =
    editableAndCanEdit &&
    hasMasterDetails &&
    editRow &&
    rows?.[editRow?.[0]]?.record;
  const selected =
    editRecord ||
    ((selectedRows?.length ?? 0) > 0
      ? rows?.[selectedRows?.[0] ?? -1]?.record
      : null);
  const recordId = detailRecord?.id;

  const detailFormName =
    (summaryView === "true" ? null : summaryView) || formView;

  const { data: detailMeta } = useAsync(async () => {
    if (!hasMasterDetails) return;
    const meta = await findView<FormView>({
      type: "form",
      name: detailFormName,
      model,
    });
    return {
      ...meta,
      widgetSchema: schema,
    };
  }, [model, detailFormName]);

  useEffect(() => {
    const savedId = saveIdRef.current;
    if (savedId) {
      const savedInd = rows?.findIndex((r) => r.record?.id === savedId);
      savedInd > 0 &&
        setState((draft) => {
          draft.selectedCell = [savedInd, 0];
          draft.selectedRows = [savedInd];
        });
      saveIdRef.current = null;
    }
  }, [selectedRows, rows, setState]);

  useEffect(() => {
    const selectedIds = (selectedRows ?? []).map(
      (ind) => rows[ind]?.record?.id,
    );
    if (isEqual(selectedIdsRef.current, selectedIds)) return;

    selectedIdsRef.current = selectedIds;
    setSelection(selectedIds);
  }, [selectedRows, rows, setSelection]);

  useEffect(() => {
    if (reorderRef.current) {
      // For dummy fields, so that internal value change is detected
      if (!orderField) {
        resetValue();
      }

      setValue(
        (values) => {
          const valIds = values.map((v) => v.id);
          return rows
            .filter((r) => valIds.includes(r.record?.id ?? 0))
            .map((r, ind) => {
              const item = values.find((v) => v.id === r.record?.id);
              return {
                ...r.record,
                ...item,
                _dirty: true,
                ...(orderField && { [orderField]: ind + 1 }),
                version: item?.version ?? item?.$version,
              };
            }) as DataRecord[];
        },
        false,
        true,
        true,
      );
    }
    reorderRef.current = false;
  }, [orderField, resetValue, rows, setValue]);

  const fetchAndSetDetailRecord = useCallback(
    async (selected: DataRecord) => {
      let record = selected?.id ? selected : null;
      if (detailMeta && record?.id && !record._dirty) {
        record = await fetchRecord(detailMeta, dataStore, record.id);
      }
      setDetailRecord(record);
    },
    [detailMeta, dataStore],
  );

  const onRowClick = useCallback(
    (e: SyntheticEvent, row: GridRow) => {
      selected?.id === row?.record?.id && fetchAndSetDetailRecord(row.record);
    },
    [selected, fetchAndSetDetailRecord],
  );

  const onDuplicate = useCallback(async () => {
    if (selected?.id) {
      const rec =
        selected.id < 0 ? { ...selected } : await dataStore.copy(selected.id);
      const newId = nextId();
      const maxOrder = orderField
        ? records
            .map((r) => r[orderField] as number | null | undefined)
            .reduce((a, b) => ((a ?? 0) > (b ?? 0) ? a : b) ?? 0)
        : null;
      setValue((values) => [
        ...values,
        {
          ...rec,
          _dirty: true,
          id: newId,
          ...(orderField && maxOrder != null && { [orderField]: maxOrder + 1 }),
        },
      ]);
      saveIdRef.current = newId;
    }
  }, [dataStore, orderField, records, selected, setValue]);

  const onSaveRecord = useCallback(
    async (record: DataRecord) => {
      const fieldList = Object.keys(viewData?.fields ?? fields);
      const res = await dataStore.save(record, {
        fields: fieldList,
      });
      return res && onSave(res);
    },
    [viewData?.fields, fields, dataStore, onSave],
  );

  const onRefreshDetailsRecord = useCallback(() => {
    detailRecord && fetchAndSetDetailRecord(detailRecord);
  }, [detailRecord, fetchAndSetDetailRecord]);

  useAsyncEffect(async () => {
    if (!detailMeta || recordId === selected?.id) return;
    fetchAndSetDetailRecord(selected);
  }, [detailMeta, selected?.id, fetchAndSetDetailRecord]);

  const onRefresh = useCallback(() => {
    resetValue();
  }, [resetValue]);

  useFormRefresh(onRefresh);

  const onDiscard = useCallback(
    (record: DataRecord) => {
      if (isCollectionTree) {
        if ((record.id ?? -1) < 0 && !record._dirty) {
          setRecords((records) =>
            records.filter((rec) => rec.id !== record.id),
          );
        }
      }
      updateViewDirty();
    },
    [isCollectionTree, updateViewDirty],
  );

  const hasActions = showBars && (toolbar?.length || menubar?.length);
  const rowSize = 40;
  const headerSize = isSubTreeGrid ? rowSize : 75;
  const maxHeight = headerSize + (+height > 0 ? +height : 10) * rowSize;
  const changed = useMemo(() => value?.some((x) => x._dirty), [value]);
  const allowSorting = !canMove && !changed;
  const allowGrouping = !canMove;
  const allowRowReorder = canMove && !readonly;
  const canShowHeader = !isSubTreeGrid; // widgetAttrs?.showHeader !== "false";
  const noHeader =
    !canShowHeader &&
    (readonly || (value?.length ?? 0) > 0 || (records?.length ?? 0) > 0);
  const canShowTitle =
    !noHeader && (schema.showTitle ?? widgetAttrs?.showTitle ?? true);
  const canExpandAll = isRootTreeGrid && widgetAttrs?.expandAll !== "false";

  const selectFieldsAtom = useMemo(() => {
    return focusAtom(
      formAtom,
      (o) => o.select?.[name] ?? {},
      (o, v) => ({
        ...o,
        select: {
          ...o.select,
          [name]: {
            ...o.select?.[name],
            ...v,
          },
        },
      }),
    );
  }, [name, formAtom]);

  const setSelectFields = useSetAtom(selectFieldsAtom);

  const syncRecordsToTree = useAtomCallback(
    useCallback(
      (get, set, records: DataRecord[]) => {
        const values = getItems(get(valueAtom)).map((item) => {
          const found = records.find((r) => r.id === item.id);
          return found ? { ...item, ...found } : item;
        });

        if (isRootTreeGrid) {
          const selectFields = get(selectFieldsAtom) || {};
          const allItems: DataRecord[] = [
            ...values.map((v) => ({ ...v, _model: model })),
          ];

          values.forEach((value) => {
            const list = Object.keys(selectFields)
              .filter((f) => selectFields[f]?._model)
              .reduce(
                (_items, key) => [
                  ..._items,
                  ...(flattenItems(value, key).map((v: DataRecord) => ({
                    ...v,
                    _model: selectFields[key]?._model,
                  })) as DataRecord[]),
                ],
                [] as DataRecord[],
              );
            allItems.push(...list);
          });

          const collectionItems = produce(get(itemsAtom), (draft) => {
            allItems.forEach((item) => {
              const found = draft.find(
                (v) =>
                  v.model === item._model &&
                  (v.record.id === item.id || v.record.id === item.cid),
              );
              if (found) {
                found.record = { ...found.record, ...item };
              } else {
                draft.push({ record: { ...item }, model });
              }
            });
          });

          set(itemsAtom, collectionItems);
        }
      },
      [isRootTreeGrid, valueAtom, selectFieldsAtom, itemsAtom, getItems, model],
    ),
  );

  const hasRowExpanded = useAtomCallback(
    useCallback(
      (get, set, { record }: GridRow) => {
        if (isTreeLimitExceed) return null;

        const isNew = (record.id ?? 0) < 0;
        const itemState = get(itemsAtom).find(
          (item) =>
            item.record.id === record.cid || item.record.id === record.id,
        );
        if (isNew || itemState?.expanded === false)
          return Boolean(itemState?.expanded);

        const noChildren =
          expandFieldList.length > 0 &&
          expandFieldList.every(
            (field) =>
              record[field] !== undefined && record[field]?.length === 0,
          );

        if (readonly && noChildren) {
          return null;
        }

        if (expandAll && noChildren) {
          return itemState?.expanded ?? false;
        }
        return expandAll || itemState?.expanded;
      },
      [isTreeLimitExceed, expandAll, itemsAtom, expandFieldList, readonly],
    ),
  );

  const onRowExpand = useAtomCallback(
    useCallback(
      (get, set, row: GridRow, expand?: boolean) => {
        const noSubItems =
          expandFieldList.length > 0 &&
          expandFieldList.every(
            (field) =>
              row.record?.[field] !== undefined &&
              row.record?.[field]?.length === 0,
          );
        const itemAtom = getItem(row.record?.id ?? 0, model);
        itemAtom &&
          set(itemAtom, (state) => ({
            ...state,
            expanded: noSubItems && !expand ? undefined : Boolean(expand),
          }));
      },
      [getItem, model, expandFieldList],
    ),
  );

  useEffect(() => {
    if (recordsSyncRef.current) {
      recordsSyncRef.current = false;
      if (expandable && isRootTreeGrid) {
        syncRecordsToTree(records);
      }
    }
  }, [isRootTreeGrid, expandable, records, syncRecordsToTree]);

  useEffect(() => {
    const fieldsSelect = gridViewData?.items
      ?.filter((item) => item.type === "field" && item.name)
      .reduce((_select: Record<string, any>, item) => {
        const { target, targetName } = item as Field;
        return {
          ..._select,
          [item.name!]: target && targetName ? { [targetName]: true } : true,
        };
      }, {});
    setSelectFields((state: any) => ({ ...state, ...fieldsSelect }));
  }, [setSelectFields, gridViewData?.items]);

  const expandableContext = useMemo(
    () => ({ selectAtom: selectFieldsAtom, level: expandLevel + 1 }),
    [expandLevel, selectFieldsAtom],
  );

  const expandableView = useMemo(() => {
    if (isTreeGrid) {
      const treeField = schema.widgetAttrs?.treeField;
      const treeFieldTitle = schema.widgetAttrs?.treeFieldTitle ?? schema.title;
      return {
        model,
        fields: {
          [treeField]: {
            ...pick(schema, ["target", "targetName"]),
            type: schema.serverType,
            name: treeField,
            title: treeFieldTitle,
          },
        },
        view: {
          type: "form",
          model: model,
          items: [
            {
              ...schema,
              onChange: undefined,
              title: treeFieldTitle,
              uid: uniqueId("w"),
              name: treeField,
            },
          ],
        },
      } as ViewData<FormView>;
    }
    return summaryView ?? formView;
  }, [schema, model, summaryView, formView, isTreeGrid]);

  return (
    <>
      <Panel
        ref={panelRef}
        className={clsx(styles.container, {
          [styles.toolbar]: hasActions,
          [styles.rootTreeGrid]: isRootTreeGrid,
          [styles.subTreeGrid]: isSubTreeGrid,
          [styles.tree]: isTreeGrid,
          [styles.hasHeader]: !isRootTreeGrid && canShowHeader,
          [styles.hasNewHeader]: isTreeGrid && !state.rows?.length,
          [styles.noHeader]: noHeader,
          [styles.noTitle]: !canShowTitle,
        })}
        header={
          <div className={styles.title}>
            <div className={styles.titleText}>
              {canShowTitle && (
                <FieldLabel
                  schema={schema}
                  formAtom={formAtom}
                  widgetAtom={widgetAtom}
                />
              )}
            </div>
            {hasActions && (
              <ToolbarActions
                buttons={toolbar}
                menus={menubar}
                actionExecutor={actionExecutor}
              />
            )}
          </div>
        }
        toolbar={{
          iconOnly: true,
          items: [
            {
              key: "select",
              text: i18n.get("Select"),
              iconProps: {
                icon: "search",
              },
              onClick: onSelect,
              hidden: !canSelect,
            },
            {
              key: "new",
              text: i18n.get("New"),
              iconProps: {
                icon: "add",
              },
              onClick: editable ? onAddInGrid : onAdd,
              hidden: !canNew,
            },
            {
              key: "edit",
              text: i18n.get("Edit"),
              iconProps: {
                icon: "edit",
              },
              disabled: !hasRowSelected,
              hidden: !canEdit || !hasRowSelected,
              onClick: () => {
                const [rowIndex] = selectedRows || [];
                const record = rows[rowIndex]?.record;
                record && onEdit(record);
              },
            },
            {
              key: "delete",
              text: i18n.get("Delete"),
              iconProps: {
                icon: "delete",
              },
              disabled: !hasRowSelected,
              hidden: !canDelete || !hasRowSelected,
              onClick: () => {
                onDelete(selectedRows!.map((ind) => rows[ind]?.record));
              },
            },
            {
              key: "more",
              iconOnly: true,
              hidden: !canCopy && !canExport,
              iconProps: {
                icon: "arrow_drop_down",
              },
              items: [
                {
                  key: "duplicate",
                  text: i18n.get("Duplicate"),
                  hidden: !canDuplicate,
                  onClick: onDuplicate,
                },
                {
                  key: "export",
                  text: i18n.get("Export"),
                  hidden: !canExport,
                  onClick: onExport,
                },
              ],
            },
          ],
        }}
        style={{
          minHeight:
            headerSize +
            (isSubTreeGrid ? (readonly ? -headerSize : 0) : 2 * rowSize), // min 2 rows
          ...(!expandable && {
            maxHeight, // auto height to the max rows to display
          }),
        }}
      >
        <GridExpandableContext.Provider value={expandableContext}>
          <ScopeProvider scope={MetaScope} value={viewMeta}>
            <GridComponent
              className={clsx(styles["grid"], {
                [styles.noRecords]: records.length === 0,
              })}
              ref={gridRef}
              allowGrouping={allowGrouping}
              allowSorting={allowSorting}
              allowRowReorder={allowRowReorder}
              showEditIcon={canEdit || canView}
              {...(isTreeGrid && {
                showAsTree: true,
                showNewIcon: canNew,
                showDeleteIcon: canDelete,
              })}
              readonly={readonly || !canEdit}
              editable={editable && (canEdit || canNew)}
              records={records}
              expandable={expandable}
              expandableView={expandableView}
              view={gridViewData}
              fields={gridViewFields}
              perms={perms}
              columnAttrs={columnAttrs}
              state={state}
              setState={setState}
              actionExecutor={actionExecutor}
              hasRowExpanded={hasRowExpanded}
              onFormInit={forceUpdate}
              onEdit={canEdit ? onEdit : canView ? onView : noop}
              onView={canView ? (canEdit ? onEdit : onView) : noop}
              onUpdate={isManyToMany ? onSave : onO2MUpdate}
              onDelete={onDelete}
              onSave={isManyToMany ? onSaveRecord : onO2MSave}
              onDiscard={onDiscard}
              onRowExpand={onRowExpand}
              onRowReorder={onRowReorder}
              onRowSelectionChange={onRowSelectionChange}
              {...(canNew && {
                onNew: editableAndCanEdit ? handleAddInGrid : onAdd,
              })}
              {...(canDelete && {
                onDelete: onDelete,
              })}
              {...(!canNew &&
                editableAndCanEdit && {
                  onRecordAdd: undefined,
                })}
              {...(hasMasterDetails &&
                selected &&
                !detailRecord && {
                  onRowClick,
                })}
              {...(isSubTreeGrid && {
                headerRowRenderer: HideGridHeaderRow,
                allowColumnCustomize: false,
                allowColumnHide: false,
              })}
              {...(canExpandAll && {
                headerCellRenderer: CustomGridHeaderCell,
              })}
              onColumnCustomize={onColumnCustomize}
            />
          </ScopeProvider>
        </GridExpandableContext.Provider>
        {hasMasterDetails && detailMeta ? (
          <Box d="flex" flexDirection="column" p={2}>
            {(!editableAndCanEdit || selected) && (
              <ScopeProvider scope={MetaScope} value={detailMeta}>
                <DetailsForm
                  meta={detailMeta}
                  readonly={readonly || editableAndCanEdit}
                  parent={formAtom}
                  record={detailRecord}
                  formAtom={gridRef.current?.form?.current?.formAtom}
                  onRefresh={onRefreshDetailsRecord}
                  onClose={onCloseInDetail}
                  onSave={isManyToMany ? onSaveRecord : onSave}
                  {...(canNew && { onNew: onAddInDetail })}
                />
              </ScopeProvider>
            )}
          </Box>
        ) : null}
      </Panel>
      <FieldError widgetAtom={widgetAtom} />
    </>
  );
}

type GridHeaderCellProps = GridColumnProps & HTMLAttributes<HTMLDivElement>;
type GridHeaderCellRef = ForwardedRef<HTMLDivElement>;

const GridHeaderCell = forwardRef(function GridHeaderCell(
  props: GridHeaderCellProps,
  ref: GridHeaderCellRef,
) {
  const { title, className, children, style, onClick } = props;
  return (
    <div ref={ref} {...{ title, className, style, onClick }}>
      {children && typeof children === "object" ? (
        children
      ) : (
        <span>{children}</span>
      )}
    </div>
  );
});

const ExpandHeaderCell = forwardRef(function ExpandHeaderCell(
  props: GridHeaderCellProps,
  ref: GridHeaderCellRef,
) {
  const { items: itemsAtom, expand: expandAtom } = useCollectionTree();

  const expandAll = useAtomValue(expandAtom);
  const handleClick = useAtomCallback(
    useCallback(
      (get, set, expanded: boolean) => {
        const items = get(itemsAtom).map((item) =>
          item.expanded === expanded || item.expanded === undefined
            ? item
            : {
                ...item,
                expanded,
              },
        );
        set(itemsAtom, items);
        set(expandAtom, expanded);
      },
      [expandAtom, itemsAtom],
    ),
  );

  return (
    <GridHeaderCell
      ref={ref}
      {...props}
      title={expandAll ? i18n.get("Collapse All") : i18n.get("Expand All")}
      onClick={() => handleClick(!expandAll)}
    >
      <ExpandIcon expand={expandAll} />
    </GridHeaderCell>
  );
});

const CustomGridHeaderCell = forwardRef(function CustomGridHeaderCell(
  props: GridHeaderCellProps,
  ref: GridHeaderCellRef,
) {
  const { data } = props;

  if (data.type === "row-expand") {
    return <ExpandHeaderCell ref={ref} {...props} />;
  }

  return <GridHeaderCell ref={ref} {...props} />;
});

function HideGridHeaderRow() {
  return null;
}
