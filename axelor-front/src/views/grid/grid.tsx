import { atom, useAtom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import isString from "lodash/isString";
import uniqueId from "lodash/uniqueId";
import {
  ReactElement,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { clsx, Box } from "@axelor/ui";
import { GridProps, GridRow, GridState } from "@axelor/ui/grid";

import { dialogs } from "@/components/dialogs";
import { PageText } from "@/components/page-text";
import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useViewPerms } from "@/hooks/use-perms";
import { useManyEditor } from "@/hooks/use-relation";
import { useDevice } from "@/hooks/use-responsive";
import { useSearchTranslate } from "@/hooks/use-search-translate";
import { useSession } from "@/hooks/use-session";
import { useShortcuts } from "@/hooks/use-shortcut";
import { openTab_internal as openTab } from "@/hooks/use-tabs";
import { request } from "@/services/client/client";
import {
  SaveOptions,
  SearchOptions,
  SearchResult,
} from "@/services/client/data";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { ViewData } from "@/services/client/meta";
import { findActionView, findView } from "@/services/client/meta-cache";
import {
  FormView,
  GridView,
  Schema,
  Widget,
} from "@/services/client/meta.types";
import { rejectAsAlert } from "@/services/client/reject";
import { commonClassNames } from "@/styles/common";
import { DEFAULT_PAGE_SIZE } from "@/utils/app-settings.ts";
import { focusAtom } from "@/utils/atoms";
import { toKebabCase } from "@/utils/names";
import { findViewItem } from "@/utils/schema";
import { AdvanceSearch } from "@/view-containers/advance-search";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { useSetPopupHandlers } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  useViewConfirmDirty,
  useViewContext,
  useViewDirtyAtom,
  useViewProps,
  useViewRoute,
  useViewSwitch,
  useViewTab,
  useViewTabRefresh,
} from "@/view-containers/views/scope";

import { Dms } from "../dms";
import { fetchRecord } from "../form";
import { useCreateFormAtomByMeta } from "../form/builder/atoms";
import { useActionExecutor, useAfterActions } from "../form/builder/scope";
import {
  createContextParams,
  nextId,
  processContextValues,
} from "../form/builder/utils";
import { HelpComponent } from "../form/widgets";
import { ViewProps } from "../types";
import { Grid as GridComponent, GridHandler } from "./builder";
import { useCustomizePopup } from "./builder/customize";
import { Details } from "./builder/details";
import { MassUpdater, useMassUpdateFields } from "./builder/mass-update";
import {
  CollectionTree,
  GridExpandableContext,
  useSetRootCollectionTreeColumnAttrs,
} from "./builder/scope";
import { AUTO_ADD_ROW, getSortBy, useGridState } from "./builder/utils";
import { SearchColumn } from "./renderers/search";
import { getSearchFilter } from "./renderers/search/utils";

import styles from "./grid.module.scss";

export function Grid(props: ViewProps<GridView>) {
  const { action } = useViewTab();
  if (action.params?.["ui-template:grid"] === "dms-file-list") {
    return <Dms {...props} />;
  }
  return <GridInner {...props} />;
}

function GridSizingWrapper({
  state,
  children,
}: {
  state: GridState;
  children: ReactElement;
}) {
  useSetRootCollectionTreeColumnAttrs(state, { padding: 8.125 });
  return children;
}

function GridWrapper({
  children,
  state,
  isTreeGrid,
}: {
  children: ReactElement;
  state: GridState;
  isTreeGrid?: boolean;
}) {
  const expandableContext = useMemo(
    () => ({
      level: 0,
      eventsAtom: atom({}),
      selectAtom: atom({}),
    }),
    [],
  );

  if (isTreeGrid) {
    return (
      <CollectionTree enabled>
        <GridExpandableContext.Provider value={expandableContext}>
          <GridSizingWrapper state={state}>{children}</GridSizingWrapper>
        </GridExpandableContext.Provider>
      </CollectionTree>
    );
  }
  return children;
}

