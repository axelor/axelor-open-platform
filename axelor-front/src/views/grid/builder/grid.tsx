import { useCallback, useMemo } from "react";
import {
  Grid as AxGrid,
  GridProvider as AxGridProvider,
  GridColumn,
  GridProps,
  GridRow,
  GridRowProps,
} from "@axelor/ui/grid";
import { Field, JsonField, GridView } from "@/services/client/meta.types";
import { MetaData } from "@/services/client/meta";
import { DataStore } from "@/services/client/data-store";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { Row as RowRenderer } from "../renderers/row";
import { Cell as CellRenderer } from "../renderers/cell";
import { useAsync } from "@/hooks/use-async";
import { useDataStore } from "@/hooks/use-data-store";
import uniq from "lodash/uniq";
import format from "@/utils/format";

function formatter(column: Field, value: any, record: any) {
  return format(value, {
    props: column,
    context: record,
  });
}

export function Grid(
  props: Partial<GridProps> & {
    dataStore: DataStore;
    view: GridView;
    fields?: MetaData["fields"];
    searchOptions?: Partial<SearchOptions>;
    showEditIcon?: boolean;
    onSearch: (options?: SearchOptions) => Promise<SearchResult>;
    onEdit?: (record: GridRow["record"]) => any;
    onView?: (record: GridRow["record"]) => any;
  }
) {
  const {
    view,
    fields,
    dataStore,
    searchOptions,
    showEditIcon = true,
    onSearch,
    onEdit,
    onView,
    ...gridProps
  } = props;

  const records = useDataStore(dataStore, (ds) => ds.records);

  const { columns, names } = useMemo(() => {
    const names: string[] = [];
    const columns: GridColumn[] = view.items!.map((item) => {
      const field = fields?.[item.name!];
      const title = item.title ?? item.autoTitle;
      const attrs = item.widgetAttrs;
      const serverType = field?.type;
      const columnAttrs: Partial<GridColumn> = {};

      if ((item as JsonField).jsonField) {
        names.push((item as JsonField).jsonField as string);
      } else if (field) {
        names.push(field.name);
      }

      if (item.width) {
        columnAttrs.width = parseInt(item.width as string);
        columnAttrs.computed = true;
      }

      if (item.type === "button") {
        columnAttrs.computed = true;
        columnAttrs.width = columnAttrs.width || 32;
        columnAttrs.title = " ";
      }

      if (field?.type === "BOOLEAN" && !item.widget) {
        (columnAttrs as Field).widget = "boolean";
      }

      if (item.hidden) {
        columnAttrs.visible = false;
      }

      return {
        ...field,
        ...item,
        ...attrs,
        serverType,
        title,
        formatter,
        ...columnAttrs,
      } as any;
    });

    if (showEditIcon && view.editIcon !== false) {
      columns.unshift({
        title: "",
        name: "$$edit",
        widget: "edit-icon",
        computed: true,
        sortable: false,
        searchable: false,
        width: 32,
      } as GridColumn);
    }

    return { columns, names: uniq(names) };
  }, [view.items, view.editIcon, showEditIcon, fields]);

  const init = useAsync(async () => {
    onSearch({ ...searchOptions, fields: names });
  }, [onSearch, searchOptions, names]);

  const handleCellClick = useCallback(
    (
      e: React.SyntheticEvent,
      col: GridColumn,
      colIndex: number,
      row: GridRow,
      rowIndex: number
    ) => {
      if (col.name === "$$edit") {
        onEdit && onEdit(row.record);
      }
    },
    [onEdit]
  );

  const handleRowDoubleClick = useCallback(
    (e: React.SyntheticEvent, row: GridRow, rowIndex: number) => {
      onView &&
        onView({
          id: row.record.id,
          mode: "view",
        });
    },
    [onView]
  );

  const CustomRowRenderer = useMemo(() => {
    const { hilites } = view;
    if (!(hilites || []).length) return;
    return (props: GridRowProps) => (
      <RowRenderer {...props} hilites={hilites} />
    );
  }, [view]);

  if (init.state === "loading") return null;

  return (
    <AxGridProvider>
      <AxGrid
        cellRenderer={CellRenderer}
        rowRenderer={CustomRowRenderer}
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
        onCellClick={handleCellClick}
        onRowDoubleClick={handleRowDoubleClick}
        {...gridProps}
        state={gridProps.state!}
        setState={gridProps.setState!}
        columns={columns}
        records={records}
      />
    </AxGridProvider>
  );
}
