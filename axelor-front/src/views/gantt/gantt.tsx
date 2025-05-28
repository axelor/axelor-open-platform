import { useCallback, useEffect, useMemo, useState } from "react";

import { Box, DndProvider } from "@axelor/ui";
import {
  ConnectProps,
  GANTT_TYPES,
  Gantt as GanttComponent,
  GanttField,
  GanttRecord,
  GanttType,
  GanttData,
  GanttFieldRenderer,
} from "@axelor/ui/gantt";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { dialogs } from "@/components/dialogs";
import { PageText } from "@/components/page-text";
import { useManyEditor } from "@/hooks/use-relation";
import { SearchOptions } from "@/services/client/data";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { moment } from "@/services/client/l10n";
import { Field, GanttView, Widget } from "@/services/client/meta.types";
import { DEFAULT_PAGE_SIZE } from "@/utils/app-settings.ts";
import format from "@/utils/format";
import { compare } from "@/utils/sort";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { useViewContext, useViewTab, useViewTabRefresh } from "@/view-containers/views/scope";

import { ViewProps } from "../types";
import { formatRecord, getFieldNames, transformRecord } from "./utils";

import styles from "./gantt.module.scss";

const connectSetTypes = {
  start_finish: "startToFinish",
  finish_finish: "finishToFinish",
  start_start: "startToStart",
  finish_start: "finishToStart",
} as const;

function TreeCell({
  value,
  data,
  onExpand,
}: {
  value: any;
  data: GanttData;
  field: GanttField;
  onExpand?: (record: GanttData, expand?: boolean) => void;
}) {
  const { _level: level, _children: children, _expand: expand = true } = data;
  const hasChildren = Boolean(children?.length);
  return (
    <Box
      d="flex"
      alignItems="center"
      className={styles.cell}
      gap={4}
      {...(level && {
        style: {
          paddingLeft: `${(level - 1) * 0.75}rem`,
        },
      })}
      {...(hasChildren && {
        fontWeight: "bold",
      })}
    >
      <Box d="flex">
        {hasChildren ? (
          <MaterialIcon
            className={styles.icon}
            icon={expand ? "arrow_drop_down" : "arrow_right"}
            onClick={() => onExpand?.(data, !expand)}
          />
        ) : (
          <Box style={{ width: 20 }} />
        )}
        <MaterialIcon
          className={styles.icon}
          icon={hasChildren ? "folder_open" : "description"}
          fontSize={hasChildren ? "1.5rem" : "1.4rem"}
        />
      </Box>
      <Box as="span">{value}</Box>
    </Box>
  );
}

function ActionsCell({
  data,
  onAdd,
  onRemove,
}: {
  data: GanttData;
  onAdd?: (record: DataRecord) => void;
  onRemove?: (record: DataRecord) => void;
}) {
  return (
    <Box d="flex" className={styles.cell}>
      <MaterialIcon
        className={styles.icon}
        icon="add"
        fontSize={"1.3rem"}
        onClick={() => onAdd?.(data)}
      />
      <MaterialIcon
        className={styles.icon}
        icon="close"
        fontSize={"1.3rem"}
        onClick={() => onRemove?.(data)}
      />
    </Box>
  );
}

function fieldFormatter(column: Field, value: any, record: any) {
  return format(value, {
    props: column,
    context: record,
  });
}

