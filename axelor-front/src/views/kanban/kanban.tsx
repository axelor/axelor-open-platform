import { Box, Panel, Popper, clsx } from "@axelor/ui";
import { useAtom, useSetAtom } from "jotai";
import { atomWithImmer } from "jotai-immer";
import { useAtomCallback } from "jotai/utils";
import { uniq } from "lodash";
import getValue from "lodash/get";
import isObject from "lodash/isObject";
import setValue from "lodash/set";
import {
  FunctionComponent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { dialogs } from "@/components/dialogs";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useMediaQuery } from "@/hooks/use-media-query";
import { useTemplate } from "@/hooks/use-parser";
import { EvalContextOptions } from "@/hooks/use-parser/context";
import { isLegacyTemplate } from "@/hooks/use-parser/utils";
import { usePerms } from "@/hooks/use-perms";
import { useManyEditor } from "@/hooks/use-relation";
import { useShortcuts } from "@/hooks/use-shortcut";
import { SearchOptions } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { MetaData } from "@/services/client/meta";
import { KanbanView, Property } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { DEFAULT_KANBAN_PAGE_SIZE } from "@/utils/app-settings.ts";
import { AdvanceSearch } from "@/view-containers/advance-search";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  useViewContext,
  useViewSwitch,
  useViewTab,
  useViewTabRefresh,
} from "@/view-containers/views/scope";
import { useActionExecutor } from "../form/builder/scope";
import { isValidSequence } from "../grid/builder/utils";
import { ViewProps } from "../types";

import { CardTemplate } from "../cards/card-template";
import { useCardClassName } from "../cards/use-card-classname";
import { KanbanBoard } from "./kanban-board";
import { KanbanColumn, KanbanRecord } from "./types";
import {
  getColumnIndex,
  getColumnRecords,
  getRecordIndex,
  reorderCards,
} from "./utils";

import styles from "./kanban.module.scss";

