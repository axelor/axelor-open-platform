import { focusAtom } from "jotai-optics";
import { useAtomCallback } from "jotai/utils";
import uniqueId from "lodash/uniqueId";
import isString from "lodash/isString";

import { useAtom, useSetAtom } from "jotai";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Box } from "@axelor/ui";
import { GridRow } from "@axelor/ui/grid";

import { dialogs } from "@/components/dialogs";
import { PageText } from "@/components/page-text";
import { useDataStore } from "@/hooks/use-data-store";
import { usePerms } from "@/hooks/use-perms";
import { useEditor } from "@/hooks/use-relation";
import { openTab_internal as openTab } from "@/hooks/use-tabs";
import { SearchOptions } from "@/services/client/data";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { commonClassNames } from "@/styles/common";
import { AdvanceSearch } from "@/view-containers/advance-search";
import { FormView, GridView, Widget } from "@/services/client/meta.types";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  useViewDirtyAtom,
  useViewProps,
  useViewRoute,
  useViewSwitch,
  useViewTab,
} from "@/view-containers/views/scope";
import { SearchColumn } from "./renderers/search";
import { getSearchFilter } from "./renderers/search/utils";

import { Dms } from "../dms";
import { usePrepareContext } from "../form/builder";
import { useFormScope } from "../form/builder/scope";
import { ViewProps } from "../types";
import { Grid as GridComponent, GridHandler } from "./builder";
import { useGridActionExecutor, useGridState } from "./builder/utils";
import { useAsync } from "@/hooks/use-async";
import { findView } from "@/services/client/meta-cache";
import { Details } from "./builder/details";
import { nextId } from "../form/builder/utils";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { fetchRecord } from "../form";
import styles from "./grid.module.scss";

export function Grid(props: ViewProps<GridView>) {
  const { action } = useViewTab();
  if (action.params?.["ui-template:grid"] === "dms-file-list") {
    return <Dms {...props} />;
  }
  return <GridInner {...props} />;
}

