import { useAtomCallback } from "jotai/utils";
import deepGet from "lodash/get";
import { useCallback, useEffect, useMemo, useState } from "react";

import { Box, Button } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import {
  Scheduler,
  SchedulerEvent,
  SchedulerView,
} from "@/components/scheduler";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { usePerms } from "@/hooks/use-perms";
import { useManyEditor } from "@/hooks/use-relation";
import { useShortcuts } from "@/hooks/use-shortcut";
import { SearchOptions } from "@/services/client/data";
import { Criteria, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { moment } from "@/services/client/l10n";
import { findView } from "@/services/client/meta-cache";
import { CalendarView, FormView } from "@/services/client/meta.types";
import { ViewToolBar } from "@/view-containers/view-toolbar";
import {
  useViewContext,
  useViewTab,
  useViewTabRefresh,
} from "@/view-containers/views/scope";

import { createFormAtom } from "../form/builder/atoms";
import { useActionExecutor } from "../form/builder/scope";
import { Picker as DatePicker } from "../form/widgets/date/picker";
import { ViewProps } from "../types";
import { getColor } from "./colors";
import { Filter, Filters } from "./filters";
import { getTimes } from "./utils";

import styles from "./calendar.module.scss";

export function Calendar(props: ViewProps<CalendarView>) {
  const { meta, dataStore, searchAtom } = props;
  const {
    eventStart,
    eventStop,
    eventLength = 1,
    colorBy,
    editable = true,
    mode: initialMode = "month",
    onChange,
  } = meta.view;

  const colorField = meta.fields?.[colorBy!];
  const eventTitle = meta.view.items?.[0]?.name ?? "name";
  const allDayOnly = meta.fields?.[eventStart]?.type === "DATE";

  const [mode, setMode] = useState<SchedulerView>(initialMode);
  const [date, setDate] = useState<Date>(() => new Date());

  const [events, setEvents] = useState<SchedulerEvent<DataRecord>[]>([]);
  const [filters, setFilters] = useState<Filter[]>([]);

  const { action, dashlet } = useViewTab();

  const [searchStart, setSearchStart] = useState<Date>();
  const [searchEnd, setSearchEnd] = useState<Date>();

  const { hasButton } = usePerms(meta.view, meta.perms);

  const getViewContext = useViewContext();

  const hasPermission = useCallback(
    (name: string) => editable !== false && hasButton(name),
    [hasButton, editable],
  );

  const searchNames = useMemo(() => {
    const fieldNames = Object.keys(meta.fields ?? {});
    const itemNames = [eventStart, eventStop!, colorBy!].filter(Boolean);
    return [...new Set([...fieldNames, ...itemNames])];
  }, [meta.fields, eventStart, eventStop, colorBy]);

  const fetchItems = useAtomCallback(
    useCallback(
      async (get, set, start: Date, end: Date) => {
        let criteria: Criteria = {
          operator: "and",
          criteria: [
            {
              fieldName: eventStart,
              operator: ">=",
              value: start,
            },
            {
              fieldName: eventStart,
              operator: "<=",
              value: end,
            },
          ],
        };

        if (eventStop) {
          criteria = {
            operator: "or",
            criteria: [
              criteria,
              {
                operator: "and",
                criteria: [
                  {
                    fieldName: eventStop,
                    operator: ">=",
                    value: start,
                  },
                  {
                    fieldName: eventStart,
                    operator: "<=",
                    value: end,
                  },
                ],
              },
            ],
          };
        }

        const opts: SearchOptions = {
          limit: -1,
          fields: searchNames,
          filter: {
            criteria: [criteria],
          },
        };

        if (searchAtom) {
          const { query = {} } = get(searchAtom);
          if (query.criteria?.length) {
            opts.filter = {
              ...query,
              operator: "and",
              criteria: [
                { operator: "and", criteria: query.criteria },
                criteria,
              ],
            };
          } else {
            opts.filter = { ...opts.filter, ...query, criteria: [criteria] };
          }
        }

        if (dashlet && opts.filter) {
          const { _domainAction, ...formContext } = getViewContext() ?? {};
          const { _domainContext } = opts.filter;
          opts.filter._domainContext = {
            ..._domainContext,
            ...formContext,
          };
          opts.filter._domainAction = _domainAction;
        }

        const { records } = await dataStore.search(opts);
        return records;
      },
      [
        dashlet,
        dataStore,
        eventStart,
        eventStop,
        getViewContext,
        searchAtom,
        searchNames,
      ],
    ),
  );

  useEffect(() => {
    return dataStore.subscribe((ds) => {
      const records = [...ds.records].sort((x, y) => x.id! - y.id!);
      const events = records.map((record) => {
        const id = String(record.id);
        const title = record[eventTitle];
        const startValue = record[eventStart];
        const endValue = eventStop && record[eventStop];
        const start = startValue
          ? moment(startValue).toDate()
          : moment().toDate();
        const end = endValue
          ? moment(endValue).toDate()
          : moment(start).add(eventLength, "hours").toDate();

        const allDay =
          allDayOnly ||
          (moment(start).format("HH:mm") === "00:00" &&
            (!endValue || moment(end).format("HH:mm") === "00:00"));

        return {
          id,
          title,
          start,
          end,
          allDay,
          data: record,
        };
      });

      const filters = colorField
        ? events.reduce((acc, event) => {
            const { name, target, targetName, selectionList } = colorField;
            const getValue = (record: DataRecord) => {
              const value = deepGet(record, name);
              return target ? value?.id : value;
            };
            const getTitle = (record: DataRecord) => {
              const value = deepGet(record, name);
              return target && targetName
                ? deepGet(value, targetName)
                : selectionList?.find((x) => x.value == value)?.title ?? value;
            };
            const record = event.data!;
            const value = getValue(record);
            if ((value || value === 0) && !acc.some((x) => x.value === value)) {
              const color = getColor(value);
              const title = getTitle(record);
              acc.push({
                value,
                title,
                color,
                match: (event) => getValue(event.data!) === value,
              });
            }
            return acc;
          }, [] as Filter[])
        : [];

      setEvents(events);
      setFilters(filters);
    });
  }, [
    colorField,
    dataStore,
    eventLength,
    eventStart,
    eventStop,
    eventTitle,
    allDayOnly,
  ]);

  const onRefresh = useCallback(async () => {
    if (searchStart && searchEnd) {
      await fetchItems(searchStart, searchEnd);
    }
  }, [fetchItems, searchEnd, searchStart]);

  useEffect(() => {
    const { start, end } = getTimes(date, "month");
    if (
      moment(start).isSameOrAfter(searchStart) &&
      moment(start).isSameOrBefore(searchEnd) &&
      moment(end).isSameOrAfter(searchStart) &&
      moment(end).isSameOrBefore(searchEnd)
    ) {
      return;
    }
    setSearchStart(start);
    setSearchEnd(end);
  }, [date, searchEnd, searchStart]);

  useAsyncEffect(async () => {
    onRefresh();
  }, [onRefresh]);

  const onDelete = useCallback(
    async (record: DataRecord) => {
      const id = record.id!;
      const version = record.version!;
      const confirmed = await dialogs.confirm({
        content: i18n.get("Do you really want to delete the selected record?"),
        yesTitle: i18n.get("Delete"),
      });
      if (confirmed) {
        await dataStore.delete({ id, version });
        await onRefresh();
      }
      return confirmed;
    },
    [dataStore, onRefresh],
  );

  const showEditor = useManyEditor(action, dashlet);
  const showRecord = useCallback(
    async (record: DataRecord) => {
      const type = "form";
      const formView = (action.views?.find((view) => view.type === type) ||
        {}) as FormView;
      const { name, model = action.model ?? "" } = formView;
      const { view } = await findView({ type, name, model });
      const { title = "", name: viewName } = view;

      const canEdit = hasPermission("edit");
      const canDelete = canEdit && !!record.id && hasPermission("delete");

      showEditor({
        title,
        model,
        viewName,
        record,
        readonly: !canEdit,
        onSelect: () => onRefresh(),
        footer: ({ close }) => {
          return (
            canDelete && (
              <Box flex={1}>
                <Button
                  variant="danger"
                  onClick={async () => {
                    const confirmed = await onDelete(record);
                    if (confirmed) {
                      close(true);
                    }
                  }}
                >
                  {i18n.get("Delete")}
                </Button>
              </Box>
            )
          );
        },
      });
    },
    [
      action.views,
      action.model,
      hasPermission,
      showEditor,
      onRefresh,
      onDelete,
    ],
  );

  const onPrev = useCallback(
    () => setDate((prev) => moment(prev).add(-1, mode).toDate()),
    [mode],
  );

  const onNext = useCallback(
    () => setDate((prev) => moment(prev).add(1, mode).toDate()),
    [mode],
  );

  const onDayClick = useCallback((date: Date) => {
    setDate(() => date);
    setMode(() => "day");
  }, []);

  const onDateChange = useCallback((date: Date) => {
    setDate(() => date);
  }, []);

  const onFilterChange = useCallback((index: number) => {
    if (index > -1) {
      setFilters((filters) =>
        filters.map((filter, i) =>
          i === index ? { ...filter, checked: !filter.checked } : filter,
        ),
      );
    }
  }, []);

  const onEventClick = useCallback(
    ({ data: record }: SchedulerEvent<DataRecord>) => {
      if (record) showRecord(record);
    },
    [showRecord],
  );

  const onEventCreate = useCallback(
    ({ start, end }: SchedulerEvent<DataRecord>) => {
      if (hasPermission("new")) {
        const record: DataRecord = {
          [eventStart]: start.toISOString(),
        };
        if (eventStop) {
          record[eventStop] = end.toISOString();
        }
        showRecord(record);
      }
    },
    [eventStart, eventStop, hasPermission, showRecord],
  );

  const formAtom = useMemo(
    () =>
      createFormAtom({
        meta: meta as any,
        record: {},
      }),
    [meta],
  );

  const actionExecutor = useActionExecutor(meta.view, {
    formAtom,
    getContext: getViewContext,
    onRefresh,
  });

  const onEventChange = useAtomCallback(
    useCallback(
      async (
        get,
        set,
        { start, end, data, allDay }: SchedulerEvent<DataRecord>,
      ) => {
        let record: DataRecord = { ...data };
        let endValue = end ?? moment(start).add(eventLength, "hours").toDate();
        if (allDay) {
          endValue = moment(endValue).startOf("day").toDate();
        }

        const startStr = start.toISOString();
        const endStr = endValue.toISOString();

        if (eventStart) record[eventStart] = startStr;
        if (eventStop) record[eventStop] = endStr;

        if (onChange) {
          set(formAtom, (prev) => ({ ...prev, record }));
          await actionExecutor.execute(onChange, {
            context: { ...record },
          });
          record = { ...record, ...get(formAtom).record };
        }

        await dataStore.save(record);
        await onRefresh();
      },
      [
        actionExecutor,
        dataStore,
        eventLength,
        eventStart,
        eventStop,
        formAtom,
        onChange,
        onRefresh,
      ],
    ),
  );

  const pageText = useMemo(() => toPageText(date, mode), [date, mode]);

  const actions = useMemo(() => {
    return [
      {
        key: "month",
        text: i18n.get("Month"),
        iconOnly: false,
        checked: mode === "month",
        onClick: () => setMode("month"),
      },
      {
        key: "week",
        text: i18n.get("Week"),
        iconOnly: false,
        checked: mode === "week",
        onClick: () => setMode("week"),
      },
      {
        key: "day",
        text: i18n.get("Day"),
        iconOnly: false,
        checked: mode === "day",
        onClick: () => setMode("day"),
      },
      {
        key: "d1",
        divider: true,
      },
      {
        key: "today",
        text: i18n.get("Today"),
        iconOnly: false,
        disabled: moment(date).startOf("day").isSame(moment().startOf("day")),
        onClick: () => setDate(new Date()),
      },
      {
        key: "refresh",
        text: i18n.get("Refresh"),
        iconOnly: false,
        onClick: onRefresh,
      },
    ];
  }, [date, mode, onRefresh]);

  const filteredEvents = useMemo(() => {
    const checked = filters.filter((x) => x.checked);
    return events.reduce((acc, event) => {
      const filter = filters.find((x) => x.match?.(event));
      if (filter) {
        event = {
          ...event,
          backgroundColor: filter.color,
          borderColor: filter.color,
        };
      }
      if (checked.length === 0 || checked.some((x) => x.match?.(event))) {
        return [...acc, event];
      }
      return acc;
    }, [] as SchedulerEvent<DataRecord>[]);
  }, [events, filters]);

  // register shortcuts
  useShortcuts({ viewType: "calendar", onRefresh: onRefresh });

  // register tab:refresh
  useViewTabRefresh("calendar", onRefresh);

  return (
    <div className={styles.container}>
      <ViewToolBar
        meta={meta}
        actions={actions}
        pagination={{
          canPrev: true,
          canNext: true,
          onNext,
          onPrev,
          text: pageText,
        }}
      />
      <div className={styles.wrapper}>
        <Scheduler
          className={styles.calendar}
          view={mode}
          date={date}
          locale={moment.locale()}
          allDayText={i18n.get("All Day")}
          allDayOnly={allDayOnly}
          moreText={(n) => i18n.get("+{0} more", n)}
          events={filteredEvents}
          editable={hasPermission("edit")}
          onDayClick={onDayClick}
          onEventClick={onEventClick}
          onEventCreate={onEventCreate}
          onEventChange={onEventChange}
        />
        <div className={styles.sidebar}>
          <DatePicker selected={date} onChange={onDateChange} inline={true} />
          <Filters data={filters} onChange={onFilterChange} />
        </div>
      </div>
    </div>
  );
}

function toPageText(date: Date, mode: SchedulerView) {
  if (mode === "month") return moment(date).format("MMMM YYYY");
  if (mode === "week") {
    const start = moment(date).startOf("week");
    const end = moment(date).endOf("week");

    const startDate = start.format("MMM DD");
    const startMonth = start.format("MMM");
    const startYear = start.format("YYYY");

    const endMonth = end.format("MMM");
    const endYear = end.format("YYYY");
    const endDate =
      startMonth === endMonth ? end.format("DD") : end.format("MMM DD");

    return startYear === endYear
      ? `${startDate} - ${endDate}, ${endYear}`
      : `${startDate}, ${startYear} - ${endDate}, ${endYear}`;
  }
  return moment(date).format("MMMM DD, YYYY");
}
