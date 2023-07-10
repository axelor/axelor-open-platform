import uniq from "lodash/uniq";
import {
  forwardRef,
  useCallback,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  Grid as AxGrid,
  GridProvider as AxGridProvider,
  GridColumn,
  GridProps,
  GridRow,
  GridRowProps,
  getRows,
} from "@axelor/ui/grid";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";
import { ScopeProvider } from "jotai-molecules";

import { useAsync } from "@/hooks/use-async";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { MetaData } from "@/services/client/meta";
import { Field, GridView, JsonField } from "@/services/client/meta.types";
import format from "@/utils/format";
import { getDefaultValues, nextId } from "@/views/form/builder/utils";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useDevice } from "@/hooks/use-responsive";
import { useSession } from "@/hooks/use-session";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { ActionExecutor } from "@/view-containers/action";
import { Attrs } from "@/views/form/builder";
import { Cell as CellRenderer } from "../renderers/cell";
import { Form as FormRenderer, GridFormHandler } from "../renderers/form";
import { Row as RowRenderer } from "../renderers/row";
import { GridScope } from "./scope";

import styles from "../grid.module.scss";
import clsx from "clsx";

function formatter(column: Field, value: any, record: any) {
  return format(value, {
    props: column,
    context: record,
  });
}

export type GridHandler = {
  onAdd?: () => void;
  onSave?: () => void;
};

export const Grid = forwardRef<
  GridHandler,
  Partial<GridProps> & {
    view: GridView;
    fields?: MetaData["fields"];
    searchOptions?: Partial<SearchOptions>;
    editable?: boolean;
    readonly?: boolean;
    showEditIcon?: boolean;
    columnAttrs?: Record<string, Partial<Attrs>>;
    columnFormatter?: (column: Field, value: any, record: DataRecord) => string;
    actionExecutor?: ActionExecutor;
    onSearch?: (options?: SearchOptions) => Promise<SearchResult | undefined>;
    onEdit?: (record: GridRow["record"]) => any;
    onView?: (record: GridRow["record"]) => any;
    onSave?: (record: GridRow["record"]) => void;
    onDiscard?: (record: GridRow["record"]) => void;
  }
>(function Grid(props, ref) {
  const {
    view,
    fields,
    searchOptions,
    actionExecutor,
    showEditIcon = true,
    editable = false,
    readonly,
    columnAttrs,
    columnFormatter,
    records,
    state,
    setState,
    onSearch,
    onEdit,
    onView,
    onSave,
    onDiscard,
    className,
    ...gridProps
  } = props;

  const formRef = useRef<GridFormHandler>(null);
  const [event, setEvent] = useState("");
  const { isMobile } = useDevice();
  const { data: user } = useSession();
  const allowCheckboxSelection =
    (view.selector ?? user?.view?.grid?.selection ?? "checkbox") === "checkbox";

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
        formatter: columnFormatter || formatter,
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
  }, [
    view.items,
    view.editIcon,
    showEditIcon,
    fields,
    columnFormatter,
    columnAttrs,
  ]);

  const init = useAsync(async () => {
    onSearch?.({ ...searchOptions, fields: names });
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
      } else if (isMobile) {
        onView?.(row.record);
      }
    },
    [isMobile, onEdit, onView]
  );

  const handleRowDoubleClick = useCallback(
    (e: React.SyntheticEvent, row: GridRow, rowIndex: number) => {
      onView?.(row.record);
    },
    [onView]
  );

  const commitForm = useCallback(async () => {
    // save current edit row
    const form = formRef.current;
    if (form) {
      return await form?.onSave?.(true);
    }
  }, []);

  const doAdd = useCallback(async () => {
    const newRecord = { id: nextId(), ...getDefaultValues(fields) };
    const newRecords = [...(records || []), newRecord];
    setState?.((draft) => {
      const { rows, columns, orderBy, groupBy } = draft;
      const newRows: GridRow[] = getRows({
        rows,
        columns,
        orderBy,
        groupBy,
        records: newRecords,
      });

      draft.rows = newRows;
      draft.selectedCell = null;
      draft.selectedRows = null;
      draft.editRow = [
        newRows.findIndex((r) => r?.record?.id === newRecord.id),
        null,
      ];
    });
  }, [fields, records, setState]);

  const handleRecordAdd = useCallback(async () => {
    setEvent("editable:add-new");
    return true;
  }, []);

  const handleRecordEdit = useCallback(
    async (
      row: GridRow,
      rowIndex?: number,
      column?: GridColumn,
      colIndex?: number
    ) => {
      // skip edit row for edit icon
      if (column?.name === "$$edit") return null;
      await commitForm();
    },
    [commitForm]
  );

  const handleRecordDiscard = useCallback(
    async (record: DataRecord) => {
      // on record discard
      if ((record.id ?? -1) < 0 && !record._dirty) {
        setState?.((draft) => {
          draft.rows = draft.rows.filter((r) => r?.record?.id !== record.id);
        });
      }
      onDiscard?.(record);
    },
    [onDiscard, setState]
  );

  const CustomRowRenderer = useMemo(() => {
    const { hilites } = view;
    if (!(hilites || []).length) return;
    return (props: GridRowProps) => (
      <RowRenderer {...props} hilites={hilites} />
    );
  }, [view]);

  const CustomCellRenderer = useMemo(
    () => (props: GridColumnProps) =>
      (
        <CellRenderer
          {...props}
          {...(actionExecutor && {
            onAction: (action: string, context?: DataContext) =>
              actionExecutor.execute(action, { context }),
          })}
          view={view}
        />
      ),
    []
  );

  const CustomFormRenderer = useMemo(() => {
    return (props: GridRowProps) => (
      <FormRenderer ref={formRef} {...props} view={view} fields={fields} />
    );
  }, [view, fields]);

  useImperativeHandle(
    ref,
    () => ({
      onAdd: handleRecordAdd,
    }),
    [handleRecordAdd]
  );

  useAsyncEffect(
    async (signal: AbortSignal) => {
      if (signal.aborted) return;
      if (event === "editable:add-new") {
        const form = formRef.current;
        if (form && form.invalid?.()) {
          return;
        }
        doAdd();
      }
      setEvent("");
    },
    [doAdd, event]
  );

  if (init.state === "loading") return null;

  return (
    <AxGridProvider>
      <ScopeProvider scope={GridScope} value={{ readonly }}>
        <AxGrid
          cellRenderer={CustomCellRenderer}
          rowRenderer={CustomRowRenderer}
          allowColumnResize
          allowGrouping
          allowSorting
          allowSelection
          allowCellSelection
          allowColumnHide
          allowColumnOptions
          allowColumnCustomize
          allowCheckboxSelection={allowCheckboxSelection}
          allowRowReorder={view?.canMove === true && !readonly}
          sortType="state"
          selectionType="multiple"
          {...(editable &&
            !isMobile && {
              editable,
              editRowRenderer: CustomFormRenderer,
              onRecordSave: onSave,
              onRecordAdd: handleRecordAdd,
              onRecordEdit: handleRecordEdit,
              onRecordDiscard: handleRecordDiscard,
            })}
          onCellClick={handleCellClick}
          onRowDoubleClick={handleRowDoubleClick}
          state={state!}
          setState={setState!}
          records={records!}
          {...gridProps}
          columns={columns}
          className={clsx(className, styles.grid)}
        />
      </ScopeProvider>
    </AxGridProvider>
  );
});
