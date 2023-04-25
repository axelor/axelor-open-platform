import { useState, useMemo, useCallback, useEffect } from "react";
import { useAtomValue } from "jotai";

import { Box, CommandItemProps } from "@axelor/ui/core";
import { Scheduler, SchedulerEvent, View } from "@axelor/ui/scheduler";
import { Event } from "@axelor/ui/scheduler/types";
import { MaterialIconProps } from "@axelor/ui/src/icons/meterial-icon";

import { i18n } from "@/services/client/i18n";
import { l10n } from "@/services/client/l10n";
import { findView } from "@/services/client/meta-cache";
import { CalendarView, FormView } from "@/services/client/meta.types";
import { Criteria, DataRecord } from "@/services/client/data.types";

import { ViewToolBar } from "@/view-containers/view-toolbar";
import { dialogs } from "@/components/dialogs";
import { addDate, getNextOf } from "@/utils/date";

import { useViewTab } from "@/view-containers/views/scope";
import { usePerms } from "@/hooks/use-perms";
import { useEditor } from "@/hooks/use-relation";
import { useAsync } from "@/hooks/use-async";
import { useSession } from "@/hooks/use-session";

import { ViewProps } from "../types";

import DatePicker from "./components/date-picker";
import Filters from "./components/filters";
import { Filter } from "./components/types";
import Popover from "./components/popover";

import {
  formatDate,
  getEventFilters,
  getTimes,
  toDateOnlyString,
  toDatetimeString,
  toDateOnly,
  toDatetime,
} from "./utils";

import styles from "./calendar.module.scss";
import { DEFAULT_COLOR } from "./colors";

const { get: _t } = i18n;

const eventStyler = ({
  event,
}: {
  event: SchedulerEvent & { $backgroundColor?: string };
}) => ({
  style: { backgroundColor: event.$backgroundColor },
});

l10n.getLocale();

