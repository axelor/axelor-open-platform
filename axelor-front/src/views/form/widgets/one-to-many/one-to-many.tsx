import { atom, useAtom, useAtomValue, useSetAtom } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { selectAtom, useAtomCallback } from "jotai/utils";

import getNested from "lodash/get";
import isEqual from "lodash/isEqual";
import {
  SetStateAction,
  SyntheticEvent,
  useCallback,
  useEffect,
  useMemo,
  useReducer,
  useRef,
  useState,
} from "react";

import { Box, Panel, clsx } from "@axelor/ui";
import { GridRow } from "@axelor/ui/grid";

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
  FormView,
  GridView,
  Property,
  View,
} from "@/services/client/meta.types";
import { focusAtom } from "@/utils/atoms";
import { download } from "@/utils/download";
import { toKebabCase } from "@/utils/names";
import { ToolbarActions } from "@/view-containers/view-toolbar";
import { MetaScope } from "@/view-containers/views/scope";
import { Grid as GridComponent, GridHandler } from "@/views/grid/builder";
import { useGridColumnNames } from "@/views/grid/builder/scope";
import { isValidSequence, useGridState } from "@/views/grid/builder/utils";

import {
  FieldError,
  FieldLabel,
  FieldProps,
  usePermission,
  usePrepareContext,
  usePrepareWidgetContext,
} from "../../builder";
import {
  useActionExecutor,
  useAfterActions,
  useFormRefresh,
  useFormScope,
} from "../../builder/scope";
import { nextId } from "../../builder/utils";
import { fetchRecord } from "../../form";
import { DetailsForm } from "./one-to-many.details";

import styles from "./one-to-many.module.scss";

const noop = () => {};

export function OneToMany(props: FieldProps<DataRecord[]>) {
  const { schema } = props;
  const { target: model, gridView } = schema;

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
          "editable",
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

  return <OneToManyInner {...props} viewData={data} />;
}

