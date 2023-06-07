import clsx from "clsx";
import { focusAtom } from "jotai-optics";
import { useAtomCallback } from "jotai/utils";
import isString from "lodash/isString";
import uniqueId from "lodash/uniqueId";

import { Box } from "@axelor/ui";
import { GridProps, GridRow } from "@axelor/ui/grid";
import { useAtom, useSetAtom } from "jotai";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { dialogs } from "@/components/dialogs";
import { PageText } from "@/components/page-text";
import { useDataStore } from "@/hooks/use-data-store";
import { usePerms } from "@/hooks/use-perms";
import { useEditor } from "@/hooks/use-relation";
import { openTab_internal as openTab } from "@/hooks/use-tabs";
import { SearchOptions } from "@/services/client/data";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { FormView, GridView, Widget } from "@/services/client/meta.types";
import { commonClassNames } from "@/styles/common";
import { AdvanceSearch } from "@/view-containers/advance-search";
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

import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useShortcuts } from "@/hooks/use-shortcut";
import { findView } from "@/services/client/meta-cache";
import { Dms } from "../dms";
import { fetchRecord } from "../form";
import { usePrepareContext } from "../form/builder";
import { useFormScope } from "../form/builder/scope";
import { nextId } from "../form/builder/utils";
import { ViewProps } from "../types";
import { Grid as GridComponent, GridHandler } from "./builder";
import { Details } from "./builder/details";
import { useGridActionExecutor, useGridState } from "./builder/utils";
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
  const detailsViewOverlay =
    (action.params?.["details-view-mode"] || "default") === "overlay";
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
        const { query = {}, search } = get(searchAtom!);

        const searchQuery = getSearchFilter(fields as any, view.items, search);

        let filter: SearchOptions["filter"] = {
          ...query,
        };

        if (searchQuery?.criteria?.length) {
          filter.operator = "and";
          filter.criteria = [
            searchQuery,
            {
              operator: query.operator || "and",
              criteria: query.criteria,
            },
          ];
        }

        setDetailsRecord(null);
        setState((draft) => {
          draft.selectedCell = null;
        });

        if (dashlet) {
          const { _domainAction, ...formContext } = getFormContext() ?? {};
          filter._domainContext = {
            ...filter?._domainContext,
            ...formContext,
          };
          filter._domainAction = _domainAction;
        }

        return dataStore.search({
          sortBy,
          ...options,
          filter,
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

  const { data: detailsMeta } = useAsync(async () => {
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
      if (detailsMeta && record && (record?.id ?? 0) > 0) {
        record = await fetchRecord(detailsMeta, dataStore, record.id!);
      }
      setDirty(false);
      setDetailsRecord(record);
    },
    [setDirty, detailsMeta, dataStore]
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

  const selectedRow =
    (selectedRows?.length ?? 0) > 0 ? rows?.[selectedCell?.[0]!] : null;
  const selectedDetail =
    selectedRow?.type !== "row" ? null : selectedRow?.record;

  const onLoadDetails = useCallback(
    (e: any, row: GridRow) => {
      selectedDetail?.id === row?.record?.id &&
        fetchAndSetDetailsRecord(row.record);
    },
    [selectedDetail, fetchAndSetDetailsRecord]
  );

  const onShowDetails = useCallback(
    (e: any, row: GridRow) => {
      !e.ctrlKey &&
        row.record === selectedDetail &&
        fetchAndSetDetailsRecord(row.record);
    },
    [selectedDetail, fetchAndSetDetailsRecord]
  );

  useAsyncEffect(async () => {
    if (!detailsMeta) return;
    const record = selectedDetail?.id ? selectedDetail : null;
    initDetailsRef.current && fetchAndSetDetailsRecord(record);
    initDetailsRef.current = true;
  }, [selectedDetail?.id, detailsMeta, dataStore]);

  const { page } = dataStore;
  const { offset = 0, limit = 40, totalCount = 0 } = page;

  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;
  const minPage = 1;
  const maxPage = Math.ceil(totalCount / limit);
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
          draft.selectedCell = [savedInd, 0];
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
  const detailsProps: Partial<GridProps> = hasDetailsView
    ? {
        ...(detailsViewOverlay && { onView: undefined }),
        ...(!detailsRecord && {
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
    if (hasDetailsView) {
      return onNewInDetails;
    }
    if (editable) {
      return onNewInGrid;
    }
    return onNew;
  }, [hasDetailsView, editable, onNewInDetails, onNewInGrid, onNew]);

  const canEdit = hasButton("edit");
  const editEnabled = hasRowSelected && (!hasDetailsView || !dirty);
  const handleEdit = useCallback(() => {
    const [rowIndex] = selectedRows ?? [];
    const record = rows[rowIndex]?.record;
    record && onEdit(record);
  }, [selectedRows, rows, onEdit]);

  const canDelete = hasButton("delete");
  const deleteEnabled = editEnabled;
  const handleDelete = useCallback(() => {
    void (async () => {
      await onDelete(selectedRows!.map((ind) => rows[ind]?.record));
    })();
  }, [onDelete, selectedRows, rows]);

  const handlePrev = useCallback(
    () =>
      switchTo("grid", {
        route: { id: String(Math.max(minPage, currentPage - 1)) },
      }),
    [currentPage, minPage, switchTo]
  );
  const handleNext = useCallback(
    () =>
      switchTo("grid", {
        route: { id: String(Math.min(maxPage, currentPage + 1)) },
      }),
    [currentPage, maxPage, switchTo]
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
          const col = draft.selectedCell?.[1] ?? 0
          draft.selectedCell = [row, col];
        } else {
          draft.selectedCell = [0, 0];
          draft.selectedRows = [0];
        }
      });
    }, [setState]),
  });

  return (
    <div className={styles.container}>
      {showToolbar && (
        <ViewToolBar
          meta={meta}
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
            {...detailsProps}
          />
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
                >
                  <Box as="span">
                    {i18n.get("Either select or create a record.")}
                  </Box>
                </Box>
              )
            )}
          </Box>
        )}
      </div>
    </div>
  );
}
