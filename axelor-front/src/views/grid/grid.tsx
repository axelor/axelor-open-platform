import { atom, useAtom, useSetAtom } from "jotai";
import { useCallback, useEffect, useMemo, useRef } from "react";

import { GridRow } from "@axelor/ui/grid";

import { dialogs } from "@/components/dialogs";
import { PageText } from "@/components/page-text";
import { SearchOptions } from "@/services/client/data";
import { i18n } from "@/services/client/i18n";
import { GridView } from "@/services/client/meta.types";
import { AdvanceSearch } from "@/view-containers/advance-search";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  useViewProps,
  useViewRoute,
  useViewSwitch,
  useViewTab,
} from "@/view-containers/views/scope";

import { ViewProps } from "../types";
import { Grid as GridComponent } from "./builder";
import { useGridState } from "./builder/utils";

import styles from "./grid.module.scss";

export function Grid(props: ViewProps<GridView>) {
  const { meta, dataStore, domains } = props;
  const { view, fields } = meta;

  const viewRoute = useViewRoute();
  const [viewProps, setViewProps] = useViewProps();
  const pageSetRef = useRef(false);

  const switchTo = useViewSwitch();

  const [advanceSearch, setAdvancedSearch] = useAtom<any>(
    useMemo(() => atom({}), [])
  );

  const [state, setState] = useGridState({
    selectedCell: viewProps?.selectedCell,
    selectedRows: viewProps?.selectedCell
      ? [viewProps?.selectedCell?.[0]!]
      : null,
  });

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
      return dataStore.search({
        sortBy,
        filter: { ...query, _archived },
        ...options,
      });
    },
    [advanceSearch, dataStore, orderBy]
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

  const onEdit = useCallback(
    (record: GridRow["record"]) => {
      switchTo("form", {
        route: { id: record.id },
        props: { readonly: false },
      });
    },
    [switchTo]
  );

  const onView = useCallback(
    (record: GridRow["record"]) => {
      switchTo("form", {
        route: { id: record.id },
        props: { readonly: true },
      });
    },
    [switchTo]
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

  const { popup, popupOptions } = useViewTab();
  const popupHandlerAtom = usePopupHandlerAtom();
  const setPopupHandlers = useSetAtom(popupHandlerAtom);

  useEffect(() => {
    if (popup) {
      setPopupHandlers({
        data: state,
        dataStore: dataStore,
        onSearch,
      });
    }
  }, [state, onSearch, popup, dataStore, setPopupHandlers]);

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

  useEffect(() => {
    let nextPage = currentPage;
    if (offset > totalCount) {
      nextPage = Math.ceil(totalCount / limit) || nextPage;
    }
    switchTo("grid", {
      route: { id: String(nextPage) },
    });
  }, [currentPage, limit, offset, switchTo, totalCount]);

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

  return (
    <div className={styles.grid}>
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          actions={[
            {
              key: "new",
              text: i18n.get("New"),
              iconProps: {
                icon: "add",
              },
            },
            {
              key: "edit",
              text: i18n.get("Edit"),
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
              iconProps: {
                icon: "delete",
              },
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
              disabled: !hasRowSelected,
              onClick: () => {
                onDelete(selectedRows!.map((ind) => rows[ind]?.record));
              },
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
            setValue={setAdvancedSearch}
          />
        </ViewToolBar>
      )}
      <GridComponent
        dataStore={dataStore}
        view={view}
        fields={fields}
        state={state}
        setState={setState}
        sortType={"live"}
        searchOptions={searchOptions}
        onEdit={onEdit}
        onView={onView}
        onSearch={onSearch}
        {...popupProps}
      />
    </div>
  );
}
