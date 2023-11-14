import clsx from "clsx";
import { atom, useAtomValue } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { focusAtom } from "jotai-optics";
import get from "lodash/get";
import uniq from "lodash/uniq";
import uniqueId from "lodash/uniqueId";
import {
  RefObject,
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
  GridLabel,
  GridProps,
  GridRow,
  GridRowProps,
  getRows,
} from "@axelor/ui/grid";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { usePermitted } from "@/hooks/use-permitted";
import { useDevice } from "@/hooks/use-responsive";
import { useSession } from "@/hooks/use-session";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { MetaData } from "@/services/client/meta";
import {
  AdvancedSearchAtom,
  Field,
  FormView,
  GridView,
  JsonField,
  Perms,
  Property,
} from "@/services/client/meta.types";
import format from "@/utils/format";
import { toKebabCase } from "@/utils/names";
import { ActionExecutor } from "@/view-containers/action";
import { Attrs } from "@/views/form/builder";
import { getDefaultValues, nextId } from "@/views/form/builder/utils";

import { Cell as CellRenderer } from "../renderers/cell";
import { Form as FormRenderer, GridFormHandler } from "../renderers/form";
import { Row as RowRenderer } from "../renderers/row";
import { GridScope } from "./scope";

import styles from "../grid.module.scss";

function formatter(column: Field, _value: any, record: DataRecord) {
  const { name } = column;
  const value =
    (name?.includes(".") ? get(record, (name ?? "").split(".")) : null) ??
    get(record, name);
  return format(value, {
    props: column,
    context: record,
  });
}

let labels: Record<GridLabel, string>;

const getLabels: () => Record<GridLabel, string> = () =>
  labels ||
  (labels = {
    Sum: i18n.get("Sum"),
    Min: i18n.get("Min"),
    Max: i18n.get("Max"),
    Avg: i18n.get("Avg"),
    Count: i18n.get("Count"),
    items: i18n.get("items"),
    Ungroup: i18n.get("Ungroup"),
    Hide: i18n.get("Hide"),
    Show: i18n.get("Show"),
    Groups: i18n.get("Groups"),
    "Sort Ascending": i18n.get("Sort Ascending"),
    "Sort Descending": i18n.get("Sort Descending"),
    "Group by": i18n.get("Group by"),
    "Customize...": i18n.get("Customize..."),
    "No records found.": i18n.get("No records found."),
  });

export type GridHandler = {
  form?: RefObject<GridFormHandler>;
  onAdd?: () => void;
  onSave?: () => void;
};

export const Grid = forwardRef<
  GridHandler,
  Partial<GridProps> & {
    view: GridView;
    fields?: MetaData["fields"];
    perms?: Perms;
    searchOptions?: Partial<SearchOptions>;
    searchAtom?: AdvancedSearchAtom;
    editable?: boolean;
    readonly?: boolean;
    showEditIcon?: boolean;
    columnAttrs?: Record<string, Partial<Attrs>>;
    columnFormatter?: (column: Field, value: any, record: DataRecord) => string;
    actionExecutor?: ActionExecutor;
    onFormInit?: () => void;
    onSearch?: (options?: SearchOptions) => Promise<SearchResult | undefined>;
    onEdit?: (record: GridRow["record"]) => any;
    onView?: (record: GridRow["record"]) => any;
    onUpdate?: (record: GridRow["record"]) => void;
    onSave?: (record: GridRow["record"]) => void;
    onDiscard?: (record: GridRow["record"]) => void;
  }
