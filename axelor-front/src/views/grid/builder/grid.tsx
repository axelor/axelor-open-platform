import { clsx } from "@axelor/ui";
import { atom, useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
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
  GridColumnProps,
  GridLabel,
  GridProps,
  GridRow,
  GridRowProps,
  getRows,
} from "@axelor/ui/grid";

import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { usePermitted } from "@/hooks/use-permitted";
import { useDevice } from "@/hooks/use-responsive";
import { useSession } from "@/hooks/use-session";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { MetaData, ViewData } from "@/services/client/meta";
import {
  AdvancedSearchAtom,
  Field,
  FormView,
  GridView,
  JsonField,
  Perms,
  Property,
} from "@/services/client/meta.types";
import { getFieldValue } from "@/utils/data-record";
import format from "@/utils/format";
import { toKebabCase } from "@/utils/names";
import { ActionExecutor } from "@/view-containers/action";
import { Attrs } from "@/views/form/builder";
import { findView } from "@/services/client/meta-cache";
import { getDefaultValues, nextId } from "@/views/form/builder/utils";
import { useViewAction } from "@/view-containers/views/scope";

import {
  getWidget,
  isValidSequence,
  useGridSortHandler,
} from "../builder/utils";
import { Cell as CellRenderer } from "../renderers/cell";
import { Form as FormRenderer, GridFormHandler } from "../renderers/form";
import { Row as RowRenderer } from "../renderers/row";
import {
  GridContext,
  useCollectionTreeEditable,
  useGridColumnNames,
} from "./scope";
import { ExpandIcon, ExpandableFormView } from "./expandable";

import styles from "../grid.module.scss";

function formatter(column: Field, value: any, record: any) {
  return format(value, {
    props: column,
    context: record,
  });
}

function columnValueGetter(column: Field, record: any) {
  return getFieldValue(record, column as Field);
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
  commit?: () => void;
};

export const Grid = forwardRef<
  GridHandler,
  Partial<GridProps> & {
    view: GridView;
    expandable?: boolean;
    expandableView?: string | ViewData<FormView>;
    fields?: MetaData["fields"];
    perms?: Perms;
    searchOptions?: Partial<SearchOptions>;
    searchAtom?: AdvancedSearchAtom;
    editable?: boolean;
    readonly?: boolean;
    showAsTree?: boolean;
    showNewIcon?: boolean;
    showEditIcon?: boolean;
    showDeleteIcon?: boolean;
    columnAttrs?: Record<string, Partial<Attrs>>;
    columnFormatter?: (column: Field, value: any, record: DataRecord) => string;
    actionExecutor?: ActionExecutor;
    onFormInit?: () => void;
    onSearch?: (options?: SearchOptions) => Promise<SearchResult | undefined>;
    onNew?: (record: GridRow["record"]) => any;
    onEdit?: (record: GridRow["record"]) => any;
    onDelete?: (record: GridRow["record"]) => any;
    onView?: (record: GridRow["record"]) => any;
    onUpdate?: (record: GridRow["record"]) => void;
    onSave?: (record: GridRow["record"]) => void;
    onDiscard?: (record: GridRow["record"]) => void;
  }
