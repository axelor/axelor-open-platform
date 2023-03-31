import { useCallback, useEffect, useMemo } from "react";
import { GridRow } from "@axelor/ui/grid";
import { Box } from "@axelor/ui";
import { atom, useAtom } from "jotai";
import { GridView } from "@/services/client/meta.types";
import { SearchOptions } from "@/services/client/data";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { Grid as GridComponent } from "./builder";
import { ViewProps } from "../types";
import { useViewProps, useViewSwitch } from "@/view-containers/views/scope";
import { useGridState } from "./builder/utils";
import { i18n } from "@/services/client/i18n";
import { dialogs } from "@/components/dialogs";
import AdvanceSearch from "@/view-containers/advance-search";
import styles from "./grid.module.scss";

export function Grid(props: ViewProps<GridView>) {
  const { meta, dataStore, domains } = props;
  const { view, fields } = meta;
  const [viewProps, setViewProps] = useViewProps();
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
      const { query } = advanceSearch as any;
      const sortBy = orderBy
        ? orderBy.map(
            (column) => `${column.order === "desc" ? "-" : ""}${column.name}`
          )
        : null;
      return dataStore.search({
        ...(sortBy ? { sortBy } : {}),
        filter: query
          ? {
              _archived: query.archived,
              ...query,
            }
          : {},
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
      switchTo({
        id: record.id,
        mode: "edit",
      });
    },
    [switchTo]
  );

  const onView = useCallback(
    (record: GridRow["record"]) => {
      switchTo({
        id: record.id,
        mode: "view",
      });
    },
    [switchTo]
  );

  const onArchiveOrUnArchive = useCallback(
    async (archived: boolean) => {
      const confirmed = await dialogs.confirm({
        title: i18n.get("Question"),
        content: i18n.get(
          `Do you really want to ${
            archived ? "" : "un"
          }archive the selected record(s)?`
        ),
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

  useEffect(() => {
    if (viewProps?.selectedCell !== state.selectedCell) {
      setViewProps({ selectedCell: state.selectedCell! });
    }
  }, [viewProps, setViewProps, state.selectedCell]);

  const { page } = dataStore;
  const canPrev = page.offset! > 0;
  const canNext = page.offset! + page.limit! < page.totalCount!;
  const hasRowSelected = (selectedRows || []).length > 0;

  return (
    <div className={styles.grid}>
      <ViewToolBar
        meta={meta}
        actions={[
          {
            key: "new",
            text: "New",
            iconProps: {
              icon: "add",
            },
          },
          {
            key: "edit",
            text: "Edit",
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
            text: "Delete",
            iconProps: {
              icon: "delete",
            },
            items: [
              {
                key: "archive",
                text: "Archive",
                onClick: () => onArchiveOrUnArchive(true),
              },
              {
                key: "unarchive",
                text: "Unarchive",
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
            text: "Refresh",
            iconProps: {
              icon: "refresh",
            },
            onClick: () => onSearch(),
          },
        ]}
        pagination={{
          onPrev: () =>
            canPrev && onSearch({ offset: page.offset! - page.limit! }),
          onNext: () =>
            canNext && onSearch({ offset: page.offset! + page.limit! }),
          text: () => (
            <Box>
              <Box as="span" className={styles.paginationText}>
                {page.offset! + 1} to{" "}
                {Math.min(
                  page.offset! + page.limit!,
                  page.totalCount as number
                )}{" "}
                of {page.totalCount}
              </Box>
            </Box>
          ),
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
      <GridComponent
        dataStore={dataStore}
        view={view}
        fields={fields}
        state={state}
        setState={setState}
        sortType={"live"}
        onEdit={onEdit}
        onView={onView}
        onSearch={onSearch}
      />
    </div>
  );
}
