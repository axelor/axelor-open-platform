import { useCallback, useEffect } from "react";
import { GridRow } from "@axelor/ui/grid";
import { Box } from "@axelor/ui";
import { GridView } from "@/services/client/meta.types";
import { SearchOptions } from "@/services/client/data";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { Grid as GridComponent } from "./builder";
import { ViewProps } from "../types";
import { useViewProps, useViewSwitch } from "@/view-containers/views/scope";
import { useGridState } from "./builder/utils";
import styles from "./grid.module.scss";

export function Grid(props: ViewProps<GridView>) {
  const { meta, dataStore } = props;
  const { view, fields } = meta;
  const [viewProps, setViewProps] = useViewProps();
  const switchTo = useViewSwitch();

  const [state, setState] = useGridState({
    selectedCell: viewProps?.selectedCell,
    selectedRows: viewProps?.selectedCell
      ? [viewProps?.selectedCell?.[0]!]
      : null,
  });
  const { orderBy } = state;

  const onSearch = useCallback(
    (options: SearchOptions = {}) => {
      const sortBy = orderBy
        ? orderBy.map(
            (column) => `${column.order === "desc" ? "-" : ""}${column.name}`
          )
        : null;
      return dataStore.search({ ...(sortBy ? { sortBy } : {}), ...options });
    },
    [dataStore, orderBy]
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

  useEffect(() => {
    if (viewProps?.selectedCell !== state.selectedCell) {
      setViewProps({ selectedCell: state.selectedCell! });
    }
  }, [viewProps, setViewProps, state.selectedCell]);

  const { page } = dataStore;
  const canPrev = page.offset! > 0;
  const canNext = page.offset! + page.limit! < page.totalCount!;

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
            disabled: (state?.selectedRows || []).length === 0,
            onClick: () => {
              const { rows, selectedRows } = state;
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
              },
              {
                key: "unarchive",
                text: "Unarchive",
              },
            ],
            onClick: () => {},
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
              <Box as="span">
                {page.offset! + 1} to {page.offset! + page.limit!} of{" "}
                {page.totalCount}
              </Box>
            </Box>
          ),
        }}
      />
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
