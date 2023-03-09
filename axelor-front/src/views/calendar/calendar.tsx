import { CalendarView } from "@/services/client/meta.types";
import { ViewProps } from "../types";

import { useState, useMemo, useCallback, useEffect } from "react";
import { atom, useAtom } from "jotai";

import {
  Scheduler,
  SchedulerProps,
  SchedulerEvent,
  Event,
  View,
} from "@axelor/ui/scheduler";

import { Tab } from "@/hooks/use-tabs";
import { DataStore } from "@/hooks/use-data-store";
import { useAsync } from "@/hooks/use-async";

import { ViewData } from "@/services/client/meta";

import {
  ActionView,
  HelpOverride,
  JsonField,
  MenuItem,
  Perms,
  Property,
  SavedFilter,
  ViewTypes,
} from "../../services/client/meta.types";

import { Criteria } from "../../services/client/data.types";
import { getStartOf, getNextOf, addDate } from "../../utils/date";

import { useSession } from "@/hooks/use-session";
import { useMoment } from "@/hooks/use-moment";

function getTimes(date: Date | string, view: View) {
  return { start: getStartOf(date, view), end: getNextOf(date, view) };
}

export function Calendar(props: ViewProps<CalendarView>) {
  const {
    meta: { view: metaView, fields: metaFields, perms: metaPerms },
    dataStore = {} as DataStore,
  } = props;
  const { search = () => {}, records = [] } = dataStore;

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
  const { momentLocale } = useMoment();

  const searchFieldNames = useMemo(() => {
    const schemaFieldNames = Object.keys(
      metaFields as Record<string, Property>
    );
    const viewFieldNames = [eventStart, eventStop, colorBy].filter(
      (field) => field
    );
    return [...new Set([...schemaFieldNames, ...viewFieldNames])]; // XXX: custom fields?
  }, [metaFields, eventStart, eventStop, colorBy]);

  const [calendarDate, setCalendarDate] = useState<Date>(() => new Date());
  const [calendarMode, setCalendarMode] = useState<View>(initialMode);

  const { start: calendarStart, end: calendarEnd } = useMemo(() => {
    return getTimes(calendarDate, calendarMode);
  }, [calendarDate, calendarMode]);

  const data = useAsync(async () => {
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
    const stopCriteria: Criteria = eventStop && {
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
    };
    const filter = stopCriteria
      ? ({
          operator: "or",
          criteria: [startCriteria, stopCriteria],
        } as Criteria)
      : startCriteria;
    return await search({
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

  const calendarEvents: SchedulerProps["events"] = useMemo(() => {
    return records.map((record) => {
      const { id, name: title } = record;
      const start = new Date(record[eventStart] as string);
      const recordStop = record[eventStop] as string;
      const end = recordStop
        ? new Date(recordStop)
        : addDate(start, eventLength, "hours");
      return {
        id,
        title,
        start,
        end,
        record,
      };
    }) as SchedulerProps["events"];
  }, [records, eventStart, eventStop, eventLength]);

  const handleNavigationChange = useCallback((date: Date) => {
    setCalendarDate(date);
  }, []);

  const handleViewChange = useCallback((view: View) => {
    setCalendarMode(view);
  }, []);

  if (data.state === "loading" || !momentLocale) {
    return <div>Loading</div>;
  }

  return (
    <Scheduler
      events={calendarEvents}
      date={calendarDate}
      view={calendarMode}
      onNavigationChange={handleNavigationChange}
      onViewChange={handleViewChange}
      style={{ height: 800, width: "100%" }}
    />
  );
}
