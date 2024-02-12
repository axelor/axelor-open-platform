import { SchedulerView } from "@/components/scheduler";

import { getNextOfAsDate, getStartOfAsDate } from "../../utils/date";

export function getTimes(date: Date | string, view: SchedulerView) {
  const times = { start: getStartOfAsDate(date, view), end: getNextOfAsDate(date, view) };

  return view === "month"
    ? {
        start: getStartOfAsDate(times.start, "week"),
        end: getNextOfAsDate(times.end, "week"),
      }
    : times;
}