function OneToManyInner({
  schema,
  valueAtom,
  widgetAtom,
  formAtom,
  viewData,
  ...props
}: FieldProps<DataRecord[]> & {
  viewData?: ViewData<GridView>;
}) {
  const {
    widgetAttrs,
    name,
    showBars = widgetAttrs?.showBars,
    toolbar = viewData?.view?.toolbar,
    menubar = viewData?.view?.menubar,
    target: model,
    fields,
    formView,
    summaryView,
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
  const reorderRef = useRef(false);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const gridRef = useRef<GridHandler>(null);
  const saveIdRef = useRef<number | null>();

  const [records, setRecords] = useState<DataRecord[]>([]);
  const [detailRecord, setDetailRecord] = useState<DataRecord | null>(null);
  const [, forceUpdate] = useReducer(() => ({}), {});

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
        ? { ...x, _dirty: true, id: nextId() }
        : x,
    );
    return lastItemsRef.current;
  }, []);

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
            const values =
              typeof setter === "function" ? setter(get(valueAtom)!) : setter;
            const valIds = (values || []).map((v) => v.id);

            setRecords((records) => {
              if (resetRecords) {
                return values
                  .map((v) => records.find((r) => r.id === v.id))
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
                  return val ? { ...rec, ...val } : rec;
                })
                .concat(newRecords);
            });

            const result = await set(
              valueAtom,
              values,
              callOnChange,
              markDirty,
            );

            const hasValueChanged = (
              result as unknown as ActionResult[]
            )?.filter?.(
              ({ values, attrs }) =>
                values?.[schema.name] !== undefined ||
                attrs?.[schema.name]?.value !== undefined,
            );

            /**
             * if same o2m field values is changed on onChange event of itself
             * then have to wait for values to get updated in state
             */
            if (hasValueChanged) {
              await new Promise((resolve) => {
                setTimeout(() => resolve(true), 500);
              });
            }
          },
        ),
      [getItems, valueAtom, schema.name],
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
    toKebabCase(schema.serverType || schema.widget) === "many-to-many";

  const viewMeta = useMemo(() => {
    return (
      viewData ?? {
        view: { ...schema, type: "grid" } as GridView,
        fields,
        model,
      }
    );
  }, [fields, model, schema, viewData]);

  const getContext = usePrepareWidgetContext(schema, formAtom, widgetAtom);

  const showEditor = useEditor();
  const showEditorInTab = useEditorInTab(schema);
  const showSelector = useSelector();
  const [state, setState] = useGridState();
  const dataStore = useMemo(() => new DataStore(model), [model]);

  const { editRow, selectedRows, rows } = state;

  const canNew = !readonly && hasButton("new");
  const canEdit = !readonly && hasButton("edit");
  const canView = hasButton("view");
  const canDelete = !readonly && hasButton("remove");
  const canSelect =
    !readonly && hasButton("select") && (isManyToMany || attrs.canSelect);
  const canDuplicate = canNew && canCopy && selectedRows?.length === 1;
  const _canMove = Boolean(schema.canMove ?? viewData?.view?.canMove);

  const orderBy = schema.orderBy ?? viewData?.view?.orderBy;
  const editable =
    !readonly &&
    canEdit &&
    (schema.editable ?? widgetAttrs?.editable ?? viewData?.view?.editable);

  const formFields = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.fields), [formAtom]),
  );

  const canMove = useMemo(() => {
    if (_canMove !== true) return false;

    // With dummy fields, it is allowed to have no orderBy.
    const orderField = orderBy?.split(/\s*,\s*/)?.[0];
    if (!orderField) {
      return !formFields[name];
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

    return field && isValidSequence(field);
  }, [_canMove, orderBy, viewMeta.fields, fields, formFields, name]);

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
          selected: ids.includes(x.id!),
        }));

        shouldSyncSelect.current = false;
        set(valueAtom, next, false, false);
      },
      [getItems, state.rows, valueAtom],
    ),
  );

  const syncSelection = useAfterActions(
    useCallback(async () => {
      const items = value ?? [];
      const ids = items
        .filter((x) => x.selected)
        .map((x) => x.id!)
        .filter(Boolean);
      const selectedRows = state.rows
        .map((row, i) => {
          if (ids.includes(row.record?.id)) {
            return i;
          }
          return undefined;
        })
        .filter((x) => x !== undefined) as number[];

      setState((draft) => {
        draft.selectedRows = selectedRows;
      });
    }, [setState, state.rows, value]),
  );

  useEffect(() => {
    if (shouldSyncSelect.current) {
      syncSelection();
    }
    shouldSyncSelect.current = true;
  }, [syncSelection]);

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
        const names =
          options?.fields ?? dataStore.options.fields ?? columnNames;

        const ids = items
          .filter(
            (v) =>
              names.length === 0 ||
              names.some((n) => getNested(v, n) === undefined),
          )
          .filter((v) => (v.id ?? 0) > 0)
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
        const newItems = records
          .map((record) => {
            const item = items.find((item) => item.id === record.id);
            return item ? { ...record, ...item } : record;
          })
          .concat(items.filter((item) => !fetchedIds.includes(item.id)));

        // reset orderby on search
        setState((draft) => {
          draft.orderBy = null;
        });
        setRecords(newItems);

        return {
          page,
          records,
        } as SearchResult;
      },
      [
        getItems,
        valueAtom,
        dataStore,
        columnNames,
        orderBy,
        model,
        name,
        parentId,
        parentModel,
        setState,
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
      fields: state.columns
        .filter((c) => c.type === "field" && c.visible !== false)
        .map((c) => c.name),
    });
    download(
      `ws/rest/${dataStore.model}/export/${fileName}?fileName=${fileName}`,
      fileName,
    );
  }, [dataStore, state.columns, state.orderBy]);

  const valueRef = useRef<DataRecord[] | null>();
  useEffect(() => {
    const last = valueRef.current ?? [];
    const next = value ?? [];

    if (
      last.length !== next.length ||
      last.some((x) => {
        const y = next.find((d) => d.id === x.id);
        return y === undefined || !equals(x, y);
      })
    ) {
      valueRef.current = value;
      onSearch(dataStore.options);
    }
  }, [dataStore.options, onSearch, value]);

  const handleSelect = useAtomCallback(
    useCallback(
      (get, set, records: DataRecord[]) => {
        const prevItems = getItems(get(valueAtom));

        const items = records.map((item) => {
          if (isManyToMany && item.id && item.id > 0) {
            const { version: $version, ...rest } = item;
            item = { ...rest, $version };
          }
          return item.selected ? item : { ...item, selected: true };
        });

        const newItems = items.filter(
          (x) => !prevItems.some((y) => y.id === x.id),
        );

        const nextItems = [
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
        ];

        const changed = !isManyToMany || prevItems.length !== nextItems.length;

        return setValue(nextItems, changed, changed);
      },
      [getItems, isManyToMany, setValue, valueAtom],
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

  const actionExecutor = useActionExecutor(actionView, {
    formAtom: null,
    getContext: getActionContext,
    onRefresh: parentScope.actionHandler.refresh.bind(
      parentScope.actionHandler,
    ),
    onSave: parentScope.actionHandler.save.bind(parentScope.actionHandler),
  });

  const [beforeSelect] = useBeforeSelect(schema, getContext);

  const onSelect = useCallback(async () => {
    const _domain = (await beforeSelect()) ?? domain;
    const _domainContext = _domain ? getContext() : {};
    showSelector({
      model,
      multiple: true,
      viewName: gridView,
      orderBy: orderBy,
      domain: _domain,
      context: _domainContext,
      limit: searchLimit,
      onSelect: handleSelect,
    });
  }, [
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
          : { onSave: (record) => onSave?.({ ...record, $id: id }) }),
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
    ],
  );

  const onSave = useCallback(
    async (record: DataRecord) => {
      const { id, $id, ...rest } = record;
      record = { ...rest, _dirty: true, id: id ?? $id ?? nextId() };

      setState(draft => {
        if (draft.editRow) {
          const [rowIndex] = draft.editRow;
          draft.editRow = null;

          if (draft.rows[rowIndex]) {
            draft.rows[rowIndex].record = record;
          } 
        }
      });
      await handleSelect([record]);
      setDetailRecord((details) =>
        details?.id === record.id ? record : details,
      );
      return record;
    },
    [handleSelect, setState],
  );

  const onAdd = useCallback(() => {
    openEditor({}, (record) => handleSelect([record]), onSave);
  }, [openEditor, onSave, handleSelect]);

  const onAddInGrid = useCallback((e: any) => {
    e?.preventDefault?.();
    const gridHandler = gridRef.current;
    if (gridHandler) {
      gridHandler.onAdd?.();
    }
  }, []);

  const onAddInDetail = useCallback(() => {
    setDetailRecord({ id: nextId() });
  }, []);

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
          (value || []).filter(({ id }) => !ids.includes(id)),
        );
        clearSelection();
      }
    },
    [setValue, clearSelection],
  );

  const onCloseInDetail = useCallback(() => {
    setDetailRecord(null);
    gridRef.current?.form?.current?.onCancel?.();
    panelRef.current?.scrollIntoView?.({ behavior: "smooth" });
  }, []);

  const onRowReorder = useCallback(() => {
    reorderRef.current = true;
  }, []);

  const hasRowSelected = !!selectedRows?.length;
  const hasMasterDetails = toKebabCase(schema.widget) === "master-detail";
  const editRecord =
    editable && hasMasterDetails && editRow && rows?.[editRow?.[0]]?.record;
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
    return await findView<FormView>({
      type: "form",
      name: detailFormName,
      model,
    });
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
      const orderField = orderBy?.split(/\s*,\s*/)?.[0];

      // For dummy fields, so that internal value change is detected
      if (!orderField) {
        valueRef.current = null;
      }

      setValue(
        (values) => {
          const valIds = values.map((v) => v.id);
          return rows
            .filter((r) => valIds.includes(r.record?.id ?? 0))
            .map((r) => values.find((v) => v.id === r.record?.id))
            .map((r, ind) => ({
              ...r,
              _dirty: true,
              ...(orderField && { [orderField]: ind + 1 }),
              version: r?.version ?? r?.$version,
            })) as DataRecord[];
        },
        false,
        true,
        true,
      );
    }
    reorderRef.current = false;
  }, [rows, setValue, orderBy]);

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
      setValue((values) => [...values, { ...rec, _dirty: true, id: newId }]);
      saveIdRef.current = newId;
    }
  }, [dataStore, selected, setValue]);

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

  useFormRefresh(onSearch);

  const hasActions = showBars && (toolbar?.length || menubar?.length);

  const rowSize = 40;
  const headerSize = 75;
  const maxHeight = headerSize + (+height > 0 ? +height : 10) * rowSize;
  const changed = useMemo(() => value?.some((x) => x._dirty), [value]);
  const allowSorting = !canMove && !changed;
  const allowGrouping = !canMove;
  const allowRowReorder = canMove && !readonly;

  return (
    <>
      <Panel
        ref={panelRef}
        className={clsx(styles.container, {
          [styles.toolbar]: hasActions,
        })}
        header={
          <div className={styles.title}>
            <div className={styles.titleText}>
              <FieldLabel
                schema={schema}
                formAtom={formAtom}
                widgetAtom={widgetAtom}
              />
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
              onClick: editable && canEdit ? onAddInGrid : onAdd,
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
          minHeight: headerSize + 2 * rowSize, // min 2 rows
          maxHeight: maxHeight, // auto height to the max rows to display
        }}
      >
        <ScopeProvider scope={MetaScope} value={viewMeta}>
          <GridComponent
            className={styles["grid"]}
            ref={gridRef}
            allowGrouping={allowGrouping}
            allowSorting={allowSorting}
            allowRowReorder={allowRowReorder}
            showEditIcon={canEdit || canView}
            readonly={readonly || !canEdit}
            editable={editable && canEdit}
            records={records}
            view={(viewData?.view || schema) as GridView}
            fields={viewData?.fields || fields}
            perms={perms}
            columnAttrs={columnAttrs}
            state={state}
            setState={setState}
            actionExecutor={actionExecutor}
            onFormInit={forceUpdate}
            onEdit={canEdit ? onEdit : canView ? onView : noop}
            onView={canView ? (canEdit ? onEdit : onView) : noop}
            onUpdate={onSave}
            onSave={isManyToMany ? onSaveRecord : onSave}
            onRowReorder={onRowReorder}
            onRowSelectionChange={onRowSelectionChange}
            {...(!canNew &&
              editable && {
                onRecordAdd: undefined,
              })}
            {...(hasMasterDetails &&
              selected &&
              !detailRecord && {
                onRowClick,
              })}
          />
        </ScopeProvider>
        {hasMasterDetails && detailMeta ? (
          <Box d="flex" flexDirection="column" p={2}>
            {(!editable || selected) && (
              <ScopeProvider scope={MetaScope} value={detailMeta}>
                <DetailsForm
                  meta={detailMeta}
                  readonly={readonly || editable}
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