function GridInner(props: ViewProps<GridView>) {
  const { meta, dataStore, searchAtom } = props;
  const { view, fields } = meta;
  const { hasButton } = usePerms(meta.view, meta.perms);

  const viewRoute = useViewRoute();
  const pageSetRef = useRef(false);
  const gridRef = useRef<GridHandler>(null);
  const selectedIdsRef = useRef<number[]>([]);
  const saveIdRef = useRef<number | null>();
  const initDetailsRef = useRef(false);
  const [viewProps, setViewProps] = useViewProps();
  const { action, dashlet, popup, popupOptions } = useViewTab();
  const [dirty, setDirty] = useAtom(useViewDirtyAtom());
  const [detailsRecord, setDetailsRecord] = useState<DataRecord | null>(null);

  const switchTo = useViewSwitch();
  const showEditor = useEditor();

  const gridSearchAtom = useMemo(
    () => focusAtom(searchAtom!, (o) => o.prop("search")),
    [searchAtom]
  );

  const [state, setState] = useGridState({
    view,
    selectedCell: viewProps?.selectedCell,
    selectedRows: viewProps?.selectedRows?.slice?.(0, 1),
  });
  const records = useDataStore(dataStore, (ds) => ds.records);
  const { orderBy, rows, selectedRows, selectedCell } = state;
  const detailsView = action.params?.["details-view"];
  const hasDetailsView = Boolean(detailsView);

  const clearSelection = useCallback(() => {
    setState((draft) => {
      draft.selectedRows = null;
      draft.selectedCell = null;
    });
  }, [setState]);

  const { formAtom } = useFormScope();
  const getFormContext = usePrepareContext(formAtom);

  const onSearch = useAtomCallback(
    useCallback(
      (get, set, options: SearchOptions = {}) => {
        const sortBy = orderBy?.map(
          (column) => `${column.order === "desc" ? "-" : ""}${column.name}`
        );
        const {
          searchText: freeSearchText,
          query = {},
          search,
        } = get(searchAtom!);

        const searchQuery = getSearchFilter(fields as any, view.items, search);

        let q: SearchOptions["filter"] = {
          ...query,
          operator: searchQuery?.operator ?? query.operator,
        };

        if (freeSearchText && searchQuery?.criteria?.length) {
          q.operator = "and";
          q.criteria = [
            { operator: "and", ...searchQuery },
            {
              operator: query.operator || "or",
              criteria: query.criteria,
            },
          ];
        } else {
          q.criteria = [...(query.criteria || [])].concat(
            searchQuery?.criteria || []
          );
          freeSearchText && (q.operator = "or");
        }

        setState((draft) => {
          draft.selectedCell = null;
        });

        if (dashlet) {
          const { _domainAction, ...formContext } = getFormContext() ?? {};
          const { _domainContext } = q;
          q._domainContext = {
            ..._domainContext,
            ...formContext,
          };
          q._domainAction = _domainAction;
        }

        return dataStore.search({
          sortBy,
          ...options,
          filter: { ...q },
        });
      },
      [
        orderBy,
        searchAtom,
        fields,
        view.items,
        setState,
        dashlet,
        dataStore,
        getFormContext,
      ]
    )
  );

  const onDelete = useCallback(
    async (records: GridRow["record"][]) => {
      const confirmed = await dialogs.confirm({
        content: i18n.get(
          "Do you really want to delete the selected record(s)?"
        ),
        yesTitle: i18n.get("Delete"),
      });
      if (confirmed) {
        try {
          await dataStore.delete(
            records.map(({ id, version }) => ({ id, version }))
          );
          clearSelection();
        } catch {}
      }
    },
    [dataStore, clearSelection]
  );

  const onViewInDashlet = useCallback(
    (record: DataRecord, readonly = false) => {
      showEditor({
        model: view.model!,
        title: view.title ?? "",
        record,
        readonly,
      });
    },
    [view, showEditor]
  );

  const onEditInDashlet = useCallback(
    (record: DataRecord, forceReadonly = false) => {
      openTab({
        ...action,
        name: uniqueId("$act"),
        viewType: "form",
        context: {
          forceReadonly,
          _showRecord: record.id,
        },
      });
    },
    [action]
  );

  const onEdit = useCallback(
    (record: DataRecord, readonly = false) => {
      if (dashlet) {
        return readonly
          ? onViewInDashlet(record, readonly)
          : onEditInDashlet(record, readonly);
      }
      const recordId = record.id || 0;
      const id = recordId > 0 ? String(recordId) : "";
      switchTo("form", {
        route: { id },
        props: { readonly },
      });
    },
    [dashlet, switchTo, onEditInDashlet, onViewInDashlet]
  );

  const onNew = useCallback(() => {
    onEdit({});
  }, [onEdit]);

  const onNewInGrid = useCallback(() => {
    gridRef.current?.onAdd?.();
  }, []);

  const onView = useCallback(
    (record: DataRecord) => {
      onEdit(record, true);
    },
    [onEdit]
  );

  const onSave = useCallback(
    async (record: DataRecord) => {
      const saved = await dataStore.save({
        ...record,
        ...((record.id || -1) < 0 && { id: undefined }),
      });
      saved && setDirty(false);
      return saved;
    },
    [dataStore, setDirty]
  );

  const onDiscard = useCallback(() => {
    setDirty(false);
  }, [setDirty]);

  const onArchiveOrUnArchive = useCallback(
    async (archived: boolean) => {
      const confirmed = await dialogs.confirm({
        title: i18n.get("Question"),
        content: archived
          ? i18n.get(`Do you really want to archive the selected record(s)?`)
          : i18n.get(`Do you really want to unarchive the selected record(s)?`),
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
      } catch {}
    },
    [rows, selectedRows, dataStore, clearSelection, onSearch]
  );

  const { data: formMeta } = useAsync(async () => {
    if (!hasDetailsView) return null;
    const name = isString(detailsView)
      ? detailsView
      : (action.views?.find((v) => v.type === "form") || {})?.name;
    return await findView<FormView>({
      type: "form",
      name,
      model: view.model,
    });
  }, [view.model]);

  const fetchAndSetDetailsRecord = useCallback(
    async (record: DataRecord | null) => {
      if (formMeta && record && (record?.id ?? 0) > 0) {
        record = await fetchRecord(formMeta, dataStore, record.id!);
      }
      setDirty(false);
      setDetailsRecord(record);
    },
    [setDirty, formMeta, dataStore]
  );

  const onSaveInDetails = useCallback(
    async (record: DataRecord) => {
      const saved = await onSave(record);
      if (saved) {
        fetchAndSetDetailsRecord(saved);
        if ((record.id ?? 0) < 0) {
          saveIdRef.current = saved.id;
        }
      }
    },
    [onSave, fetchAndSetDetailsRecord]
  );

  const onNewInDetails = useCallback(() => {
    dialogs.confirmDirty(
      async () => dirty,
      async () => {
        initDetailsRef.current = false;
        clearSelection();
        fetchAndSetDetailsRecord({ id: nextId() });
      }
    );
  }, [dirty, clearSelection, fetchAndSetDetailsRecord]);

  const onRefreshInDetails = useCallback(() => {
    dialogs.confirmDirty(
      async () => dirty,
      async () => fetchAndSetDetailsRecord(detailsRecord)
    );
  }, [fetchAndSetDetailsRecord, dirty, detailsRecord]);

  const onCancelInDetails = useCallback(() => {
    dialogs.confirmDirty(
      async () => dirty,
      async () => fetchAndSetDetailsRecord(null)
    );
  }, [dirty, fetchAndSetDetailsRecord]);

  useAsyncEffect(async () => {
    if (!formMeta) return;
    const [ind = -1] = selectedRows || [];
    const selected = rows?.[ind]?.record;
    const record = selected?.id ? selected : null;
    initDetailsRef.current && fetchAndSetDetailsRecord(record);
    initDetailsRef.current = true;
  }, [selectedRows, formMeta, dataStore]);

  const { page } = dataStore;
  const { offset = 0, limit = 40, totalCount = 0 } = page;

  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;
  const hasRowSelected = !!selectedRows?.length;
  const currentPage = useMemo(() => {
    const hasPageSet = pageSetRef.current;
    pageSetRef.current = true;
    if (hasPageSet || dataStore.records.length === 0) {
      return +(viewRoute?.id || 1);
    }
    return Math.floor(offset / limit) + 1;
  }, [dataStore, offset, limit, viewRoute?.id]);

  const getContext = useCallback<() => DataContext>(
    () => ({
      ...action.context,
      _ids: selectedIdsRef.current,
      _model: action.model,
      _viewName: action.name,
      _viewType: action.viewType,
      _views: action.views,
    }),
    [action]
  );
  const actionExecutor = useGridActionExecutor(view, {
    getContext,
    onRefresh: onSearch,
  });

  const popupHandlerAtom = usePopupHandlerAtom();
  const setPopupHandlers = useSetAtom(popupHandlerAtom);
  const setDashletHandlers = useSetAtom(useDashletHandlerAtom());

  useEffect(() => {
    if (popup) {
      setPopupHandlers({
        data: state,
        dataStore: dataStore,
        onSearch,
      });
    }
  }, [state, onSearch, popup, dataStore, setPopupHandlers]);

  useEffect(() => {
    if (dashlet) {
      setDashletHandlers({
        dataStore,
        view,
        actionExecutor,
        onRefresh: () => onSearch({}),
      });
    }
  }, [dashlet, view, dataStore, actionExecutor, onSearch, setDashletHandlers]);

  useEffect(() => {
    if (dashlet || popup) return;
    let nextPage = currentPage;
    if (offset > totalCount) {
      nextPage = Math.ceil(totalCount / limit) || nextPage;
    }
    switchTo("grid", {
      route: { id: String(nextPage) },
    });
  }, [dashlet, popup, currentPage, limit, offset, switchTo, totalCount]);

  useEffect(() => {
    setViewProps((viewProps) => {
      if (
        viewProps?.selectedCell !== selectedCell ||
        viewProps?.selectedRows !== selectedRows
      ) {
        const selectedId = selectedRows
          ? rows[selectedRows?.[0]]?.record.id
          : undefined;
        return {
          selectedId,
          selectedCell: selectedCell,
          selectedRows: selectedRows,
        };
      }
      return viewProps;
    });
  }, [setViewProps, selectedCell, selectedRows, rows]);

  useEffect(() => {
    selectedIdsRef.current = (state.selectedRows || []).map(
      (ind) => state.rows[ind]?.record?.id
    );
    const savedId = saveIdRef.current;
    if (savedId) {
      const savedInd = state.rows?.findIndex((r) => r.record?.id === savedId);
      savedInd > 0 &&
        setState((draft) => {
          draft.selectedRows = [savedInd];
        });
    }
    saveIdRef.current = null;
  }, [state.selectedRows, state.rows, setState]);

  const searchOptions = useMemo(() => {
    if (currentPage) {
      return { offset: (currentPage - 1) * limit };
    }
  }, [currentPage, limit]);

  const searchColumnRenderer = useMemo(() => {
    return (props: any) => (
      <SearchColumn {...props} dataAtom={gridSearchAtom} onSearch={onSearch} />
    );
  }, [gridSearchAtom, onSearch]);

  const showToolbar = popupOptions?.showToolbar !== false;
  const showEditIcon = popupOptions?.showEditIcon !== false;
  const showCheckbox = popupOptions?.multiSelect !== false;
  const { editable } = view;

  const popupProps: any = popup
    ? {
        showEditIcon,
        allowSelection: true,
        selectionType: showCheckbox ? "multiple" : "single",
        allowCheckboxSelection: showCheckbox,
      }
    : {};
  const dashletProps: any = dashlet
    ? {
        readonly: viewProps?.readonly,
      }
    : {
        allowSearch: true,
        searchRowRenderer: Box,
        searchColumnRenderer: searchColumnRenderer,
      };

  const readonly = dashletProps.readonly;

  return (
    <div className={styles.container}>
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          actions={[
            {
              key: "new",
              text: i18n.get("New"),
              hidden: !hasButton("new"),
              iconProps: {
                icon: "add",
              },
              onClick: hasDetailsView
                ? onNewInDetails
                : editable
                ? onNewInGrid
                : onNew,
            },
            {
              key: "edit",
              text: i18n.get("Edit"),
              hidden: !hasButton("edit"),
              iconProps: {
                icon: "edit",
              },
              disabled: !hasRowSelected || (hasDetailsView && dirty),
              className: commonClassNames("hide-sm"),
              onClick: () => {
                const [rowIndex] = selectedRows || [];
                const record = rows[rowIndex]?.record;
                record && onEdit(record);
              },
            },
            {
              key: "delete",
              text: i18n.get("Delete"),
              hidden: !hasButton("delete"),
              iconProps: {
                icon: "delete",
              },
              disabled: !hasRowSelected || (hasDetailsView && dirty),
              onClick: () => {
                onDelete(selectedRows!.map((ind) => rows[ind]?.record));
              },
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
          actionExecutor={actionExecutor}
          pagination={{
            canPrev,
            canNext,
            onPrev: () =>
              switchTo("grid", {
                route: { id: String(currentPage - 1) },
              }),
            onNext: () =>
              switchTo("grid", {
                route: { id: String(currentPage + 1) },
              }),
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
      <div className={styles.views}>
        <div className={styles["grid-view"]}>
          <GridComponent
            className={styles.grid}
            ref={gridRef}
            records={records}
            view={view}
            fields={fields}
            state={state}
            setState={setState}
            sortType={"live"}
            editable={editable}
            searchOptions={searchOptions}
            actionExecutor={actionExecutor}
            onEdit={readonly ? onView : onEdit}
            onView={onView}
            onSearch={onSearch}
            onSave={onSave}
            onDiscard={onDiscard}
            noRecordsText={i18n.get("No records found.")}
            {...dashletProps}
            {...popupProps}
          />
          {hasDetailsView && dirty && (
            <Box bg="light" className={styles.overlay} />
          )}
        </div>
        {hasDetailsView && formMeta && detailsRecord && (
          <Box bg="light" className={styles["details-view"]}>
            <Details
              dirty={dirty}
              meta={formMeta}
              record={detailsRecord}
              onNew={onNewInDetails}
              onRefresh={onRefreshInDetails}
              onSave={onSaveInDetails}
              onCancel={onCancelInDetails}
            />
          </Box>
        )}
      </div>
    </div>
  );
}
