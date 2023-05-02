import uniq from "lodash/uniq";
import { useCallback, useMemo, useRef } from "react";

import {
  Grid as AxGrid,
  GridProvider as AxGridProvider,
  GridColumn,
  GridProps,
  GridRow,
  GridRowProps,
} from "@axelor/ui/grid";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

import { useAsync } from "@/hooks/use-async";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { MetaData } from "@/services/client/meta";
import { Field, GridView, JsonField } from "@/services/client/meta.types";
import format from "@/utils/format";

import { Attrs } from "@/views/form/builder";
import { Cell as CellRenderer } from "../renderers/cell";
import { Row as RowRenderer } from "../renderers/row";
import { Form as FormRenderer, GridFormHandler } from "../renderers/form";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { ActionExecutor } from "@/view-containers/action";

function formatter(column: Field, value: any, record: any) {
  return format(value, {
    props: column,
    context: record,
  });
}

export function Grid(
  props: Partial<GridProps> & {
    view: GridView;
    fields?: MetaData["fields"];
    searchOptions?: Partial<SearchOptions>;
    editable?: boolean;
    showEditIcon?: boolean;
    columnAttrs?: Record<string, Partial<Attrs>>;
    actionExecutor?: ActionExecutor;
    onSearch: (options?: SearchOptions) => Promise<SearchResult | undefined>;
    onEdit?: (record: GridRow["record"]) => any;
    onView?: (record: GridRow["record"]) => any;
  }
) {
  const {
    view,
    fields,
    searchOptions,
    actionExecutor,
    showEditIcon = true,
    editable = true,
    columnAttrs,
    onSearch,
    onEdit,
    onView,
    ...gridProps
  } = props;

  const formRef = useRef<GridFormHandler>(null);

  const names = useMemo(
    () =>
      uniq(
        view.items!.reduce((names, item) => {
          const field = fields?.[item.name!];
          if ((item as JsonField).jsonField) {
            return [...names, (item as JsonField).jsonField as string];
          } else if (field) {
            return [...names, field.name];
          }
          return names;
        }, [] as string[])
      ),
    [fields, view.items]
  );

  const columns = useMemo(() => {
    const columns: GridColumn[] = view.items!.map((item) => {
      const field = fields?.[item.name!];
      const title = item.title ?? item.autoTitle;
      const attrs = item.widgetAttrs;
      const serverType = field?.type;
      const columnProps: Partial<GridColumn> = {};
      const extraAttrs = columnAttrs?.[item.name!];

      if (item.width) {
        columnProps.width = parseInt(item.width as string);
        columnProps.computed = true;
      }

      if (item.type === "button" || attrs?.type === "icon") {
        columnProps.searchable = false;
        columnProps.computed = true;
        columnProps.width = columnProps.width || 40;
        columnProps.title = " ";
        columnProps.action = true;
      }

      if (field?.type === "BOOLEAN" && !item.widget) {
        (columnProps as Field).widget = "boolean";
      }

      if (item.hidden || extraAttrs?.hidden) {
        columnProps.visible = false;
      }

      return {
        ...field,
        ...item,
        ...attrs,
        serverType,
        title,
        formatter,
        ...columnProps,
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
        width: 40,
      } as GridColumn);
    }

    return columns;
  }, [view.items, view.editIcon, showEditIcon, fields, columnAttrs]);

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
        onEdit?.(row.record);
      }
    },
    [onEdit]
  );

  const handleRowDoubleClick = useCallback(
    (e: React.SyntheticEvent, row: GridRow, rowIndex: number) => {
      onView?.(row.record);
    },
    [onView]
  );

  const handleRecordEdit = useCallback(
    async (
      row: GridRow,
      rowIndex?: number,
      column?: GridColumn,
      colIndex?: number
    ) => {
      // skip edit row for edit icon
      if (column?.name === "$$edit") return null;

      // save current edit row
      const form = formRef.current;
      if (form) {
        return await form?.onSave?.();
      }
    },
    []
  );

  const handleRecordDiscard = useCallback(async (record: DataRecord) => {
    // TODO: on record discard
  }, []);

  const CustomRowRenderer = useMemo(() => {
    const { hilites } = view;
    if (!(hilites || []).length) return;
    return (props: GridRowProps) => (
      <RowRenderer {...props} hilites={hilites} />
    );
  }, [view]);

  const ActionCellRenderer = useMemo(() => {
    return (props: GridColumnProps) => (
      <CellRenderer
        {...props}
        onAction={(action: string, context?: DataContext) =>
          actionExecutor!.execute(action, { context })
        }
      />
    );
  }, [actionExecutor]);

  const CustomFormRenderer = useMemo(() => {
    return (props: GridRowProps) => (
      <FormRenderer ref={formRef} {...props} view={view} fields={fields} />
    );
  }, [view, fields]);

  const hasActionCell = useMemo(
    () => columns.some((c) => ["button"].includes(c.type!)),
    [columns]
  );

  if (init.state === "loading") return null;

  return (
    <AxGridProvider>
      <AxGrid
        cellRenderer={
          hasActionCell && actionExecutor ? ActionCellRenderer : CellRenderer
        }
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
        {...(editable &&
          view.editable && {
            editable: true,
            editRowRenderer: CustomFormRenderer,
            onRecordEdit: handleRecordEdit,
            onRecordDiscard: handleRecordDiscard,
          })}
        onCellClick={handleCellClick}
        onRowDoubleClick={handleRowDoubleClick}
        {...gridProps}
        records={gridProps.records!}
        state={gridProps.state!}
        setState={gridProps.setState!}
        columns={columns}
      />
    </AxGridProvider>
  );
}