export function Gantt({ dataStore, meta }: ViewProps<GanttView>) {
  const [type, setType] = useState<GanttType>(GANTT_TYPES.WEEK);
  const [records, setRecords] = useState<DataRecord[]>([]);
  const { action, dashlet } = useViewTab();
  const showEditor = useManyEditor(action, dashlet);
  const getViewContext = useViewContext();

  const { fields, view } = meta;
  const { items } = view;
  const { domain, context } = action;

  const fieldNames = useMemo(() => getFieldNames(view), [view]);

  const { formatter, transformer } = useMemo(
    () => ({
      formatter: (record: DataRecord) => formatRecord(view, record),
      transformer: (record: GanttRecord) => transformRecord(view, record),
    }),
    [view],
  );

  const handleExpand = useCallback(
    (record: GanttData, _expand?: boolean) =>
      setRecords((prevRecords) =>
        prevRecords.map((r) => (r.id === record.id ? { ...r, _expand } : r)),
      ),
    [],
  );

  const onSearch = useCallback(
    async (options?: SearchOptions) => {
      const { records: recs } = await dataStore.search({
        ...options,
        fields: fieldNames,
        filter: {
          _domain: domain || undefined,
          _domainContext: context,
        },
      });
      setRecords(recs);
    },
    [dataStore, fieldNames, domain, context],
  );

  const updateRecord = useCallback(
    async (data: DataRecord) => {
      const updated = await dataStore.save(data);
      if (updated) {
        setRecords((prevRecords) =>
          prevRecords.map((r) => (r.id === data.id ? { ...r, ...updated } : r)),
        );
      }
    },
    [dataStore],
  );

  const handleRecordConnect = useCallback(
    ({ startId, finishId, source, target }: ConnectProps) => {
      const set = connectSetTypes[`${source}_${target}`];
      const record = records.find((r) => r.id === finishId);
      if (record) {
        const $set: DataRecord[] = record[set] || [];
        if (!$set?.find((obj) => String(obj.id) === String(startId))) {
          return updateRecord({
            ...record,
            [set]: [...$set, { id: startId }],
          });
        }
      }
    },
    [records, updateRecord],
  );

  const handleRecordDisconnect = useCallback(
    ({ startId, finishId, source, target }: ConnectProps) => {
      const set = connectSetTypes[`${source}_${target}`];
      const record = records.find((r) => r.id === finishId);
      if (record) {
        const $set: DataRecord[] = record[set] || [];
        return updateRecord({
          ...record,
          [set]: $set?.filter((obj) => String(obj.id) !== String(startId)),
        });
      }
    },
    [records, updateRecord],
  );

  const handleRecordUpdate = useCallback(
    (record: GanttData, changes: Partial<GanttRecord>) =>
      updateRecord({
        id: record.id,
        version: record.version,
        ...transformer(changes as GanttRecord),
      }),
    [transformer, updateRecord],
  );

  const handleRecordRemove = useCallback(
    async (record: DataRecord) => {
      const confirmed = await dialogs.confirm({
        content: i18n.get("Do you really want to delete the selected task?"),
        yesTitle: i18n.get("Delete"),
      });
      if (confirmed) {
        const { id, version } = record;
        if (id) {
          const res = await dataStore.delete({ id, version: version ?? 0 });
          if (res) {
            setRecords((prevRecords) => prevRecords.filter((r) => r.id !== id));
          }
        }
      }
    },
    [dataStore],
  );

  const showRecordEditor = useCallback(
    (record: GanttData) => {
      const { model, title } = view;
      if (model) {
        const isNew = !record.id;
        showEditor({
          title: title ?? "",
          model,
          record,
          viewName: action.views?.find((v) => v.type === "form")?.name,
          readonly: false,
          context: getViewContext(true),
          onSelect: (_record: DataRecord) => {
            setRecords((_records) => {
              return isNew
                ? [..._records, _record]
                : _records.map((r) =>
                    r.id === _record.id ? { ...r, ..._record } : r,
                  );
            });
          },
        });
      }
    },
    [view, showEditor, getViewContext, action.views],
  );

  const handleRecordAddSubTask = useCallback(
    async (record?: DataRecord) => {
      const { taskParent, taskDuration, taskProgress, taskStart } = view;
      showRecordEditor({
        ...(taskStart && {
          [taskStart]: moment().format("YYYY-MM-DDTHH:mm:ss[Z]"),
        }),
        ...(taskDuration && { [taskDuration]: 1 }),
        ...(taskProgress && { [taskProgress]: 0 }),
        ...(taskParent &&
          record && {
            [taskParent]: {
              id: record.id,
              version: record.version,
              name: record.name,
            },
          }),
      } as GanttData);
    },
    [view, showRecordEditor],
  );

  useEffect(() => {
    onSearch();
  }, [onSearch]);

  useEffect(() => {
    return dataStore.subscribe(() => {
      setRecords(dataStore.records);
    });
  }, [dataStore]);

  const { page } = dataStore;
  const { offset = 0, limit = DEFAULT_PAGE_SIZE, totalCount = 0 } = page;

  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;

  const ganttItems = useMemo(
    () =>
      (items || [])
        .concat(
          view.taskUser
            ? [{ type: "field", name: view.taskUser } as Widget]
            : [],
        )
        .map((item, ind) => ({
          ...(ind === 0 && {
            width: 160,
            renderer: (props: GanttFieldRenderer) => (
              <TreeCell
                value={props.value}
                data={props.data}
                field={props.field}
                onExpand={handleExpand}
              />
            ),
          }),
          ...item,
          ...fields?.[item.name!],
          formatter: fieldFormatter,
        }))
        .concat([
          {
            title: (
              <Box d="flex">
                <MaterialIcon
                  className={styles.icon}
                  icon="add"
                  onClick={() => handleRecordAddSubTask()}
                />
              </Box>
            ),
            name: "$actions",
            renderer: (props: GanttFieldRenderer) => (
              <ActionsCell
                data={props.data}
                onAdd={handleRecordAddSubTask}
                onRemove={handleRecordRemove}
              />
            ),
            width: 60,
          } as any,
        ]) as unknown as GanttField[],
    [
      view,
      items,
      fields,
      handleExpand,
      handleRecordAddSubTask,
      handleRecordRemove,
    ],
  );

  const ganttRecords = useMemo(() => {
    const { taskStart = "", taskSequence, taskParent = "" } = view;
    const getParent = (task: DataRecord) => task[taskParent];

    function collect(
      parent?: DataRecord["id"],
      level: number = 1,
    ): DataRecord[] {
      let dataset = records.filter(
        (item) =>
          item[taskStart] &&
          (parent
            ? (getParent(item) || {}).id === parent
            : getParent(item) === parent),
      );

      if (taskSequence) {
        dataset = dataset.sort((x1, x2) =>
          compare(x1[taskSequence], x2[taskSequence]),
        );
      }

      return dataset.reduce((list, _item) => {
        const item: DataRecord = { ..._item, _level: level };
        const subTasks = collect(item.id, level + 1);

        return list.concat([
          subTasks.length
            ? { ...item, _children: subTasks.map((t) => t.id) }
            : item,
          ...(item._expand === false ? [] : subTasks),
        ]);
      }, []) as DataRecord[];
    }

    const list = collect(null);
    return list.map(formatter) as GanttRecord[];
  }, [view, formatter, records]);

  // register tab:refresh
  useViewTabRefresh("gantt", onSearch);

  const actions = useMemo(() => {
    return [
      {
        key: GANTT_TYPES.YEAR,
        text: i18n.get("Year"),
        iconOnly: false,
        checked: type === GANTT_TYPES.YEAR,
        onClick: () => setType(GANTT_TYPES.YEAR),
      },
      {
        key: GANTT_TYPES.MONTH,
        text: i18n.get("Month"),
        iconOnly: false,
        checked: type === GANTT_TYPES.MONTH,
        onClick: () => setType(GANTT_TYPES.MONTH),
      },
      {
        key: GANTT_TYPES.WEEK,
        text: i18n.get("Week"),
        iconOnly: false,
        checked: type === GANTT_TYPES.WEEK,
        onClick: () => setType(GANTT_TYPES.WEEK),
      },
      {
        key: GANTT_TYPES.DAY,
        text: i18n.get("Day"),
        iconOnly: false,
        checked: type === GANTT_TYPES.DAY,
        onClick: () => setType(GANTT_TYPES.DAY),
      },
      {
        key: "d1",
        divider: true,
      },
      {
        key: "refresh",
        text: i18n.get("Refresh"),
        iconOnly: false,
        onClick: () => onSearch(),
      },
    ];
  }, [onSearch, type]);

  const config = useMemo(
    () => ({
      progress: Boolean(view.taskProgress),
      duration: Boolean(view.taskDuration),
      startDate: Boolean(view.taskStart),
      endDate: Boolean(view.taskEnd),
    }),
    [view],
  );

  return (
    <Box d="flex" className={styles.container} flex={1} flexDirection="column">
      <ViewToolBar
        meta={meta}
        actions={actions}
        pagination={{
          canPrev,
          canNext,
          onPrev: () => onSearch({ offset: offset - limit }),
          onNext: () => onSearch({ offset: offset + limit }),
          text: () => <PageText dataStore={dataStore} />,
        }}
      />
      <Box d="flex" flex={1} overflow="auto" px={1}>
        <DndProvider>
          <GanttComponent
            className={styles.gantt}
            view={type}
            config={config}
            items={ganttItems}
            records={ganttRecords}
            onRecordView={showRecordEditor}
            onRecordConnect={handleRecordConnect}
            onRecordDisconnect={handleRecordDisconnect}
            onRecordUpdate={handleRecordUpdate}
          />
        </DndProvider>
      </Box>
    </Box>
  );
}