function GridInner(props: ViewProps<GridView>) {
  const { meta, dataStore, searchAtom } = props;
  const { view, perms, fields } = meta;
  const { hasButton } = useViewPerms(meta);

  const viewRoute = useViewRoute();
  const pageSetRef = useRef(false);
  const gridRef = useRef<GridHandler>(null);
  const selectedIdsRef = useRef<number[]>([]);
  const saveIdRef = useRef<number | null>();
  const saveGridStateRef = useRef(0);
  const initDetailsRef = useRef(false);
  const reorderRef = useRef(false);
  const [massUpdatePopperEl, setMassUpdatePopperEl] =
    useState<HTMLElement | null>();
  const [viewProps, setViewProps] = useViewProps();

  const {
    action,
    dashlet,
    popup,
    popupOptions,
    state: tabStateAtom,
  } = useViewTab();
  const [dirty, setDirty] = useAtom(useViewDirtyAtom());
  const [detailsRecord, setDetailsRecord] = useState<DataRecord | null>(null);

  const showConfirmDirty = useViewConfirmDirty();
  const switchTo = useViewSwitch();
  const { isMobile } = useDevice();
  const { data: sessionData } = useSession();

  const viewContext = useMemo(
    () => processContextValues(action.context ?? {}),
    [action.context],
  );

  const gridSearchAtom = useMemo(
    () =>
      focusAtom(
        searchAtom!,
        (state) => state.search,
        (state, search) => ({ ...state, search }),
      ),
    [searchAtom],
  );
  const allFields = useAtomValue(
    useMemo(
      () => selectAtom(searchAtom!, (state) => state.fields),
      [searchAtom],
    ),
  );

  const viewSelectedRows = viewProps?.gridState?.selectedRows?.slice?.(0, 1);
  const [state, setState, gridStateAtom] = useGridState({
    ...viewProps?.gridState,
    view,
    params: action.params,
    selectedRows: viewSelectedRows,
  });

  const selector = action?.name?.startsWith("$selector");
  const hasRowSelectedFromState = useRef((viewSelectedRows?.length ?? 0) > 0);
  const [records, setRecords] = useState(dataStore.records);

  const processSearchResult = useAtomCallback(
    useCallback(
      (get, set, { records, page }: SearchResult) => {
        const { onGridSearch } = popupOptions ?? {};
        if (onGridSearch) {
          const { search } = (searchAtom && get(searchAtom)) ?? {};
          return onGridSearch(records, page, search);
        }
        return records;
      },
      [popupOptions, searchAtom],
    ),
  );

  useEffect(
    () =>
      dataStore.subscribe((ds) => {
        setRecords(processSearchResult(ds));
      }),
    [dataStore, processSearchResult],
  );

  const onColumnCustomize = useCustomizePopup({
    view,
    stateAtom: gridStateAtom,
    allowCustomization: Boolean(
      action.params?.["_can-customize-popup"] ?? true,
    ),
  });

  const { orderBy = null, rows, selectedRows, selectedCell } = state;
  const dashletParams = action.params?.["dashlet.params"];
  const detailsView = action.params?.["details-view"];
  const detailsViewOverlay = action.params?.["details-view-mode"] !== "inline";
  const hasDetailsView = Boolean(detailsView);
  const gridWidth = action.params?.["grid-width"];
  const hasPopup =
    action.params?.["popup"] || action.params?.["dashlet.in.popup"];
  const hasPopupMaximize = popupOptions?.fullScreen;
  const cacheDataRef = useRef(!action.params?.["reload-dotted"]);

  const { editable: _editable, onDelete: onDeleteAction, inlineHelp } = view;
  const canShowHelp = !sessionData?.user?.noHelp && inlineHelp;

  const showEditor = useManyEditor(action, dashlet);

  const clearSelection = useCallback(() => {
    setState((draft) => {
      draft.selectedRows = null;
      draft.selectedCell = null;
    });
  }, [setState]);

  const getViewContext = useViewContext();

  const getSearchTranslate = useSearchTranslate(orderBy, fields);

  const getSearchOptions = useAtomCallback(
    useCallback(
      (get, set, options: SearchOptions = {}) => {
        const { query = {}, search } = get(searchAtom!);

        const sortBy = getSortBy(orderBy);
        const searchQuery = getSearchFilter(fields as any, view.items, search);

        const filter: SearchOptions["filter"] = {
          ...query,
        };

        if (searchQuery?.criteria?.length) {
          filter.operator = "and";
          filter.criteria = [
            searchQuery,
            ...(query.criteria?.length
              ? [
                  {
                    operator: query.operator || "and",
                    criteria: query.criteria,
                  },
                ]
              : []),
          ];
        }

        if (dashlet) {
          const { _domainAction, ...formContext } = getViewContext() ?? {};
          filter._domainContext = {
            ...filter?._domainContext,
            ...formContext,
          };
          filter._domainAction = _domainAction;
        }

        const translate = getSearchTranslate(filter);

        return {
          translate,
          sortBy,
          ...options,
          filter,
        };
      },
      [
        orderBy,
        searchAtom,
        fields,
        view,
        dashlet,
        getViewContext,
        getSearchTranslate,
      ],
    ),
  );

  const getActionData = useCallback(
    () => ({
      ...dataStore.options?.filter,
      ...getSearchOptions().filter,
    }),
    [dataStore, getSearchOptions],
  );

  const doSearch = useCallback(
    (options: SearchOptions = {}) =>
      dataStore.search(getSearchOptions(options)),
    [dataStore, getSearchOptions],
  );

  const onSearch = useAfterActions(doSearch);
  const onSearchRef = useRef(onSearch);

  const { page } = dataStore;
  const { offset = 0, limit = DEFAULT_PAGE_SIZE, totalCount = 0 } = page;

  const onMassUpdate = useCallback(
    async (values: Partial<DataRecord>, hasAll?: boolean) => {
      const ids = hasAll
        ? []
        : (selectedRows || [])
            .map((ind) => rows[ind]?.record?.id)
            .filter((id) => id > 0);
      const count = hasAll ? totalCount : ids.length;
      const { model } = view;

      if (count === 0) {
        return dialogs.info({
          content: hasAll
            ? i18n.get("There are no records to update.")
            : i18n.get("Please select at least one record."),
        });
      }

      const confirmed = await dialogs.confirm({
        content: i18n.get(
          "Do you really want to update all {0} record(s)?",
          count,
        ),
      });
      if (confirmed) {
        setMassUpdatePopperEl(null);

        let _domain = action.domain;
        let _context = { _model: model, ...viewContext } as any;
        if (!hasAll) {
          if (_domain) {
            _domain = _domain + " AND self.id IN (:__ids__)";
          } else {
            _domain = "self.id IN (:__ids__)";
          }
          _context = { __ids__: ids, ..._context };
        }

        const { filter, ...data } = getSearchOptions();
        const resp = await request({
          url: `ws/rest/${model}/updateMass`,
          method: "POST",
          body: {
            data: {
              ...data,
              ...filter,
              _domain: _domain,
              _domainContext: _context,
            },
            records: [values],
          },
        });

        if (resp.ok) {
          const { status, data } = await resp.json();
          if (status !== 0) {
            return rejectAsAlert(data);
          }
        }

        onSearch();
      }
    },
    [
      selectedRows,
      totalCount,
      view,
      rows,
      action.domain,
      viewContext,
      getSearchOptions,
      onSearch,
    ],
  );

  const onEditInPopup = useCallback(
    (record: DataRecord, context: DataContext, readonly = false) => {
      showEditor({
        model: view.model!,
        title:
          (action.params?.["forceTitle"] ? action.title : view.title) ?? "",
        viewName: (action.views?.find((v) => v.type === "form") || {})?.name,
        maximize: hasPopupMaximize,
        context,
        record,
        readonly,
        onSearch: () => onSearch({}),
        ...(dashlet && {
          params: dashletParams,
        }),
      });
    },
    [
      view,
      action,
      hasPopupMaximize,
      dashlet,
      dashletParams,
      showEditor,
      onSearch,
    ],
  );

  const onEditInTab = useCallback(
    (record: DataRecord, context: DataContext, readonly = false) => {
      openTab({
        ...action,
        name: uniqueId("$act"),
        viewType: "form",
        params: {
          ...(dashlet && {
            ...dashletParams,
          }),
          forceEdit: !readonly,
        },
        context: {
          ...context,
          _showRecord: record.id,
        },
      });
    },
    [action, dashlet, dashletParams],
  );

  const canEdit = hasButton("edit");
  const editable = _editable && canEdit;
  const hasEditInMobile = isMobile && editable;

  const onEdit = useCallback(
    async (record: DataRecord, readonly = false) => {
      if (dashlet || hasEditInMobile) {
        let editorContext = getViewContext(true);
        if (dashlet) {
          const { _domainAction, ...formContext } = getViewContext() ?? {};
          const actionView = await findActionView(_domainAction, formContext, {
            silent: true,
          });
          const { context = {} } = actionView;
          editorContext = {
            ...editorContext,
            ...processContextValues(context),
            __check_version: context["__check_version"],
          };
        }

        const forceEdit = action.params?.["forceEdit"];
        if (hasPopup || hasEditInMobile || viewProps?.readonly === true) {
          return onEditInPopup(record, editorContext, readonly);
        }
        return onEditInTab(record, editorContext, forceEdit ? false : readonly);
      }
      const recordId = record.id || 0;
      const id = recordId > 0 ? String(recordId) : "";
      switchTo("form", {
        route: { id },
        props: { readonly },
      });
    },
    [
      dashlet,
      hasEditInMobile,
      switchTo,
      action.params,
      hasPopup,
      viewProps?.readonly,
      onEditInTab,
      getViewContext,
      onEditInPopup,
    ],
  );

  const onNew = useCallback(() => {
    onEdit({});
  }, [onEdit]);

  const onNewInGrid = useCallback((e?: any) => {
    // to prevent active edited row outside click
    e?.preventDefault?.();
    gridRef.current?.onAdd?.();
  }, []);

  const onView = useCallback(
    (record: DataRecord) => {
      onEdit(record, true);
    },
    [onEdit],
  );

  const onSave = useCallback(
    async (record: DataRecord, options?: SaveOptions<DataRecord>) => {
      const fields = Object.keys(meta.fields ?? {});
      const saved = await dataStore.save(
        {
          ...record,
          ...((record.id || -1) < 0 && { id: undefined }),
        },
        { ...options, ...(fields.length ? { fields } : {}) },
      );
      saved && setDirty(false);
      return saved;
    },
    [dataStore, meta, setDirty],
  );

  const onDiscard = useCallback(() => {
    setDirty(false);
  }, [setDirty]);

  const onRowReorder = useCallback(() => {
    reorderRef.current = true;
  }, []);

  const onArchiveOrUnArchive = useCallback(
    async (archived: boolean) => {
      const confirmed = await dialogs.confirm({
        title: i18n.get("Question"),
        content: archived
          ? i18n.get("Do you really want to archive the selected record(s)?")
          : i18n.get("Do you really want to unarchive the selected record(s)?"),
      });
      if (!confirmed) return;
      const records = selectedRows!
        .map((ind) => rows[ind]?.record)
        .map((record) => ({
          id: record.id,
          version: record.version,
          archived,
        }));
      try {
        await dataStore.save(records);
        onSearch();
        clearSelection();
      } catch {
        // Ignore
      }
    },
    [rows, selectedRows, dataStore, clearSelection, onSearch],
  );

  const formViewName = useMemo(
    () => (action.views?.find((v) => v.type === "form") || {})?.name,
    [action.views],
  );

  const { data: detailsMeta } = useAsync(async () => {
    if (!hasDetailsView) return null;
    const name = isString(detailsView) ? detailsView : formViewName;
    return await findView<FormView>({
      type: "form",
      name,
      model: view.model,
    });
  }, [view.model, formViewName]);

  const fetchAndSetDetailsRecord = useCallback(
    async (
      record: DataRecord | null,
      select?: Record<string, any>,
      restoreDummyValues?: (
        saved: DataRecord,
        fetched: DataRecord,
      ) => DataRecord,
    ) => {
      if (detailsMeta && record && (record?.id ?? 0) > 0) {
        const saved = record;
        record = await fetchRecord(detailsMeta, dataStore, record.id!, select);
        if (restoreDummyValues) {
          record = restoreDummyValues(saved, record);
        }
      }
      setDirty(false);
      setDetailsRecord(record);
    },
    [setDirty, detailsMeta, dataStore],
  );

  const onSaveInDetails = useCallback(
    async (
      record: DataRecord,
      options?: SaveOptions<DataRecord>,
      restoreDummyValues?: (
        saved: DataRecord,
        fetched: DataRecord,
      ) => DataRecord,
    ) => {
      const saved = await onSave(record, options);
      if (saved) {
        fetchAndSetDetailsRecord(saved, options?.select, restoreDummyValues);
        if ((record.id ?? 0) < 0) {
          saveIdRef.current = saved.id;
        }
      }
    },
    [onSave, fetchAndSetDetailsRecord],
  );

  const onNewInDetails = useCallback(
    ({ showConfirm = true } = {}) => {
      showConfirmDirty(
        async () => showConfirm && dirty,
        async () => {
          initDetailsRef.current = false;
          clearSelection();
          fetchAndSetDetailsRecord({ id: nextId() });
        },
      );
    },
    [dirty, clearSelection, fetchAndSetDetailsRecord, showConfirmDirty],
  );

  const onRefreshInDetails = useCallback(
    ({
      showConfirm = true,
      select,
    }: { showConfirm?: boolean; select?: Record<string, any> } = {}) => {
      showConfirmDirty(
        async () => showConfirm && dirty,
        async () => fetchAndSetDetailsRecord(detailsRecord, select),
      );
    },
    [fetchAndSetDetailsRecord, dirty, detailsRecord, showConfirmDirty],
  );

  const onCancelInDetails = useCallback(() => {
    showConfirmDirty(
      async () => dirty,
      async () => fetchAndSetDetailsRecord(null),
    );
  }, [dirty, fetchAndSetDetailsRecord, showConfirmDirty]);

  const selectedRow =
    (selectedRows?.length ?? 0) > 0 && selectedCell != null
      ? rows?.[selectedCell[0]]
      : null;
  const selectedDetail =
    selectedRow?.type !== "row" ? null : selectedRow?.record;
  const hasManySelected = (selectedRows?.length ?? 0) > 1;

  const onLoadDetails = useCallback(
    (e: any, row: GridRow) => {
      selectedDetail?.id === row?.record?.id &&
        fetchAndSetDetailsRecord(row.record);
    },
    [selectedDetail, fetchAndSetDetailsRecord],
  );

  const onShowDetails = useCallback(
    (e: any, row: GridRow) => {
      !e.ctrlKey &&
        row.record === selectedDetail &&
        fetchAndSetDetailsRecord(row.record);
    },
    [selectedDetail, fetchAndSetDetailsRecord],
  );

  useAsyncEffect(async () => {
    if (!detailsMeta) return;
    if (hasManySelected) return fetchAndSetDetailsRecord(null);
    const record = selectedDetail?.id ? selectedDetail : null;
    if (record && hasRowSelectedFromState.current) {
      hasRowSelectedFromState.current = false;
      return;
    }
    initDetailsRef.current && fetchAndSetDetailsRecord(record);
    initDetailsRef.current = true;
  }, [hasManySelected, selectedDetail?.id, detailsMeta, dataStore]);

  useAsyncEffect(async () => {
    if (
      action.context?._showSingle &&
      !viewProps?.showSingle &&
      records.length === 1
    ) {
      setViewProps((props) => ({ ...props, showSingle: true }));
      onEdit(records[0], true);
    }
  }, [records, onEdit, action]);

  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;
  const minPage = 1;
  const maxPage = Math.ceil(totalCount / limit);
  const hasRowSelected = !!selectedRows?.length;
  const currentPage = useMemo(() => {
    if (dashlet) return 0;
    const hasPageSet = pageSetRef.current;
    pageSetRef.current = true;
    if (hasPageSet || dataStore.records.length === 0) {
      return +(viewRoute?.id || 1);
    }
    return Math.floor(offset / limit) + 1;
  }, [dataStore, offset, limit, viewRoute?.id, dashlet]);

  const updatePage = useCallback(
    (page: number) => {
      if (dashlet) return;
      switchTo("grid", {
        route: { id: String(page) },
      });
    },
    [dashlet, switchTo],
  );

  const getContext = useCallback<() => DataContext>(
    () => ({
      ...createContextParams(view, action),
      ...getViewContext(true),
      ...(selectedIdsRef.current?.length > 0 && {
        _ids: selectedIdsRef.current,
      }),
    }),
    [action, view, getViewContext],
  );

  const formAtom = useCreateFormAtomByMeta(meta);

  const handleRowSave = useAtomCallback(
    useCallback(
      async (get, set) => {
        const { record, dirty } = get(formAtom);
        if (dirty) {
          const res = await dataStore.save(record);
          const rec = { ...record, ...res, _dirty: undefined };
          set(formAtom, (prev) => ({ ...prev, dirty: false, record: rec }));
          setState((draft) => {
            const selected = draft.rows.find((x) => x.record?.id === record.id);
            if (selected) {
              selected.record = rec;
            }
          });
        }
      },
      [dataStore, formAtom, setState],
    ),
  );

  const handleRowSelectionChange = useAtomCallback(
    useCallback(
      (get, set) => {
        set(formAtom, (state) => {
          const record = selectedRow?.record ?? {};
          const dirty = record._dirty ?? false;
          return {
            ...state,
            dirty,
            record,
          };
        });
      },
      [formAtom, selectedRow?.record],
    ),
  );

  useEffect(handleRowSelectionChange, [handleRowSelectionChange]);

  const actionExecutor = useActionExecutor(view, {
    formAtom,
    getContext,
    onRefresh: doSearch,
    onSave: handleRowSave,
  });

  const setPopupHandlers = useSetPopupHandlers();
  const setDashletHandlers = useSetAtom(useDashletHandlerAtom());

  useEffect(() => {
    if (popup) {
      setPopupHandlers({
        data: state,
        dataRecords: records,
        dataStore: dataStore,
        onSearch,
      });
    }
  }, [state, records, onSearch, popup, dataStore, setPopupHandlers]);

  useEffect(() => {
    if (popup) return;
    let nextPage = currentPage;
    if (offset > totalCount) {
      nextPage = Math.ceil(totalCount / limit) || nextPage;
    }
    updatePage(nextPage);
  }, [popup, currentPage, limit, offset, updatePage, totalCount]);

  const saveGridStateToViewProps = useAtomCallback(
    useCallback(
      (get, set, gridState: GridState) => {
        const tab = get(tabStateAtom);
        set(tabStateAtom, {
          ...tab,
          props: {
            ...tab.props,
            grid: {
              ...tab.props?.grid,
              gridState,
            },
          },
        });
      },
      [tabStateAtom],
    ),
  );

  useEffect(() => {
    // timeout is used in order to save grid state only on unmount
    clearTimeout(saveGridStateRef.current);
    return () => {
      saveGridStateRef.current = window.setTimeout(() => {
        saveGridStateToViewProps(state);
      }, 100);
    };
  }, [state, saveGridStateToViewProps]);

  useEffect(() => {
    setViewProps((viewProps) => {
      const selectedId = selectedRows
        ? rows[selectedRows?.[0]]?.record.id
        : undefined;
      if (viewProps?.selectedId !== selectedId) {
        return {
          selectedId,
        };
      }
      return viewProps;
    });
  }, [setViewProps, selectedRows, rows]);

  useEffect(() => {
    selectedIdsRef.current = (state.selectedRows || []).map(
      (ind) => state.rows[ind]?.record?.id,
    );
    const savedId = saveIdRef.current;
    if (savedId) {
      const savedInd = state.rows?.findIndex((r) => r.record?.id === savedId);
      savedInd > 0 &&
        setState((draft) => {
          draft.selectedCell = [savedInd, 0];
          draft.selectedRows = [savedInd];
        });
    }
    saveIdRef.current = null;
  }, [state.selectedRows, state.rows, setState]);

  useAsyncEffect(async () => {
    const orderField = orderBy?.[0]?.name;
    if (reorderRef.current && orderField) {
      const recIds = records.map((r) => r.id);
      const updateRecords = rows
        .filter((r) => recIds.includes(r.record?.id ?? 0))
        .map((r) => records.find((v) => v.id === r.record?.id))
        .map(
          (r, ind) =>
            ({
              id: r?.id,
              [orderField]: ind + 1,
              version: r?.version ?? r?.$version,
            }) as DataRecord,
        )
        .filter(
          (r) =>
            !orderField ||
            r[orderField] !== records.find((v) => v.id === r.id)?.[orderField],
        );
      const res = await dataStore.save(updateRecords);
      res && onSearch();
    }
    reorderRef.current = false;
  }, [view, rows, records, dataStore, orderBy]);

  const searchOptions = useMemo(() => {
    const options: Partial<SearchOptions> = {
      sortBy: getSortBy(orderBy),
    };
    if (currentPage) {
      return { ...options, offset: (currentPage - 1) * limit };
    }
    return options;
  }, [orderBy, currentPage, limit]);

  const onGridSearch = useCallback(
    (options?: SearchOptions) => {
      if (cacheDataRef.current) {
        cacheDataRef.current = false;
        if (isEqual(dataStore.options?.fields, options?.fields)) {
          return Promise.resolve({
            records: dataStore.records,
            page: dataStore.page,
          } as SearchResult);
        }
      }

      const hasOnSearchChanged = onSearchRef.current !== onSearch;

      const currOptions = dataStore.options || {};
      // if any search options changed then only trigger search
      if (
        !hasOnSearchChanged &&
        options &&
        Object.entries(options).every(([k, v]) =>
          isEqual(currOptions[k as keyof SearchOptions], v),
        )
      ) {
        return (cacheDataRef.current = false);
      }
      return (onSearchRef.current = onSearch)(options);
    },
    [onSearch, dataStore],
  );

  const onGridColumnSearch = useCallback(
    (_options?: SearchOptions) =>
      doSearch({ offset: 0, ..._options }).then(() => {
        if (dataStore.page?.offset === 0) {
          updatePage(1);
        }
      }),
    [doSearch, dataStore, updatePage],
  );

  const searchColumnRenderer = useMemo(() => {
    return (props: any) => (
      <SearchColumn
        {...props}
        dataAtom={gridSearchAtom}
        onSearch={onGridColumnSearch}
      />
    );
  }, [gridSearchAtom, onGridColumnSearch]);

  const showToolbar = popupOptions?.showToolbar !== false;
  const showEditIcon = popupOptions?.showEditIcon !== false && canEdit;
  const showCheckbox = popupOptions?.multiSelect !== false;

  const searchProps: any = {
    allowSearch: true,
    searchRowRenderer: Box,
    searchColumnRenderer: searchColumnRenderer,
  };
  const popupProps: any =
    !dashlet && popup
      ? {
          showEditIcon,
          allowSelection: true,
          selectionType: showCheckbox ? "multiple" : "single",
          allowCheckboxSelection: true,
        }
      : {};
  const dashletProps: any = dashlet
    ? {
        ...(action.params?.["dashlet.canSearch"] === true ? searchProps : {}),
        readonly: viewProps?.readonly,
        ...(hasPopup &&
          viewProps?.readonly === false && {
            onView: onEdit,
          }),
      }
    : {};

  const detailsProps: Partial<GridProps> = hasDetailsView
    ? {
        ...(detailsViewOverlay && { onView: undefined }),
        ...(!detailsRecord &&
          !hasManySelected && {
            onRowClick: detailsViewOverlay
              ? onShowDetails
              : selectedDetail
                ? onLoadDetails
                : undefined,
          }),
      }
    : {};

  const readonly = dashletProps.readonly;

  const canNew = hasButton("new");
  const handleNew = useMemo(() => {
    return (e?: any) => {
      if (hasDetailsView) {
        return onNewInDetails();
      }
      if (editable) {
        return onNewInGrid(e);
      }
      return onNew();
    };
  }, [hasDetailsView, editable, onNewInDetails, onNewInGrid, onNew]);

  const editEnabled = hasRowSelected && (!hasDetailsView || !dirty);
  const handleEdit = useCallback(() => {
    const [rowIndex] = selectedRows ?? [];
    const record = rows[rowIndex]?.record;
    record && onEdit(record);
  }, [selectedRows, rows, onEdit]);

  const canDelete = hasButton("delete");
  const deleteEnabled = editEnabled;

  const onDelete = useCallback(
    async (records: GridRow["record"][]) => {
      const confirmed = await dialogs.confirm({
        content: i18n.get(
          "Do you really want to delete the selected record(s)?",
        ),
        yesTitle: i18n.get("Delete"),
      });
      if (confirmed) {
        if (onDeleteAction) {
          await actionExecutor.execute(onDeleteAction, {
            context: {
              _ids: records.map((r) => r.id),
            },
          });
        }
        try {
          await dataStore.delete(
            records.map(({ id, version }) => ({ id, version })),
          );
          clearSelection();
        } catch {
          // Ignore
        }
      }
    },
    [onDeleteAction, actionExecutor, dataStore, clearSelection],
  );

  const handleDelete = useCallback(() => {
    void (async () => {
      await onDelete(selectedRows!.map((ind) => rows[ind]?.record));
    })();
  }, [onDelete, selectedRows, rows]);

  const handlePrev = useCallback(
    () => updatePage(Math.max(minPage, currentPage - 1)),
    [currentPage, minPage, updatePage],
  );

  const handleNext = useCallback(
    () => updatePage(Math.min(maxPage, currentPage + 1)),
    [currentPage, maxPage, updatePage],
  );

  useShortcuts({
    viewType: view.type,
    canHandle: useCallback(() => state.editRow == null, [state.editRow]),
    onNew: canNew ? handleNew : undefined,
    onEdit: canEdit && editEnabled ? handleEdit : undefined,
    onDelete: canDelete && deleteEnabled ? handleDelete : undefined,
    onRefresh: onSearch,
    onFocus: useCallback(() => {
      setState?.((draft) => {
        const row = draft.selectedRows?.[0];
        if (row != null) {
          const col = draft.selectedCell?.[1] ?? 0;
          draft.selectedCell = [row, col];
        } else {
          draft.selectedCell = [0, 0];
          draft.selectedRows = [0];
        }
      });
    }, [setState]),
  });

  useEffect(() => {
    if (dashlet) {
      setDashletHandlers({
        dataStore,
        view,
        actionExecutor,
        gridStateAtom,
        getContext,
        ...(canNew &&
          !readonly && {
            onAdd: handleNew,
          }),
        ...(canDelete &&
          !readonly &&
          deleteEnabled && {
            onDelete: handleDelete,
          }),
        onRefresh: doSearch,
      });
    }
  }, [
    readonly,
    canNew,
    canDelete,
    deleteEnabled,
    dashlet,
    view,
    gridStateAtom,
    dataStore,
    actionExecutor,
    getContext,
    doSearch,
    handleNew,
    handleDelete,
    setDashletHandlers,
  ]);

  // register tab:refresh
  useViewTabRefresh("grid", onSearch);

  const massUpdateFields = useMassUpdateFields(allFields, view.items);
  const canMassUpdate = perms?.massUpdate != false && hasButton("edit") && massUpdateFields.length > 0;

  const { treeLimit, treeField, treeFieldTitle } = view;
  const widget = toKebabCase(view.widget ?? "");
  const isExpandable = widget === "expandable";
  const isTreeGrid = treeField && widget === "tree-grid";

  const { data: expandableSummaryMeta } = useAsync(async () => {
    const { summaryView, model } = view;
    if (!isTreeGrid || !summaryView) return;
    return await findView<FormView>({
      type: "form",
      name: summaryView,
      model,
    });
  }, [isTreeGrid, view]);

  const expandableView = useMemo(() => {
    const { model } = view;
    const treeFieldMeta = meta.fields?.[treeField!];
    if (isTreeGrid) {
      const summaryFields = expandableSummaryMeta?.fields ?? {};
      // only considered defined fields of view for tree grid
      const subFields = Object.keys(summaryFields).reduce(
        (_fields, fieldName) =>
          expandableSummaryMeta &&
          findViewItem(expandableSummaryMeta, fieldName)
            ? { ..._fields, [fieldName]: summaryFields[fieldName] }
            : _fields,
        {},
      );
      const field = {
        type: "ONE_TO_MANY", // default type to ONE_TO_MANY
        ...treeFieldMeta,
        target: model,
        name: treeField,
        title:
          (treeFieldTitle && i18n.get(treeFieldTitle)) ||
          i18n.get("Add subitem"),
      };
      return {
        model,
        fields: {
          ...subFields,
          [treeField]: field,
        },
        view: {
          type: "form",
          model: model,
          items: [
            {
              ...field,
              editable,
              colSpan: 12,
              canNew,
              canEdit,
              canRemove: canDelete,
              canView: true,
              editIcon: true,
              onDelete: view.onDelete,
              onNew: view.onNew,
              onSave: view.onSave,
              summaryView: view.summaryView,
              fields: meta.fields,
              items: view.items,
              uid: uniqueId("w"),
              type: "panel-related",
              formView: formViewName,
              widget,
              widgetAttrs: {
                treeField,
                treeFieldTitle,
                ...(treeLimit && {
                  treeLimit: treeLimit - 1, // as grid itself occupy 1 level
                }),
              },
              serverType: field.type,
              [AUTO_ADD_ROW]: false,
            },
            ...(expandableSummaryMeta?.view?.items ?? [])
              .filter(
                (item) => item.name !== "$wkfStatus", // skip wkf status
              )
              .map((item) => {
                if ((item as Schema).serverType?.endsWith("_TO_MANY")) {
                  return {
                    ...item,
                    [AUTO_ADD_ROW]: false,
                  };
                }
                return item;
              }),
          ],
          width: "*",
        } as any,
      } as ViewData<FormView>;
    }
    return view.summaryView ?? formViewName;
  }, [
    expandableSummaryMeta,
    view,
    editable,
    formViewName,
    meta,
    widget,
    canNew,
    canEdit,
    canDelete,
    isTreeGrid,
    treeLimit,
    treeField,
    treeFieldTitle,
  ]);

  const gridViewStyles =
    detailsMeta && !detailsViewOverlay && gridWidth
      ? { minWidth: gridWidth, maxWidth: gridWidth }
      : undefined;

  const gridContext = useMemo(() => ({ type: "grid", newIcon: false }), []);

  return (
    <div className={styles.container}>
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          formAtom={formAtom}
          actions={[
            {
              key: "new",
              text: i18n.get("New"),
              hidden: !canNew,
              iconProps: {
                icon: "add",
              },
              onClick: handleNew,
            },
            {
              key: "edit",
              text: i18n.get("Edit"),
              hidden: !canEdit,
              iconProps: {
                icon: "edit",
              },
              disabled: !editEnabled,
              className: commonClassNames("hide-sm"),
              onClick: handleEdit,
            },
            {
              key: "massUpdate",
              iconOnly: true,
              iconProps: {
                icon: "arrow_drop_down",
              },
              className: clsx(styles["mass-update"], {
                [styles.active]: Boolean(massUpdatePopperEl),
              }),
              onClick: (e) => setMassUpdatePopperEl(e.target as HTMLElement),
              hidden: !canMassUpdate,
            },
            {
              key: "delete",
              text: i18n.get("Delete"),
              hidden: !canDelete,
              iconProps: {
                icon: "delete",
              },
              disabled: !deleteEnabled,
              onClick: handleDelete,
              items: hasButton("archive")
                ? [
                    {
                      key: "archive",
                      text: i18n.get("Archive"),
                      onClick: () => onArchiveOrUnArchive(true),
                    },
                    {
                      key: "unarchive",
                      text: i18n.get("Unarchive"),
                      onClick: () => onArchiveOrUnArchive(false),
                    },
                  ]
                : undefined,
            },
            {
              key: "refresh",
              text: i18n.get("Refresh"),
              iconProps: {
                icon: "refresh",
              },
              onClick: () => onSearch(),
            },
          ]}
          dataStore={dataStore}
          getActionData={getActionData}
          actionExecutor={actionExecutor}
          pagination={{
            canPrev,
            canNext,
            onPrev: handlePrev,
            onNext: handleNext,
            text: () => <PageText dataStore={dataStore} />,
          }}
        >
          {searchAtom && (
            <AdvanceSearch
              stateAtom={searchAtom}
              dataStore={dataStore}
              items={state.columns as Widget[]}
              customSearch={view.customSearch}
              freeSearch={view.freeSearch}
              onSearch={onSearch}
            />
          )}
        </ViewToolBar>
      )}
      {canShowHelp && (
        <div className={styles.help}>
          <HelpComponent text={inlineHelp.text} css={inlineHelp.css} />
        </div>
      )}
      <div className={styles.views}>
        <div className={styles["grid-view"]} style={gridViewStyles}>
          <GridWrapper state={state} isTreeGrid={Boolean(isTreeGrid)}>
            <GridComponent
              className={styles.grid}
              ref={gridRef}
              records={records}
              view={view}
              viewContext={viewContext}
              fields={fields}
              perms={perms}
              state={state}
              setState={setState}
              sortType={"live"}
              editable={!selector && !readonly && editable}
              {...((isExpandable || isTreeGrid) && {
                gridContext,
                showAsTree: isTreeGrid,
                showNewIcon: canNew,
                showDeleteIcon: canDelete,
                expandable: true,
                expandableView,
                onNew: handleNew,
                onDelete,
              })}
              showEditIcon={canEdit}
              searchOptions={searchOptions}
              searchAtom={searchAtom}
              actionExecutor={actionExecutor}
              onEdit={readonly === true ? onView : onEdit}
              onView={onView}
              onSearch={onGridSearch}
              onSave={onSave}
              onDiscard={onDiscard}
              onRowReorder={onRowReorder}
              noRecordsText={i18n.get("No records found.")}
              onColumnCustomize={onColumnCustomize}
              {...(dashlet ? {} : searchProps)}
              {...dashletProps}
              {...popupProps}
              {...detailsProps}
              {...(!canNew && {
                onRecordAdd: undefined,
              })}
            />
          </GridWrapper>
          {hasDetailsView && dirty && (
            <Box bg="light" className={styles.overlay} />
          )}
        </div>
        {detailsMeta && (
          <Box
            bg="body"
            className={clsx(styles["details-view"], {
              [styles["details-view-overlay"]]: detailsViewOverlay,
              [styles.hide]: !detailsRecord,
            })}
            {...(detailsViewOverlay &&
              detailsRecord && {
                shadow: "2xl",
                dropShadow: "2xl",
              })}
          >
            {detailsRecord ? (
              <Details
                relatedViewType={view.type}
                dirty={dirty}
                overlay={detailsViewOverlay}
                meta={detailsMeta}
                record={detailsRecord}
                onNew={onNewInDetails}
                onRefresh={onRefreshInDetails}
                onSave={onSaveInDetails}
                onCancel={onCancelInDetails}
              />
            ) : (
              !detailsViewOverlay && (
                <Box
                  d="flex"
                  flex={1}
                  justifyContent="center"
                  alignItems="center"
                  border
                  borderBottom={false}
                >
                  <Box as="span">
                    {i18n.get("Either select or create a record.")}
                  </Box>
                </Box>
              )
            )}
          </Box>
        )}

        {canMassUpdate && (
          <MassUpdater
            open={Boolean(massUpdatePopperEl)}
            target={massUpdatePopperEl}
            fields={massUpdateFields}
            onUpdate={onMassUpdate}
            onClose={() => setMassUpdatePopperEl(null)}
          />
        )}
      </div>
    </div>
  );
}