export function Calendar(props: ViewProps<CalendarView>) {
  const { meta, dataStore, searchAtom } = props;
  const { view: metaView, fields: metaFields, perms: metaPerms } = meta;
  const {
    eventStart,
    eventStop,
    eventLength = 1,
    colorBy,
    mode: initialMode = "month",
  } = metaView;

  const advancedSearch = useAtomValue(searchAtom!);

  const nameField = (metaView.items?.[0] || { name: "name" }).name ?? "name";

  const { hasButton } = usePerms(metaView, metaPerms);
  const editableAndButton = useCallback(
    (name: string) => metaView.editable !== false && hasButton(name),
    [hasButton, metaView.editable]
  );

  const session = useSession();
  const maxPerPage = useMemo(
    () => session.data?.api?.pagination?.maxPerPage || -1,
    [session]
  );

  const isDateCalendar = useMemo(
    () => metaFields?.[eventStart]?.type === "DATE",
    [metaFields, eventStart]
  );

  const convertDate = useMemo(() => {
    return isDateCalendar
      ? {
          toDate: toDateOnly,
          toString: toDateOnlyString,
        }
      : {
          toDate: toDatetime,
          toString: toDatetimeString,
        };
  }, [isDateCalendar]);

  const components = useMemo(() => {
    const timeComponents = isDateCalendar
      ? {
          dayColumnWrapper: () => null,
          timeSlotWrapper: () => null,
          timeGutterWrapper: () => null,
        }
      : {};
    return {
      week: {
        header: ({ date, localizer }: any) => {
          return localizer.format(date, "ddd D");
        },
      },
      toolbar: () => null,
      ...timeComponents,
    };
  }, [isDateCalendar]);

  const searchFieldNames = useMemo(() => {
    const schemaFieldNames = Object.keys(metaFields || {});
    const viewFieldNames = [eventStart, eventStop, colorBy].filter(
      (field) => field
    ) as string[];
    return [...new Set([...schemaFieldNames, ...viewFieldNames])];
  }, [metaFields, eventStart, eventStop, colorBy]);

  const [calendarDate, setCalendarDate] = useState<Date>(() => new Date());
  const [calendarMode, setCalendarMode] = useState<View>(initialMode);

  const { start: calendarStart, end: calendarEnd } = useMemo(() => {
    return getTimes(calendarDate, calendarMode);
  }, [calendarDate, calendarMode]);

  const filter = useMemo(() => {
    const startCriteria: Criteria = {
      operator: "and",
      criteria: [
        {
          fieldName: eventStart,
          operator: ">=",
          value: calendarStart,
        },
        {
          fieldName: eventStart,
          operator: "<",
          value: calendarEnd,
        },
      ],
    };
    const stopCriteria: Criteria | null = eventStop
      ? ({
          operator: "and",
          criteria: [
            {
              fieldName: eventStop,
              operator: ">=",
              value: calendarStart,
            },
            {
              fieldName: eventStart, // include intermediate events: RM-31366
              operator: "<",
              value: calendarEnd,
            },
          ],
        } as Criteria)
      : null;
    let filter = stopCriteria
      ? ({
          operator: "or",
          criteria: [startCriteria, stopCriteria],
        } as Criteria)
      : startCriteria;

    if (advancedSearch.query) {
      filter = {
        ...advancedSearch.query,
        operator: "and",
        criteria: [advancedSearch.query, filter],
      };
    }

    return filter;
  }, [eventStart, calendarStart, calendarEnd, eventStop, advancedSearch.query]);

  const handleRefresh = useCallback(async () => {
    const res = await dataStore.search({
      filter,
      fields: searchFieldNames,
      limit: maxPerPage,
    });
    setRecords(res.records);
  }, [dataStore, filter, searchFieldNames, maxPerPage]);

  useAsync(handleRefresh, [dataStore, filter]);

  const [records, setRecords] = useState<DataRecord[]>([]);
  useEffect(() => setRecords(dataStore.records), [dataStore.records]);

  const unfilteredCalendarEvents: SchedulerEvent[] = useMemo(() => {
    return records.map((record) => {
      const { id, [nameField]: title } = record;
      const start = convertDate.toDate(record[eventStart] as string);
      const recordStop = eventStop && (record[eventStop] as string);
      let end = convertDate.toDate(
        recordStop ? recordStop : addDate(start, eventLength, "hours")
      );
      if (end && isDateCalendar) {
        end = convertDate.toDate(getNextOf(end, "days"));
      }
      return {
        id,
        title,
        start,
        end,
        record,
        allDay: isDateCalendar,
      } as SchedulerEvent;
    });
  }, [
    records,
    nameField,
    convertDate,
    eventStart,
    eventStop,
    eventLength,
    isDateCalendar,
  ]);

  const handleNavigationChange = useCallback((date: Date) => {
    setCalendarDate(date);
  }, []);

  const handleViewChange = useCallback((view: View) => {
    setCalendarMode(view);
  }, []);

  // Filters

  const colorByField = colorBy && metaFields?.[colorBy];

  const [filters, setFilters] = useState<Filter[]>([]);

  useEffect(() => {
    colorByField &&
      setFilters(getEventFilters(unfilteredCalendarEvents || [], colorByField));
  }, [unfilteredCalendarEvents, colorByField]);

  const handleFilterChange = useCallback((ind: number) => {
    if (ind > -1) {
      setFilters((filters) =>
        filters.map((filter, index) =>
          index === ind ? { ...filter, checked: !filter.checked } : filter
        )
      );
    }
  }, []);

  const calendarEvents: SchedulerEvent[] = useMemo(() => {
    const checkedFilters = filters.filter((x) => x.checked);
    const showAll = checkedFilters.length === 0;
    return unfilteredCalendarEvents.reduce(
      (list: object[], event: SchedulerEvent) => {
        const filter = (showAll ? filters : checkedFilters).find(
          (filter: Filter) => filter.match!(event)
        );
        return filter || showAll
          ? [
              ...list,
              {
                ...event,
                $backgroundColor: (filter || {}).color || DEFAULT_COLOR,
              },
            ]
          : list;
      },
      []
    ) as SchedulerEvent[];
  }, [unfilteredCalendarEvents, filters]);

  // Toobar

  const handleNav = useCallback(
    (amount: 1 | -1) => {
      setCalendarDate((date) => addDate(date, amount, calendarMode));
    },
    [calendarMode]
  );

  const handleNext = useCallback(() => handleNav(1), [handleNav]);
  const handlePrev = useCallback(() => handleNav(-1), [handleNav]);

  const handleToday = useCallback(() => {
    setCalendarDate(new Date());
  }, []);

  const calendarTitle = useMemo(() => {
    return formatDate(calendarDate, calendarMode);
  }, [calendarDate, calendarMode]);

  const actions = useMemo<CommandItemProps[]>(() => {
    const views: {
      view: View;
      text: string;
      iconProps: MaterialIconProps;
    }[] = [
      {
        view: "month",
        text: _t("Month"),
        iconProps: { icon: "calendar_view_month" },
      },
      {
        view: "week",
        text: _t("Week"),
        iconProps: { icon: "calendar_view_week" },
      },
      {
        view: "day",
        text: _t("Day"),
        iconProps: { icon: "calendar_view_day" },
      },
    ];

    const today = new Date();
    const inToday = today >= calendarStart && today <= calendarEnd;

    return [
      ...views.map(({ view, text, iconProps }) => ({
        key: view,
        text: text,
        description: text,
        iconProps,
        iconOnly: false,
        checked: view === calendarMode,
        onClick: () => handleViewChange(view),
      })),
      {
        key: "modeDivider",
        divider: true,
      },
      {
        key: "today",
        text: _t("Today"),
        description: _t("Today"),
        iconProps: {
          icon: "today",
        },
        iconOnly: false,
        checked: inToday,
        onClick: handleToday,
      },
      {
        key: "refresh",
        text: _t("Refresh"),
        description: _t("Refresh"),
        iconProps: {
          icon: "refresh",
        },
        iconOnly: false,
        onClick: handleRefresh,
      },
    ];
  }, [
    calendarStart,
    calendarEnd,
    handleToday,
    handleRefresh,
    calendarMode,
    handleViewChange,
  ]);

  const handleDateChange = useCallback(
    (date: Date) => {
      setCalendarDate(date);
      handleNavigationChange(date);
    },
    [handleNavigationChange]
  );

  // Update

  const _handleEventUpdate = useCallback(
    async ({ event, start, end }: Event) => {
      const record = (event as any).record as DataRecord;
      const { id, version } = record;

      const eventData = {
        id,
        version,
        [eventStart]: convertDate.toString(start),
      } as DataRecord;

      if (eventStop) {
        const _end = isDateCalendar ? addDate(end, -1, "days") : end;
        eventData[eventStop] = convertDate.toString(_end);
      }

      const eventStartJsonField =
        eventStart && metaFields?.[eventStart]?.jsonField;
      const eventStopJsonField =
        eventStop && metaFields?.[eventStop]?.jsonField;
      if (eventStartJsonField) {
        eventData[eventStartJsonField] = record[eventStartJsonField];
      }
      if (eventStopJsonField) {
        eventData[eventStopJsonField] = record[eventStopJsonField];
      }

      setRecords((records) =>
        records.map((record) =>
          record.id === eventData.id ? { ...record, ...eventData } : record
        )
      );

      const updatedRecord = await dataStore.save(eventData);

      setRecords((records) =>
        records.map((record) =>
          record.id === updatedRecord.id
            ? { ...record, ...updatedRecord }
            : record
        )
      );
    },
    [eventStart, convertDate, eventStop, dataStore, metaFields, isDateCalendar]
  );

  const handleEventUpdate = editableAndButton("edit")
    ? _handleEventUpdate
    : undefined;

  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<SchedulerEvent | null>(
    null
  );

  const closePopover = useCallback(() => {
    setAnchorEl(null);
    setSelectedEvent(null);
  }, []);

  const handleEventSelect = useCallback(
    ({ event }: Event, e: React.SyntheticEvent<any>) => {
      setSelectedEvent(event);
      setAnchorEl(e.currentTarget);
    },
    []
  );

  const showEditor = useEditor();
  const viewTab = useViewTab();

  const showRecord = useCallback(
    async (
      record: DataRecord,
      readonly: boolean,
      onSelect: (data: DataRecord) => void
    ) => {
      const action = viewTab.action;
      const type = "form";
      const formView = (action.views?.find((view) => view.type === type) ||
        {}) as FormView;
      const { name, model = action.model ?? "" } = formView;

      const view = await findView({ type, name, model });
      const {
        view: { title = "", name: viewName },
      } = view;

      showEditor({
        title,
        model,
        viewName,
        record,
        readonly,
        onSelect,
      });
    },
    [showEditor, viewTab.action]
  );

  const handleOpenEvent = useCallback(
    (
      event: SchedulerEvent,
      readonly: boolean,
      onSelect: (data: DataRecord) => void
    ) => {
      setAnchorEl(null);
      const record = (event as Record<string, any>).record as DataRecord;
      showRecord(record, readonly, onSelect);
    },
    [showRecord]
  );

  const _handleEditEvent = useCallback(
    (event: SchedulerEvent) =>
      handleOpenEvent(event, false, (data: DataRecord) => {
        setRecords((records) =>
          records.map((record) =>
            record.id === data.id ? { ...record, ...data } : record
          )
        );
      }),
    [handleOpenEvent]
  );

  const _handleViewEvent = useCallback(
    (event: SchedulerEvent) => handleOpenEvent(event, true, () => {}),
    [handleOpenEvent]
  );

  const handleEditEvent = editableAndButton("edit")
    ? _handleEditEvent
    : undefined;
  const handleViewEvent = hasButton("view") ? _handleViewEvent : undefined;

  const _handleEventCreate = useCallback(
    ({ start, end }: Event) => {
      const record = {
        [eventStart]: convertDate.toString(start),
      } as DataRecord;

      if (eventStop) {
        const _end = isDateCalendar ? addDate(end, -1, "days") : end;
        record[eventStop] = convertDate.toString(_end);
      }

      showRecord(record, false, (data: DataRecord) => {
        setRecords((records) => [...records, data]);
      });
    },
    [convertDate, eventStart, eventStop, isDateCalendar, showRecord]
  );

  const handleEventCreate = editableAndButton("new")
    ? _handleEventCreate
    : undefined;

  const _handleDeleteEvent = useCallback(
    async (event: SchedulerEvent) => {
      closePopover();

      const confirmed = await dialogs.box({
        title: _t("Deletion Confirmation"),
        content: _t('Are you sure you want to delete "{0}"?', event.title),
        yesTitle: _t("Delete"),
        noTitle: _t("Cancel"),
      });

      if (!confirmed) return;

      const record = (event as Record<string, any>).record as DataRecord;
      const { id, version } = record;
      if (id == null || version == null) return;

      await dataStore.delete({ id, version });
      setRecords((records) => records.filter((record) => record.id !== id));
    },
    [closePopover, dataStore]
  );

  const handleDeleteEvent = editableAndButton("delete")
    ? _handleDeleteEvent
    : undefined;

  return (
    <div className={styles.calendar}>
      <ViewToolBar
        meta={meta}
        actions={actions}
        pagination={{
          canPrev: true,
          canNext: true,
          onNext: handleNext,
          onPrev: handlePrev,
          text: calendarTitle,
        }}
      />
      <Box d="flex" flexDirection="row" flexGrow={1}>
        <Box d="flex" p={2} pe={0} className={styles["scheduler-panel"]}>
          <Scheduler
            events={calendarEvents}
            date={calendarDate}
            view={calendarMode}
            onNavigationChange={handleNavigationChange}
            onViewChange={handleViewChange}
            onEventResize={handleEventUpdate}
            onEventDrop={handleEventUpdate}
            onEventSelect={handleEventSelect}
            onEventCreate={handleEventCreate}
            eventStyler={eventStyler}
            components={components}
            style={{ width: "100%" }}
            selectable="ignoreEvents"
          />
          <Popover
            anchorEl={anchorEl}
            data={selectedEvent}
            onEdit={handleEditEvent}
            onView={handleViewEvent}
            onDelete={handleDeleteEvent}
            onClose={closePopover}
            eventStart={eventStart}
            eventStop={eventStop}
            isDateCalendar={isDateCalendar}
          />
        </Box>
        <Box flex={1} p={2} className={styles["calendar-panel"]}>
          <DatePicker
            selected={calendarDate}
            onChange={handleDateChange}
            {...({ inline: true } as any)}
          />
          <Filters data={filters} onChange={handleFilterChange} />
        </Box>
      </Box>
    </div>
  );
}
