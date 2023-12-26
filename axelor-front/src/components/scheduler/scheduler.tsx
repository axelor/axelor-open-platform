import {
  DateSelectArg,
  EventApi,
  EventChangeArg,
  EventClickArg,
} from "@fullcalendar/core";
import allLocales from "@fullcalendar/core/locales-all";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";
import FullCalendar from "@fullcalendar/react";
import timeGridPlugin from "@fullcalendar/timegrid";
import clsx from "clsx";
import { useCallback, useEffect, useRef } from "react";

import styles from "./scheduler.module.scss";

export type SchedulerView = "month" | "week" | "day";

export interface SchedulerEvent<T> {
  id: string;
  start: Date;
  end: Date;
  title: string;
  allDay?: boolean;
  className?: string;
  textColor?: string;
  backgroundColor?: string;
  borderColor?: string;
  data?: T;
}

function toSchedulerEvent<T>(event: EventApi) {
  const { start, end, extendedProps = {} } = event;
  const { data } = extendedProps;
  return {
    ...event.toPlainObject(),
    start: start ?? undefined,
    end: end ?? undefined,
    data,
  } as SchedulerEvent<T>;
}

export interface SchedulerProps<T> {
  className?: string;
  date?: Date;
  view?: SchedulerView;
  events?: SchedulerEvent<T>[];
  editable?: boolean;
  editableDuration?: boolean;
  locale?: string;
  allDayText?: string;
  allDayOnly?: boolean;
  maxEvents?: number | boolean;
  moreText?: (n: number) => string;
  onDayClick?: (date: Date) => void;
  onEventClick?: (event: SchedulerEvent<T>, element: HTMLElement) => void;
  onEventCreate?: (event: SchedulerEvent<T>) => void;
  onEventChange?: (event: SchedulerEvent<T>) => void;
}

const timeViewMap = {
  month: "dayGridMonth",
  week: "timeGridWeek",
  day: "timeGridDay",
} as const;

const dayViewMap = {
  month: "dayGridMonth",
  week: "dayGridWeek",
  day: "dayGridDay",
} as const;

export function Scheduler<T>(props: SchedulerProps<T>) {
  const {
    className,
    editable = true,
    editableDuration,
    locale,
    view: mode = "month",
    events,
    allDayText,
    allDayOnly,
    maxEvents = true,
    moreText,
    onEventCreate,
    onEventChange,
    onEventClick,
    onDayClick,
  } = props;
  const calendarRef = useRef<FullCalendar>(null);

  const view = allDayOnly ? dayViewMap[mode] : timeViewMap[mode];
  const date = props.date;

  useEffect(() => {
    const api = calendarRef.current?.getApi();
    if (view) api?.changeView(view);
    if (date) api?.gotoDate(date);
  }, [date, view]);

  const handleSet = useCallback(
    ({ start, end, allDay }: DateSelectArg) => {
      const api = calendarRef.current?.getApi();
      api?.unselect();
      onEventCreate?.({ start, end, allDay, id: "", title: "" });
    },
    [onEventCreate],
  );

  const handleChange = useCallback(
    (arg: EventChangeArg) => {
      onEventChange?.(toSchedulerEvent<T>(arg.event));
    },
    [onEventChange],
  );

  const handleClick = useCallback(
    ({ event, el }: EventClickArg) => {
      onEventClick?.(toSchedulerEvent<T>(event), el);
    },
    [onEventClick],
  );

  const handleDayClick = useCallback(
    (date: Date) => {
      onDayClick?.(date);
    },
    [onDayClick],
  );

  return (
    <div className={clsx(styles.scheduler, className)}>
      <FullCalendar
        ref={calendarRef}
        selectable={true}
        selectMirror={true}
        dayMaxEvents={maxEvents}
        editable={editable}
        locales={allLocales}
        locale={locale}
        initialView={view}
        initialDate={date}
        events={events}
        viewHint={view}
        headerToolbar={false}
        plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
        select={handleSet}
        eventChange={handleChange}
        eventClick={handleClick}
        eventDurationEditable={editableDuration}
        navLinks={true}
        navLinkDayClick={handleDayClick}
        allDayText={allDayText}
        moreLinkText={moreText}
      />
    </div>
  );
}