export function Kanban(props: ViewProps<KanbanView>) {
  const { meta, dataStore, searchAtom } = props;
  const { view, fields } = meta;
  const { action, dashlet, popup, popupOptions } = useViewTab();
  const [columns, setColumns] = useAtom(
    useMemo(() => atomWithImmer<KanbanColumn[]>([]), []),
  );

  const columnsRef = useRef(columns);

  useEffect(() => {
    columnsRef.current = columns;
  }, [columns]);

  const { hasButton } = usePerms(meta.view, meta.perms);
  const switchTo = useViewSwitch();

  const { params } = action;
  const {
    columnBy,
    editWindow,
    limit = +params?.limit || DEFAULT_KANBAN_PAGE_SIZE,
    sequenceBy,
  } = view;
  const hasEditPopup = dashlet || editWindow === "popup";
  const hasAddPopup = hasEditPopup || editWindow === "popup-new";
  const hideCols = params?.["kanban-hide-columns"] || "";
  const colWidth = params?.["kanban-column-width"];

  const showEditor = useManyEditor(action, dashlet);

  const getViewContext = useViewContext();
  const getContext = useCallback(
    () =>
      ({
        ...getViewContext(true),
        _model: action.model,
      }) as DataContext,
    [action.model, getViewContext],
  );

  const getActionContext = useCallback(() => {
    return {
      ...getContext(),
      _viewName: action.name,
      _viewType: action.viewType,
      _views: action.views,
    };
  }, [action.name, action.viewType, action.views, getContext]);

  const getColumnByValue = useCallback(
    (value: any) => {
      const field = fields?.[columnBy ?? ""];
      return isObject(value) ? value : field?.target ? { id: value } : value;
    },
    [fields, columnBy],
  );

  const $columns = useMemo(() => {
    const collapseCols = view.collapseColumns?.split(/\s*,\s*/);
    return (view.columns || fields?.[columnBy ?? ""]?.selectionList || []).map(
      (_column, ind) => {
        const { title, value: name } = _column;
        const column: KanbanColumn = {
          id: name!,
          title: title!,
          name: name!,
          dataStore: new DataStore(dataStore.model, dataStore.options),
        };

        if (view.onNew && ind === 0) {
          column.canCreate = true;
        }
        if (collapseCols?.includes(column.name)) {
          column.collapsed = true;
        }
        return column;
      },
    );
  }, [view, dataStore, columnBy, fields]);

  const fetchRecords = useAtomCallback(
    useCallback(
      (
        get,
        set,
        column: KanbanColumn,
        options: Partial<SearchOptions> = {},
      ) => {
        const { query = null } = searchAtom ? get(searchAtom) : {};
        const names = Object.keys(fields ?? {});

        let filter: SearchOptions["filter"] = {
          criteria: [
            {
              fieldName: columnBy,
              operator: "=",
              value: getColumnByValue(column.name),
            },
          ],
          operator: "and",
        };

        const { criteria, operator, ...filterQuery } = query || {};

        if (criteria?.length) {
          filter = {
            ...filterQuery,
            operator: "and",
            criteria: [{ criteria, operator }, filter],
          };
        } else {
          filter = { ...filter, ...filterQuery };
        }

        if (dashlet) {
          const { _domainAction, ...formContext } = getViewContext() ?? {};
          filter._domainContext = {
            ...filter?._domainContext,
            ...formContext,
          };
          filter._domainAction = _domainAction;
        }
        return column.dataStore.search({
          ...(sequenceBy && { sortBy: [sequenceBy] }),
          filter,
          limit,
          fields: uniq(
            [...names, columnBy, sequenceBy].filter((name) => name),
          ) as string[],
          ...options,
        });
      },
      [
        dashlet,
        fields,
        searchAtom,
        limit,
        columnBy,
        sequenceBy,
        getViewContext,
        getColumnByValue,
      ],
    ),
  );

  const onSearch = useCallback(
    (
      options: Partial<SearchOptions> = {},
      searchColumnsList?: string[],
    ): any => {
      const hideColumns = hideCols.split(/\s*,\s*/);

      let columns = columnsRef.current;
      if (!columns.length) {
        columns = ($columns || [])
          .filter((item) => !hideColumns.includes(item.name))
          .map((column) => ({
            ...column,
            loading: true,
            records: [],
          }));
      } else {
        columns = columns.map((c) =>
          c.collapsed
            ? {
                ...c,
                loading: true,
                ...(searchColumnsList?.includes(c.name) && {
                  collapsed: false,
                }),
              }
            : c,
        );
      }

      setColumns(columns);

      columns.forEach((column: KanbanColumn, index: number) => {
        if (
          (column.collapsed && !searchColumnsList) ||
          (searchColumnsList && !searchColumnsList.includes(column.name!))
        ) {
          return;
        }
        fetchRecords(column, options).then(({ records }) => {
          setColumns((draft) => {
            const column = draft[index];
            if (column) {
              column.loading = false;
              column.records = records as KanbanRecord[];
            }
          });
        });
      });
    },
    [$columns, hideCols, fetchRecords, setColumns],
  );

  const onRefresh = useCallback(() => {
    return onSearch({ offset: 0 });
  }, [onSearch]);

  const onCollapse = useCallback(
    (column: KanbanColumn) => {
      setColumns((draft) => {
        const col = draft.find((c) => c.name === column.name);
        col && (col.collapsed = !col.collapsed);
      });
      column.loading && onSearch({ offset: 0 }, [column.name!]);
    },
    [setColumns, onSearch],
  );

  const onLoadMore = useCallback(
    ({ column }: { column: KanbanColumn }) => {
      const { name, records } = column;
      fetchRecords(column, { offset: records?.length ?? 0 }).then(
        ({ records }) => {
          setColumns((columns) => {
            const columnIndex = columns.findIndex((c) => c.name === name);
            if (columns[columnIndex]) {
              columns[columnIndex].records?.push(
                ...(records as KanbanRecord[]),
              );
            }
          });
        },
      );
    },
    [fetchRecords, setColumns],
  );

  const onDelete = useCallback(
    async ({
      record,
      column,
    }: {
      record: KanbanRecord;
      column: KanbanColumn;
    }) => {
      const confirmed = await dialogs.confirm({
        content: i18n.get(
          "Do you really want to delete the selected record(s)?",
        ),
        yesTitle: i18n.get("Delete"),
      });
      if (confirmed) {
        const { id, version } = record as DataRecord;
        try {
          const removed = await column.dataStore.delete([
            { id: id!, version: version! },
          ]);
          removed &&
            setColumns((columns) => {
              const state = columns.find((c) => c.name === column.name);
              state &&
                (state.records = state.records?.filter((r) => r.id !== id));
            });
        } catch {
          // Ignore
        }
      }
    },
    [setColumns],
  );

  const onEdit = useCallback(
    (
      { record, column }: { record: KanbanRecord; column?: KanbanColumn },
      readonly = false,
    ) => {
      const recordId = (record.id || 0) as number;
      const id = recordId > 0 ? String(recordId) : "";
      switchTo("form", {
        route: { id },
        props: {
          readonly,
          recordId: id,
          ...(column && { dataStore: column.dataStore }),
        },
      });
    },
    [switchTo],
  );

  const onEditInPopup = useCallback(
    (
      { record }: { record: KanbanRecord; column?: KanbanColumn },
      readonly = false,
    ) => {
      const viewName = action.views?.find((v) => v.type === "form")?.name;
      const { title, model } = view;
      model &&
        showEditor({
          title: title ?? "",
          model,
          viewName,
          record: record as DataRecord,
          readonly,
          onSearch: () => onRefresh(),
        });
    },
    [showEditor, view, action, onRefresh],
  );

  const onNew = useCallback(() => {
    hasAddPopup
      ? onEditInPopup({ record: {} as KanbanRecord })
      : onEdit({ record: {} as KanbanRecord });
  }, [hasAddPopup, onEdit, onEditInPopup]);

  const actionExecutor = useActionExecutor(view, {
    getContext: getActionContext,
    onRefresh,
  });

  const onCreate = useCallback(
    async ({
      record,
      column,
    }: {
      record: KanbanRecord;
      column: KanbanColumn;
    }) => {
      if (view.onNew && record && column) {
        const res = await actionExecutor.execute(view.onNew, {
          data: {
            _domainContext: {
              ...getContext(),
              _value: (record as DataRecord).text,
            },
          },
        });
        const values = res?.reduce?.(
          (obj, { values }) => ({
            ...obj,
            ...values,
          }),
          {},
        );
        if (values) {
          const record = {
            ...values,
            [columnBy!]: getColumnByValue(column.name),
          };
          const saved = await column.dataStore.save(record);
          saved &&
            setColumns((columns) => {
              const state = columns.find((c) => c.name === column.name);
              state &&
                (state.records = [
                  saved as KanbanRecord,
                  ...(state.records || []),
                ]);
            });
        }
      }
    },
    [getContext, getColumnByValue, actionExecutor, setColumns, columnBy, view],
  );

  const onView = useCallback(
    ({ record, column }: { record: KanbanRecord; column?: KanbanColumn }) => {
      hasEditPopup
        ? onEditInPopup({ record, column }, true)
        : onEdit({ record, column }, true);
    },
    [hasEditPopup, onEdit, onEditInPopup],
  );

  const onMove = useCallback(
    async ({
      column,
      index,
      source,
      record,
    }: {
      column: KanbanColumn;
      index: number;
      source: KanbanColumn;
      record: KanbanRecord;
    }) => {
      function getRecord(
        _record: DataRecord,
        columnByValue: any,
        sequenceByValue: any,
      ) {
        const { id, version } = _record;
        const record: any = { id, version };
        const columnByJSONField = fields?.[columnBy ?? ""]?.jsonField;
        const sequenceByJSONField = fields?.[sequenceBy ?? ""]?.jsonField;

        if (columnByJSONField) {
          record[columnByJSONField] = _record[columnByJSONField];
        }
        if (sequenceByJSONField) {
          record[sequenceByJSONField] = _record[sequenceByJSONField];
        }

        columnBy && setValue(record, columnBy, getColumnByValue(columnByValue));
        sequenceBy && setValue(record, sequenceBy, sequenceByValue);

        return record;
      }

      const updatedColumns = reorderCards({
        columns,
        destinationColumn: column,
        destinationIndex: index,
        sourceColumn: source,
        sourceIndex: getRecordIndex(
          record.id,
          getColumnRecords(columns, source.name),
        ),
      }).slice();

      setColumns(updatedColumns);

      const records = [
        ...(updatedColumns[getColumnIndex(updatedColumns, column.name)]
          ?.records || []),
      ];
      const previousRecord = records[index - 1];
      const updatedRecord = getRecord(
        records[index] as DataRecord,
        column.name,
        Number(
          previousRecord
            ? parseInt(getValue(previousRecord, sequenceBy!) + 1)
            : 0,
        ),
      );

      if (view.onMove) {
        try {
          const ctx = {
            ...getContext(),
            ...updatedRecord,
          };
          await actionExecutor.execute(view.onMove, {
            context: ctx,
            data: {
              _domainContext: ctx,
            },
          });
        } catch {
          // reset columns to last state
          return setColumns(
            columns.map((col) => ({
              ...col,
              records: [...(col.records || [])],
            })),
          );
        }
      }

      const updatedRecords = [
        updatedRecord,
        ...records.slice(index + 1, records.length).map((record, i) => {
          const $sequenceBy = getValue(updatedRecord, sequenceBy!);
          return getRecord(
            record as DataRecord,
            getValue(record, columnBy!),
            $sequenceBy === null ? null : parseInt($sequenceBy) + i + 1,
          );
        }),
      ];

      try {
        const res = await column.dataStore.save(updatedRecords);

        records.splice(index, res.length, ...(res as KanbanRecord[]));

        const colInd = getColumnIndex(updatedColumns, column.name);

        /**
         * Setting same reference with mutation of records with updated sequence and versioning
         */
        setColumns(
          updatedColumns.map((col, ind) =>
            ind === colInd ? { ...col, records: [...records] } : col,
          ),
        );
      } catch {
        onRefresh();
      }
    },
    [
      view,
      columnBy,
      sequenceBy,
      columns,
      actionExecutor,
      fields,
      getColumnByValue,
      getContext,
      setColumns,
      onRefresh,
    ],
  );

  const setPopupHandlers = useSetAtom(usePopupHandlerAtom());
  const setDashletHandlers = useSetAtom(useDashletHandlerAtom());

  useEffect(() => {
    if (popup) {
      setPopupHandlers({
        dataStore: dataStore,
        onSearch: onRefresh,
      });
    }
  }, [onRefresh, popup, dataStore, setPopupHandlers]);

  useEffect(() => {
    if (dashlet) {
      setDashletHandlers({
        dataStore,
        view,
        actionExecutor,
        onRefresh: () => onRefresh(),
      });
    }
  }, [dashlet, view, dataStore, actionExecutor, onRefresh, setDashletHandlers]);

  useAsyncEffect(async () => {
    await onSearch({ offset: 0 });
  }, [onSearch]);

  // register tab:refresh
  useViewTabRefresh("kanban", onRefresh);

  const showToolbar = popupOptions?.showToolbar !== false;
  const small = useMediaQuery("(max-width: 768px)");

  const hasValidSequenceByField = useMemo(() => {
    const $sequenceBy = (fields?.[sequenceBy ?? ""] || {}) as Property;
    return isValidSequence($sequenceBy);
  }, [fields, sequenceBy]);

  const Template = useTemplate(view.template!);

  const components = useMemo(
    () => ({
      Card: ({ record }: { record: KanbanRecord }) => (
        <KanbanCard
          view={view}
          fields={fields}
          record={record}
          onRefresh={onRefresh}
          Template={Template}
        />
      ),
    }),
    [view, fields, Template, onRefresh],
  );

  const canNew = hasButton("new");

  useShortcuts({
    viewType: view.type,
    onNew: canNew ? onNew : undefined,
    onRefresh: onRefresh,
  });

  return (
    <Box
      className={legacyClassNames(styles.kanban, "kanban-view", "row-fluid")}
    >
      {showToolbar && (
        <ViewToolBar
          meta={meta}
          actionExecutor={actionExecutor}
          actions={[
            {
              key: "new",
              text: i18n.get("New"),
              hidden: !canNew,
              iconProps: {
                icon: "add",
              },
              onClick: onNew,
            },
            {
              key: "refresh",
              text: i18n.get("Refresh"),
              iconProps: {
                icon: "refresh",
              },
              onClick: () => onRefresh(),
            },
          ]}
        >
          {searchAtom && (
            <AdvanceSearch
              stateAtom={searchAtom}
              dataStore={dataStore}
              items={view.items}
              canExport={false}
              customSearch={view.customSearch}
              freeSearch={view.freeSearch}
              onSearch={onRefresh}
            />
          )}
        </ViewToolBar>
      )}
      <Box
        d="flex"
        flexGrow={1}
        overflow={small ? "auto" : "hidden"}
        className={styles["board"]}
      >
        <KanbanBoard
          readonly={view.draggable === false || !hasValidSequenceByField}
          responsive={small}
          columnWidth={colWidth}
          columns={columns}
          components={components as any}
          onCollapse={onCollapse}
          onLoadMore={onLoadMore}
          onCardMove={onMove}
          onCardClick={onView}
          {...(hasButton("delete") && { onCardDelete: onDelete })}
          {...(hasButton("new") && { onCardAdd: onCreate })}
          {...(hasButton("edit") && {
            onCardEdit: hasEditPopup ? onEditInPopup : onEdit,
          })}
          {...({} as any)}
        />
      </Box>
    </Box>
  );
}

