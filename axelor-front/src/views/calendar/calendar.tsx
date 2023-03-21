import {
  useState,
  useMemo,
  useCallback,
  useEffect,
  lazy,
  Suspense,
} from "react";

import { Box } from "@axelor/ui/core";
import { SchedulerProps, SchedulerEvent, View } from "@axelor/ui/scheduler";

import { CalendarView } from "@/services/client/meta.types";

import { useAsync } from "@/hooks/use-async";
import { useSession } from "@/hooks/use-session";

import { Property } from "../../services/client/meta.types";
import { Criteria } from "../../services/client/data.types";
import { addDate } from "../../utils/date";

import { ViewProps } from "../types";

import { Filter } from "./components/types";
import Loading from "./components/loading";
import { getEventFilters, getTimes } from "./utils";

import styles from "./calendar.module.scss";
import { DEFAULT_COLOR } from "./colors";

const Scheduler = lazy(() => import("./components/scheduler"));
const DatePicker = lazy(() => import("./components/date-picker"));
const Filters = lazy(() => import("./components/filters"));

const eventStyler = ({
  event,
}: {
  event: SchedulerEvent & { $backgroundColor?: string };
}) => ({
  style: { backgroundColor: event.$backgroundColor },
});

function CalendarToobar(props: any) {
  console.log(props);
  return null;
}

export function Calendar(props: ViewProps<CalendarView>) {
  const components = useMemo(
    () => ({
      week: {
        header: ({ date, localizer }: any) => {
          return localizer.format(date, "ddd D");
        },
      },
      // toolbar: CalendarToobar,
    }),
    []
  );

  const {
    meta: { view: metaView, fields: _metaFields, perms: metaPerms },
    dataStore,
  } = props;
  const metaFields = _metaFields as Record<string, Property>;

  const {
    eventStart,
    eventStop,
    eventLength = 1,
    colorBy,
    mode: initialMode = "month",
  } = metaView;
  const session = useSession();
  const maxPerPage = useMemo(
    () => session.data?.api?.pagination?.maxPerPage || -1,
    [session]
  );

  const searchFieldNames = useMemo(() => {
    const schemaFieldNames = Object.keys(metaFields);
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

  useAsync(async () => {
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
    const filter = stopCriteria
      ? ({
          operator: "or",
          criteria: [startCriteria, stopCriteria],
        } as Criteria)
      : startCriteria;
    return await dataStore.search({
      fields: searchFieldNames,
      filter,
      limit: maxPerPage,
    });
  }, [
    calendarStart,
    calendarEnd,
    searchFieldNames,
    eventStart,
    eventStop,
    maxPerPage,
  ]);

  const _calendarEvents: SchedulerEvent[] = useMemo(() => {
    return (dataStore.records || []).map((record) => {
      const { id, name: title } = record;
      const start = new Date(record[eventStart] as string);
      const recordStop = eventStop && (record[eventStop] as string);
      const end = recordStop
        ? new Date(recordStop)
        : addDate(start, eventLength, "hours");
      return {
        id,
        title,
        start,
        end,
        record,
      } as SchedulerEvent;
    });
  }, [dataStore.records, eventStart, eventStop, eventLength]);

  const handleNavigationChange = useCallback((date: Date) => {
    setCalendarDate(date);
  }, []);

  const handleViewChange = useCallback((view: View) => {
    setCalendarMode(view);
  }, []);

  // Filters

  const colorByField = colorBy ? metaFields[colorBy] : null;

  const [filters, setFilters] = useState<Filter[]>([]);

  useEffect(() => {
    colorByField &&
      setFilters(getEventFilters(_calendarEvents || [], colorByField));
  }, [_calendarEvents, colorByField]);

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
    return _calendarEvents.reduce((list: object[], event: SchedulerEvent) => {
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
    }, []) as SchedulerEvent[];
  }, [_calendarEvents, filters]);

  return (
    <Suspense fallback={<Loading />}>
      <Box
        d="flex"
        flexDirection="row"
        flexGrow={1}
        className={styles["calendar"]}
      >
        <Box d="flex" p={2} pe={0} className={styles["scheduler-panel"]}>
          <Scheduler
            events={calendarEvents}
            date={calendarDate}
            view={calendarMode}
            onNavigationChange={handleNavigationChange}
            onViewChange={handleViewChange}
            eventStyler={eventStyler}
            components={components}
            style={{ width: "100%" }}
          />
        </Box>
        <Box flex={1} p={2} className={styles["calendar-panel"]}>
          <DatePicker
            selected={calendarDate}
            onChange={(date: Date) => {
              setCalendarDate(date);
              handleNavigationChange(date);
            }}
            {...({ inline: true } as any)}
          />
          <Filters data={filters} onChange={handleFilterChange} />
        </Box>
      </Box>
    </Suspense>
  );
}
