import {
  Gantt as GanttComponent,
  GANTT_TYPES,
  GanttType,
  GanttField,
  GanttRecord,
  ConnectProps,
} from "@axelor/ui/gantt";
import { Box, Button } from "@axelor/ui";
import { GridProvider as DNDProvider } from "@axelor/ui/grid";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { Field, GanttView, Widget } from "@/services/client/meta.types";
import { ViewProps } from "../types";
import { useCallback, useEffect, useMemo, useState } from "react";
import { DataRecord } from "@/services/client/data.types";
import { formatRecord, transformRecord } from "./utils";
import { useViewTab } from "@/view-containers/views/scope";
import { i18n } from "@/services/client/i18n";
import { PageText } from "@/components/page-text";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import { SearchOptions } from "@/services/client/data";
import format from "@/utils/format";
import styles from "./gantt.module.scss";

function fieldFormatter(column: Field, value: any, record: any) {
  return format(value, {
    props: column,
    context: record,
  });
}

const FILTERS: { key: GanttType; title: string }[] = [
  { key: GANTT_TYPES.YEAR, title: i18n.get("Year") },
  { key: GANTT_TYPES.MONTH, title: i18n.get("Month") },
  { key: GANTT_TYPES.WEEK, title: i18n.get("Week") },
  { key: GANTT_TYPES.DAY, title: i18n.get("Day") },
];

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
  data: GanttRecord & {
    _level?: number;
    _expand?: boolean;
  };
  field: GanttField;
  onExpand?: (record: DataRecord, expand?: boolean) => void;
}) {
  const { _level: level, _children: children, _expand: expand = true } = data;
  const isRoot = level === 1;
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
      {...(isRoot && {
        fontWeight: "bold",
      })}
    >
      <Box d="flex">
        {hasChildren ? (
          <MaterialIcon
            icon={expand ? "arrow_drop_down" : "arrow_right"}
            onClick={() => onExpand?.(data, !expand)}
          />
        ) : (
          <Box style={{ width: 20 }} />
        )}
        <MaterialIcon
          icon={hasChildren ? "folder_open" : "description"}
          fontSize={hasChildren ? "1.5rem" : "1.4rem"}
        />
      </Box>
      <Box as="span">{value}</Box>
    </Box>
  );
}

export function Gantt({ dataStore, meta }: ViewProps<GanttView>) {
  const [type, setType] = useState<GanttType>(GANTT_TYPES.WEEK);
  const [records, setRecords] = useState<DataRecord[]>([]);
  const { action } = useViewTab();

  const { fields, view } = meta;
  const { items } = view;
  const { domain, context } = action;

  const { formatter, transformer } = useMemo(
    () => ({
      formatter: (record: DataRecord) => formatRecord(view, record),
      transformer: (record: DataRecord) => transformRecord(view, record),
    }),
    [view]
  );

  const handleExpand = useCallback(
    (record: DataRecord, _expand?: boolean) =>
      setRecords((records) =>
        records.map((r) => (r.id === record.id ? { ...r, _expand } : r))
      ),
    []
  );

  const onSearch = useCallback(
    async (options?: SearchOptions) => {
      const { records } = await dataStore.search({
        ...options,
        filter: {
          _domain: domain || undefined,
          _domainContext: context,
        },
      });
      setRecords(records);
    },
    [dataStore, domain, context]
  );

  const updateRecord = useCallback(
    async (data: DataRecord) => {
      const record = transformer(data);
      const updated = await dataStore.save(record);
      updated &&
        setRecords((records) =>
          records.map((r) =>
            r.id === record.id ? { ...r, ...formatter(updated) } : r
          )
        );
    },
    [transformer, formatter, dataStore]
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
    [records, updateRecord]
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
    [records, updateRecord]
  );

  const handleRecordUpdate = useCallback(
    (record: DataRecord, changes: DataRecord) =>
      updateRecord({ ...record, ...changes }),
    [updateRecord]
  );

  useEffect(() => {
    onSearch();
  }, [onSearch]);

  const { page } = dataStore;
  const { offset = 0, limit = 40, totalCount = 0 } = page;

  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;

  const ganttItems = useMemo(
    () =>
      (items || [])
        .concat(
          view.taskUser
            ? [{ type: "field", name: view.taskUser } as Widget]
            : []
        )
        .map((item, ind) => ({
          ...(ind === 0 && {
            width: 160,
            renderer: (props: any) => (
              <TreeCell {...props} onExpand={handleExpand} />
            ),
          }),
          ...item,
          ...fields?.[item.name!],
          formatter: fieldFormatter,
        })) as unknown as GanttField[],
    [view, items, fields, handleExpand]
  );

  const ganttRecords = useMemo(() => {
    const getParent = (task: DataRecord) => task[view.taskParent!];

    function collect(
      parent?: DataRecord["id"],
      level: number = 1
    ): DataRecord[] {
      return records
        .filter((item) =>
          parent
            ? (getParent(item) || {}).id === parent
            : getParent(item) === parent
        )
        .reduce((list, _item) => {
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
    return list
      .filter((item) => item[view.taskStart!])
      .map(formatter) as GanttRecord[];
  }, [view, formatter, records]);

  return (
    <Box d="flex" className={styles.container} flex={1} flexDirection="column">
      <ViewToolBar
        meta={meta}
        actions={[]}
        pagination={{
          canPrev,
          canNext,
          onPrev: () => onSearch({ offset: offset - limit }),
          onNext: () => onSearch({ offset: offset + limit }),
          text: () => <PageText dataStore={dataStore} />,
        }}
      >
        <Box d="flex" gap={4}>
          {FILTERS.map((filter) => {
            const active = type === filter.key;
            return (
              <Button
                key={filter.key}
                variant="light"
                bg={active ? "light" : "body"}
                onClick={() => setType(filter.key)}
              >
                {filter.title}
              </Button>
            );
          })}
          <Button variant="light" bg="body" ms={2} onClick={() => onSearch()}>
            {i18n.get("Refresh")}
          </Button>
          <Button variant="light" bg="body">
            {i18n.get("Print")}
          </Button>
        </Box>
      </ViewToolBar>
      <Box d="flex" flex={1} overflow="auto">
        <DNDProvider>
          <GanttComponent
            className={styles.gantt}
            view={type}
            items={ganttItems}
            records={ganttRecords}
            onRecordConnect={handleRecordConnect}
            onRecordDisconnect={handleRecordDisconnect}
            onRecordUpdate={handleRecordUpdate}
          />
        </DNDProvider>
      </Box>
    </Box>
  );
}
