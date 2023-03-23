import { useAtom } from "jotai";
import { atomWithImmer } from "jotai-immer";
import { useCallback, useEffect, useMemo } from "react";
import _ from "lodash";
import { Box } from "@axelor/ui";
import {
  Grid as AxGrid,
  GridProvider as AxGridProvider,
  GridColumn,
  GridRow,
  GridRowProps,
  GridState,
} from "@axelor/ui/grid";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { Field, JsonField, GridView } from "@/services/client/meta.types";
import { Row as RowRenderer } from "./renderers/row";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { ViewProps } from "../types";
import { useViewProps, useViewSwitch } from "@/view-containers/views/scope";
import { useAsync } from "@/hooks/use-async";
import { useDataStore } from "@/hooks/use-data-store";
import format from "@/utils/format";
import styles from "./grid.module.scss";

function formatter(column: Field, value: any, record: any) {
  return format(value, {
    props: column,
    context: record,
  });
}

export function Grid(props: ViewProps<GridView>) {
  const { meta, dataStore } = props;
  const { view, fields } = meta;
  const [viewProps, setViewProps] = useViewProps();
  const switchTo = useViewSwitch();

  const [state, setState] = useAtom(
    useMemo(
      () =>
        atomWithImmer<GridState>({
          rows: [],
          columns: [],
          selectedCell: viewProps?.selectedCell,
          selectedRows: viewProps?.selectedCell
            ? [viewProps?.selectedCell?.[0]!]
            : null,
        }),
      // eslint-disable-next-line react-hooks/exhaustive-deps
      []
    )
  );
  const records = useDataStore(dataStore, (ds) => ds.records);

  const { columns, names } = useMemo(() => {
    const names: string[] = [];
    const columns: GridColumn[] = view.items!.map((item) => {
      const field = fields?.[item.name!];
      const title = item.title ?? item.autoTitle;
      const attrs = item.widgetAttrs;
      const serverType = field?.type;

      if ((item as JsonField).jsonField) {
        names.push((item as JsonField).jsonField as string);
      } else if (field) {
        names.push(field.name);
      }

      return {
        ...field,
        ...item,
        ...attrs,
        serverType,
        title,
        formatter,
      } as any;
    });

    if (view.editIcon !== false) {
      columns.unshift({
        title: "",
        name: "$$edit",
        renderer: ({ style, onClick, className }: any) => (
          <Box
            d="flex"
            justifyContent="center"
            alignItems="center"
            {...{ style, onClick, className }}
          >
            <MaterialIcon icon="edit" opticalSize={20} />
          </Box>
        ),
        computed: true,
        sortable: false,
        searchable: false,
        width: 32,
      });
    }

    return { columns, names: _.uniq(names) };
  }, [view, fields]);

  const onSearch = useCallback(async () => {
    await dataStore.search({ fields: names });
  }, [dataStore, names]);

  const onEdit = useCallback(
    (record: GridRow["record"]) => {
      switchTo({
        id: record.id,
        mode: "edit",
      });
    },
    [switchTo]
  );

  const init = useAsync(async () => {
    if (dataStore.records.length === 0) {
      onSearch();
    }
  }, [dataStore]);

  const handleCellClick = useCallback(
    (
      e: React.SyntheticEvent,
      col: GridColumn,
      colIndex: number,
      row: GridRow,
      rowIndex: number
    ) => {
      if (col.name === "$$edit") {
        onEdit(row.record);
      }
    },
    [onEdit]
  );

  const handleRowDoubleClick = useCallback(
    (e: React.SyntheticEvent, row: GridRow, rowIndex: number) => {
      switchTo({
        id: row.record.id,
        mode: "view",
      });
    },
    [switchTo]
  );

  const CustomRowRenderer = useMemo(() => {
    const { hilites } = view;
    if (!(hilites || []).length) return;
    return (props: GridRowProps) => (
      <RowRenderer {...props} hilites={hilites} />
    );
  }, [view]);

  useEffect(() => {
    if (viewProps?.selectedCell !== state.selectedCell) {
      setViewProps({ selectedCell: state.selectedCell ?? undefined });
    }
  }, [viewProps, setViewProps, state.selectedCell]);

  if (init.state === "loading") return null;

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
            key: "save",
            text: "Save",
            iconProps: {
              icon: "save",
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
      />
      <AxGridProvider>
        <AxGrid
          allowColumnResize
          allowGrouping
          allowSorting
          allowSelection
          allowCellSelection
          allowColumnHide
          allowColumnOptions
          allowColumnCustomize
          sortType="state"
          selectionType="multiple"
          records={records}
          columns={columns}
          state={state}
          setState={setState}
          rowRenderer={CustomRowRenderer}
          onCellClick={handleCellClick}
          onRowDoubleClick={handleRowDoubleClick}
        />
      </AxGridProvider>
    </div>
  );
}