>(function Grid(props, ref) {
  const {
    view,
    fields,
    perms,
    searchOptions,
    searchAtom,
    actionExecutor,
    showEditIcon = true,
    editable = false,
    readonly,
    columnAttrs,
    columnFormatter,
    records,
    state,
    setState,
    onFormInit,
    onSearch,
    onEdit,
    onUpdate,
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
        }, [] as string[]),
      ),
    [fields, view.items],
  );

  const viewItems = useMemo(
    () =>
      (view.items || []).map((item) => ({
        ...item,
        id: uniqueId(view.name || "grid-column"),
      })),
    [view.name, view.items],
  );

  const contextField = useAtomValue(
    useMemo(
      () =>
        searchAtom
          ? focusAtom(searchAtom, (o) => o.prop("appliedContextField"))
          : atom(undefined),
      [searchAtom],
    ),
  );

  const columns = useMemo(() => {
    const { field, value } = contextField ?? {};
    const { name } = field ?? {};
    const { id } = value ?? {};
    const activeContextField = id ? { name, id: String(id) } : null;

    const columns: GridColumn[] = viewItems.map((item) => {
      const field = fields?.[item.name!];
      const title = item.title ?? item.autoTitle;
      const attrs = item.widgetAttrs;
      const serverType = (item as Field).serverType || field?.type;
      const columnProps: Partial<GridColumn> = {};
      const extraAttrs = columnAttrs?.[item.name!];

      if (view.sortable === false) {
        columnProps.sortable = (item as Field).sortable === true;
      }

      if (item.width) {
        columnProps.width = parseInt(item.width as string);
        columnProps.computed = true;
      }

      if (item.type === "button" || attrs?.type === "icon") {
        columnProps.sortable = false;
        columnProps.searchable = false;
        columnProps.editable = false;
        columnProps.computed = true;
        columnProps.width = columnProps.width || 40;
        columnProps.title = " ";
        columnProps.action = true;
      }

      const jsonField = (item as unknown as JsonField).jsonField;
      const searchable =
        jsonField ||
        (field && // check dummy
          !field.transient &&
          !field.json &&
          !field.encrypted &&
          !["one-to-many", "many-to-many"].includes(toKebabCase(field.type)));

      if (!searchable) {
        columnProps.sortable = false;
        columnProps.searchable = false;
      }

      if (
        ["DECIMAL", "INTEGER", "LONG"].includes(serverType ?? "") &&
        !(item as Field).selection
      ) {
        columnProps.$css = clsx(styles.number);
      }

      if (extraAttrs?.hidden ?? item.hidden) {
        columnProps.visible = false;
      }

      const { contextField, contextFieldValue } = item as Record<string, any>;

      if (
        contextField &&
        (!activeContextField ||
          contextField !== activeContextField.name ||
          String(contextFieldValue) !== activeContextField.id)
      ) {
        columnProps.visible = false;
        columnProps.hidden = true;
      }

      return {
        ...field,
        ...item,
        ...attrs,
        serverType,
        title,
        formatter: columnFormatter || formatter,
        ...columnProps,
        ...extraAttrs,
      } as any;
    });

    if (showEditIcon && view.editIcon !== false) {
      columns.unshift({
        title: "",
        name: "$$edit",
        widget: "edit-icon",
        computed: true,
        editable: false,
        sortable: false,
        searchable: false,
        width: 40,
      } as GridColumn);
    }

    return columns;
  }, [
    viewItems,
    view.sortable,
    view.editIcon,
    showEditIcon,
    fields,
    columnFormatter,
    columnAttrs,
    contextField,
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
      rowIndex: number,
    ) => {
      if (col.name === "$$edit") {
        onEdit?.(row.record);
      } else if (isMobile) {
        onView?.(row.record);
      }
    },
    [isMobile, onEdit, onView],
  );

  const handleRowDoubleClick = useCallback(
    (e: React.SyntheticEvent, row: GridRow, rowIndex: number) => {
      onView?.(row.record);
    },
    [onView],
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

  const model = view.model ?? (view as unknown as Property)?.target ?? "";

  const isPermitted = usePermitted(model, perms);

  const handleRecordAdd = useCallback(async () => {
    setEvent("editable:add-new");
    return true;
  }, []);

  const handleRecordEdit = useCallback(
    async (
      row: GridRow,
      rowIndex?: number,
      column?: GridColumn,
      colIndex?: number,
    ) => {
      // Skip edit row for edit icon and check write permission
      if (
        ["icon", "button"].includes(column?.type ?? "") ||
        column?.name === "$$edit" ||
        !(await isPermitted(row.record, false, true))
      ) {
        return null;
      }

      await commitForm();
    },
    [commitForm, isPermitted],
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
    [onDiscard, setState],
  );

  const CustomRowRenderer = useMemo(() => {
    const { hilites } = view;
    if (!(hilites || []).length) return;
    return (props: GridRowProps) => (
      <RowRenderer {...props} hilites={hilites} />
    );
  }, [view]);

  const CustomCellRenderer = useMemo(
    () => (props: GridColumnProps) => (
      <CellRenderer
        {...props}
        onUpdate={onUpdate}
        view={view}
        actionExecutor={actionExecutor}
      />
    ),
    [view, actionExecutor, onUpdate],
  );

  const CustomFormRenderer = useMemo(() => {
    const items = view.items?.map((item) => {
      const found = columns.find((x) => x.name === item.name);
      if (found) return found;
      return item;
    });
    const formView = { ...view, type: "form", items } as FormView;
    return (props: GridRowProps) => (
      <FormRenderer
        ref={formRef}
        {...props}
        view={formView}
        fields={fields}
        onInit={onFormInit}
      />
    );
  }, [onFormInit, view, columns, fields]);

  useImperativeHandle(
    ref,
    () => ({
      form: formRef,
      onAdd: handleRecordAdd,
    }),
    [formRef, handleRecordAdd],
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
    [doAdd, event],
  );

  if (init.state === "loading") return null;

  return (
    <AxGridProvider>
      <ScopeProvider scope={GridScope} value={{ readonly }}>
        <AxGrid
          labels={getLabels()}
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