function KanbanCard({
  view,
  fields,
  record,
  onRefresh,
  Template,
}: {
  record: KanbanRecord;
  view: KanbanView;
  context?: DataContext;
  fields?: MetaData["fields"];
  onRefresh?: () => Promise<any>;
  Template: FunctionComponent<{
    context: DataContext;
    options?: EvalContextOptions;
  }>;
}) {
  const { template: templateString } = view;
  const divRef = useRef<any>(null);
  const className = useCardClassName(view, record as DataRecord);
  const timer = useRef<any>();
  const [popover, setPopover] = useState(false);
  const [popoverData, setPopoverData] = useState<{ title: ""; body: "" }>({
    title: "",
    body: "",
  });

  function showPopover() {
    const div = divRef.current;
    const summary =
      div &&
      (div.querySelector(".card-summary.popover") ||
        div.querySelector(
          `.${legacyClassNames("card-summary")}.${legacyClassNames("popover")}`,
        ));
    if (summary) {
      const text = (summary.textContent || "").trim();
      if (text) {
        timer.current = setTimeout(() => {
          setPopover(true);
          setPopoverData({
            title: summary.title,
            body: summary.innerHTML,
          });
        }, 500);
      }
    }
  }

  const hidePopover = useCallback(function hidePopover() {
    setPopover(false);
    clearTimeout(timer.current);
  }, []);

  const hasPopover = (templateString || "").includes("popover");
  const isLegacyTmpl = isLegacyTemplate(templateString);

  useEffect(() => {
    return () => hidePopover();
  }, [hidePopover]);

  return (
    <>
      <Box
        ref={divRef}
        {...(hasPopover
          ? {
              onMouseEnter: showPopover,
              onMouseLeave: hidePopover,
              onMouseDown: hidePopover,
            }
          : {})}
        className={clsx(
          isLegacyTmpl ? legacyClassNames("kanban-card") : "",
          styles["kanban-card"],
          className,
        )}
      >
        <CardTemplate
          component={Template}
          fields={fields}
          record={record! as DataRecord}
          onRefresh={onRefresh}
        />
      </Box>

      <Popper
        arrow
        bg={"white" as any}
        placement="end"
        open={popover}
        target={divRef.current}
        offset={[0, 4]}
      >
        <Box
          style={{
            maxWidth: 400,
            minWidth: 200,
          }}
        >
          <Panel header={popoverData.title}>
            <Box>
              {popoverData.body && (
                <div
                  dangerouslySetInnerHTML={{ __html: popoverData.body } as any}
                />
              )}
            </Box>
          </Panel>
        </Box>
      </Popper>
    </>
  );
}