>(function Grid(props, ref) {
  const {
    view,
    expandable,
    expandableView,
    fields,
    perms,
    searchOptions,
    searchAtom,
    actionExecutor,
    showAsTree,
    showNewIcon,
    showEditIcon = true,
    showDeleteIcon,
    editable = false,
    readonly,
    columnAttrs,
    columnFormatter,
    records,
    state,
    setState,
    onFormInit,
    onSearch,
    onNew,
    onEdit,
    onDelete,
    onUpdate,
    onView,
    onSave,
    onDiscard,
    className,
    ...gridProps
  } = props;

  const { context: viewContext } = useViewAction();

  const formRef = useRef<GridFormHandler>(null);
  const [event, setEvent] = useState("");
  const { isMobile } = useDevice();
  const { data: user } = useSession();
  const allowCheckboxSelection =
    (view.selector ?? user?.view?.grid?.selection ?? "checkbox") === "checkbox";
  const hasClientSideSort = (gridProps?.sortType || "state") === "state";

  const { commit: commitTreeForm } = useCollectionTreeEditable();

  const names = useGridColumnNames({
    view,
    fields,
  });
  const sortHandler = useGridSortHandler(fields);

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
          ? selectAtom(searchAtom, (o) => o.appliedContextField)
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

      let widget;
      if (item.type === "field") {
        widget = getWidget(item, field);
      }

      if (view.sortable === false) {
        columnProps.sortable = (item as Field).sortable === true;
      }

      if (item.width) {
        columnProps.width = parseInt(item.width as string);
        columnProps.computed = true;
      }

      if (item.type === "button" || item.widget === "icon") {
        columnProps.sortable = false;
        columnProps.searchable = false;
        columnProps.editable = false;
        columnProps.computed = true;
        columnProps.width = columnProps.width || 40;
        columnProps.action = true;
      }

      const isCollection = ["one-to-many", "many-to-many"].includes(
        toKebabCase(field?.type ?? ""),
      );
      const jsonField = (item as unknown as JsonField).jsonField;
      const searchable =
        jsonField ||
        (field && // check dummy
          !field.transient &&
          !field.json &&
          !field.encrypted);

      if (!searchable || (isCollection && !field?.targetName)) {
        columnProps.searchable = false;
      }

      if ((!searchable || isCollection) && !hasClientSideSort) {
        columnProps.sortable = false;
      }

      if (
        ["DECIMAL", "INTEGER", "LONG"].includes(serverType ?? "") &&
        !(item as Field).selection
      ) {
        columnProps.$css = clsx(styles.number);
        columnProps.$headerCss = clsx(styles.numberHeaderColumn);
      }

      if (
        serverType === "TEXT" ||
        ["html"].includes(item.widget?.toLowerCase() ?? "")
      ) {
        columnProps.$css = clsx(styles["multi-line-text"]);
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
        ...(item.type === "field" && { serverType }),
        title,
        formatter: columnFormatter || formatter,
        valueGetter: columnValueGetter,
        ...columnProps,
        ...extraAttrs,
        ...(widget && { widget }),
      } as any;
    });

    if (showNewIcon) {
      columns.push({
        title: "",
        name: "$$new",
        widget: "new-icon",
        computed: true,
        editable: false,
        sortable: false,
        searchable: false,
        width: 40,
      } as GridColumn);
    }

    if (showEditIcon && view.editIcon !== false) {
      const editColumn = {
        title: "",
        name: "$$edit",
        widget: "edit-icon",
        computed: true,
        editable: false,
        sortable: false,
        searchable: false,
        width: 40,
      } as GridColumn;
      showAsTree ? columns.push(editColumn) : columns.unshift(editColumn);
    }

    if (showDeleteIcon) {
      columns.push({
        title: "",
        name: "$$delete",
        widget: "delete-icon",
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
    hasClientSideSort,
    showAsTree,
    showEditIcon,
    showNewIcon,
    showDeleteIcon,
    fields,
    columnFormatter,
    columnAttrs,
    contextField,
  ]);

  const model = view.model ?? (view as unknown as Property)?.target ?? "";

  // cache expadable form in advance
  const { data: expandViewMeta } = useAsync(async () => {
    if (expandable) {
      return expandableView && typeof expandableView === "object"
        ? expandableView
        : await findView<FormView>({
            type: "form",
            name: expandableView,
            model,
          });
    }
  }, [expandable, model, fields, expandableView]);

  const init = useAsync(async () => {
    onSearch?.({ ...searchOptions, fields: names });
  }, [onSearch, searchOptions, names]);

  const handleCellClick = useCallback(
    (
      e: React.SyntheticEvent,
      col: GridColumn,
      colIndex: number,
      row: GridRow,
    ) => {
      if (col.name === "$$new") {
        onNew?.(row.record);
      } else if (col.name === "$$edit") {
        onEdit?.(row.record);
      } else if (col.name === "$$delete") {
        onDelete?.([row.record]);
      } else if (isMobile) {
        onView?.(row.record);
      }
    },
    [isMobile, onNew, onEdit, onView, onDelete],
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

    if (commitTreeForm) {
      await commitTreeForm();
    }
  }, [commitTreeForm]);

  const doAdd = useCallback(async () => {
    const newRecord = {
      id: nextId(),
      ...getDefaultValues(fields, view.items),
    };
    const newRecords = [...(records || []), newRecord];
    setState?.((draft) => {
      const { rows, columns, orderBy, groupBy } = draft;
      const _rows = draft.rows;
      const newRows: GridRow[] = getRows({
        rows,
        columns,
        orderBy,
        groupBy,
        records: newRecords,
        sortFn: sortHandler,
      });

      draft.rows = newRows.map((row) => {
        const oldRow = _rows.find((r) => r.key === row.key);
        return oldRow ? { ...row, expand: oldRow.expand } : row;
      });
      draft.selectedCell = null;
      draft.selectedRows = null;
      draft.editRow = [
        newRows.findIndex((r) => r?.record?.id === newRecord.id),
        null,
      ];
    });
  }, [fields, records, setState, view.items, sortHandler]);

  const isPermitted = usePermitted(model, perms);

  const handleRecordAdd = useCallback(async () => {
    setEvent("editable:add-new");
    return true;
  }, []);

  const onRecordAdd = useCallback(async () => {
    await commitForm();
    return handleRecordAdd();
  }, [commitForm, handleRecordAdd]);

  const handleRecordEdit = useCallback(
    async (row: GridRow, rowIndex?: number, column?: GridColumn) => {
      // Skip edit row for edit icon and check write permission
      if (
        ["icon"].includes((column as Field)?.widget ?? "") ||
        ["button", "row-expand"].includes(column?.type ?? "") ||
        ["$$new", "$$edit", "$$delete"].includes(column?.name ?? "") ||
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
          draft.selectedCell = null;
        });
      } else {
        setState?.((draft) => {
          draft.selectedCell = null;
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
        viewContext={viewContext}
        actionExecutor={actionExecutor}
      />
    ),
    [view, actionExecutor, onUpdate, viewContext],
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

  const detailsProps = useMemo(
    () => ({
      rowDetailsExpandIcon: ExpandIcon,
    }),
    [],
  );

  const RowDetailsRenderer = useMemo(() => {
    return ({
      data,
      onClose,
    }: GridRowProps & {
      onClose?: () => void;
    }) =>
      expandViewMeta ? (
        <ExpandableFormView
          gridView={view}
          meta={expandViewMeta}
          record={data.record!}
          onUpdate={onUpdate}
          onSave={onSave}
          onDiscard={handleRecordDiscard}
          onClose={onClose}
        />
      ) : null;
    // eslint-disable-next-line
  }, [view, onSave, onUpdate, expandViewMeta]);

  useImperativeHandle(
    ref,
    () => ({
      form: formRef,
      onAdd: onRecordAdd,
      commit: commitForm,
    }),
    [formRef, onRecordAdd, commitForm],
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

  const canMove = useMemo(() => {
    if (readonly) return false;

    const { canMove, orderBy } = view ?? {};
    if (canMove !== true) return false;

    // On top-level grid, orderBy is required for canMove
    const orderField = orderBy?.split(/\s*,\s*/)?.[0];
    if (!orderField) return false;

    const field = fields?.[orderField];
    return field && isValidSequence(field);
  }, [readonly, view, fields]);

  const gridContext = useMemo(
    () => ({ readonly: !editable && readonly }),
    [editable, readonly],
  );

  if (init.state === "loading") return null;

  return (
    <AxGridProvider>
      <GridContext.Provider value={gridContext}>
        <AxGrid
          labels={getLabels()}
          cellRenderer={CustomCellRenderer}
          rowRenderer={CustomRowRenderer}
          allowColumnResize
          allowGrouping={!canMove}
          allowSorting={!canMove}
          allowSelection
          allowCellSelection
          allowColumnHide
          allowColumnOptions
          allowColumnCustomize
          allowCheckboxSelection={allowCheckboxSelection}
          allowRowReorder={canMove}
          allowRowExpand={expandable}
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
          {...(expandable && {
            rowDetailsRenderer: RowDetailsRenderer,
            ...detailsProps,
          })}
          onCellClick={handleCellClick}
          onRowDoubleClick={handleRowDoubleClick}
          sortHandler={sortHandler}
          state={state!}
          setState={setState!}
          records={records!}
          rowHeight={Math.max(view.rowHeight ?? 35, 35)}
          {...gridProps}
          columns={columns}
          className={clsx(className, styles.grid)}
        />
      </GridContext.Provider>
    </AxGridProvider>
  );
});
