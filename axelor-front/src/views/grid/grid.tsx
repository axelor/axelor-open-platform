import { atom, useAtom, useSetAtom } from "jotai";
import { useCallback, useEffect, useMemo, useRef } from "react";
import { ScopeProvider } from "jotai-molecules";
import { Box } from "@axelor/ui";
import { GridRow } from "@axelor/ui/grid";

import { AdvanceSearch } from "@/view-containers/advance-search";
import { dialogs } from "@/components/dialogs";
import { PageText } from "@/components/page-text";
import { SearchOptions } from "@/services/client/data";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { GridView } from "@/services/client/meta.types";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  GridSearchScope,
  GridSearchScopeState,
  SearchColumn,
  SearchState,
} from "./renderers/search";
import { getSearchFilter } from "./renderers/search/utils";
import {
  useViewProps,
  useViewRoute,
  useViewSwitch,
  useViewTab,
} from "@/view-containers/views/scope";

import { ViewProps } from "../types";
import { Grid as GridComponent } from "./builder";
import { useGridActionExecutor, useGridState } from "./builder/utils";
import { useDataStore } from "@/hooks/use-data-store";
import { useEditor } from "@/hooks/use-relation";
import { usePerms } from "@/hooks/use-perms";
import styles from "./grid.module.scss";

export function Grid(props: ViewProps<GridView>) {
  const { meta, dataStore, searchAtom, domains } = props;
  const { view, fields } = meta;
  const { hasButton } = usePerms(meta.view, meta.perms);

  const viewRoute = useViewRoute();
  const pageSetRef = useRef(false);
  const selectedIdsRef = useRef<number[]>([]);
  const [viewProps, setViewProps] = useViewProps();
  const { action, dashlet, popup, popupOptions } = useViewTab();

  const switchTo = useViewSwitch();
  const showEditor = useEditor();

  const [search, setSearch] = useAtom<any>(useMemo(() => atom({}), []));
  const [advanceSearch, setAdvancedSearch] = useAtom(searchAtom!);

  const [state, setState] = useGridState({
    selectedCell: viewProps?.selectedCell,
    selectedRows: viewProps?.selectedCell
      ? [viewProps?.selectedCell?.[0]!]
      : null,
  });
  const records = useDataStore(dataStore, (ds) => ds.records);
  const { orderBy, rows, selectedRows } = state;

  const clearSelection = useCallback(() => {
    setState((draft) => {
      draft.selectedRows = null;
      draft.selectedCell = null;
    });
  }, [setState]);

  const onSearch = useCallback(
    (options: SearchOptions = {}) => {
      const { query = {} } = advanceSearch;
      const { archived: _archived } = query;
      const sortBy = orderBy?.map(
        (column) => `${column.order === "desc" ? "-" : ""}${column.name}`
      );
      const { filter } = options;
      let { freeSearchText, ...filterQuery } = query;
      if (freeSearchText && filter?.criteria?.length) {
        filterQuery = {
          operator: "and",
          criteria: [
            { operator: "and", ...filter },
            {
              operator: query.operator || "or",
              criteria: query.criteria,
            },
          ],
        };
      } else {
        filterQuery.criteria = [...(filterQuery.criteria || [])].concat(
          filter?.criteria || []
        );
        freeSearchText && (filterQuery.operator = "or");
      }

      return dataStore.search({
        sortBy,
        ...options,
        filter: { ...filterQuery, _archived },
      });
    },
    [advanceSearch, dataStore, orderBy]
  );

  const onGridSearch = useCallback(
    (searchValues: SearchState) => {
      setSearch && (setSearch as CallableFunction)(searchValues);
      return onSearch({
        filter: getSearchFilter(fields as any, view.items, searchValues)!,
      });
    },
    [fields, view.items, setSearch, onSearch]
  );

  const onDelete = useCallback(
    async (records: GridRow["record"][]) => {
      const confirmed = await dialogs.confirm({
        title: i18n.get("Question"),
        content: i18n.get(
          "Do you really want to delete the selected record(s)?"
        ),
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

  const onViewInPopup = useCallback(
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

  const onEdit = useCallback(
    (record: DataRecord, readonly = false) => {
      if (dashlet) {
        return onViewInPopup(record, readonly);
      }
      const recordId = record.id || 0;
      const id = recordId > 0 ? String(recordId) : "";
      switchTo("form", {
        route: { id },
        props: { readonly },
      });
    },
    [dashlet, switchTo, onViewInPopup]
  );

  const onNew = useCallback(() => {
    onEdit({});
  }, [onEdit]);

  const onView = useCallback(
    (record: DataRecord) => {
      onEdit(record, true);
    },
    [onEdit]
  );

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
        onRefresh: () => onSearch({}),
      });
    }
  }, [dashlet, view, dataStore, onSearch, setDashletHandlers]);

  const { page } = dataStore;
  const { offset = 0, limit = 40, totalCount = 0 } = page;

  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;
  const hasRowSelected = !!selectedRows?.length;
  const currentPage = useMemo(() => {
    if (pageSetRef.current || dataStore.records.length === 0) {
      return +(viewRoute?.id || 1);
    }
    pageSetRef.current = true;
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
  const actionExecutor = useGridActionExecutor(view, getContext);

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
    if (viewProps?.selectedCell !== state.selectedCell) {
      const selectedCell = state.selectedCell || undefined;
      const selectedId = selectedCell
        ? state.rows[selectedCell[0]]?.record.id
        : undefined;
      setViewProps({
        selectedCell,
        selectedId,
      });
    }
  }, [viewProps, setViewProps, state.selectedCell, state.rows]);

  useEffect(() => {
    selectedIdsRef.current = (state.selectedRows || []).map(
      (ind) => state.rows[ind]?.record?.id
    );
  }, [state.selectedRows, state.rows]);

  const searchScope = useMemo<GridSearchScopeState>(
    () => ({
      search,
      setSearch,
      onSearch: onGridSearch,
    }),
    [search, setSearch, onGridSearch]
  );

  const searchOptions = useMemo(() => {
    if (currentPage) {
      return { offset: (currentPage - 1) * limit };
    }
  }, [currentPage, limit]);

  const showToolbar = popupOptions?.showToolbar !== false;
  const showEditIcon = popupOptions?.showEditIcon !== false;
  const showCheckbox = popupOptions?.multiSelect !== false;

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
        showEditIcon: viewProps?.readonly !== true,
      }
    : {
        allowSearch: true,
        searchRowRenderer: Box,
        searchColumnRenderer: SearchColumn,
      };

  return (
    <div className={styles.grid}>
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
              onClick: onNew,
            },
            {
              key: "edit",
              text: i18n.get("Edit"),
              hidden: !hasButton("edit"),
              iconProps: {
                icon: "edit",
              },
              disabled: !hasRowSelected,
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
              disabled: !hasRowSelected,
              onClick: () => {
                onDelete(selectedRows!.map((ind) => rows[ind]?.record));
              },
            },
            {
              key: "archive",
              text: "",
              hidden: !hasButton("archive"),
              disabled: !hasRowSelected,
              items: [
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
              ],
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
          <AdvanceSearch
            dataStore={dataStore}
            items={view.items}
            fields={fields}
            domains={domains}
            value={advanceSearch}
            setValue={setAdvancedSearch as any}
          />
        </ViewToolBar>
      )}

      <ScopeProvider scope={GridSearchScope} value={searchScope}>
        <GridComponent
          dataStore={dataStore}
          records={records}
          view={view}
          fields={fields}
          state={state}
          setState={setState}
          sortType={"live"}
          searchOptions={searchOptions}
          actionExecutor={actionExecutor}
          onEdit={onEdit}
          onView={onView}
          onSearch={onSearch}
          {...dashletProps}
          {...popupProps}
        />
      </ScopeProvider>
    </div>
  );
}
