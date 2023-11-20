import { SchedulerView } from "@/components/scheduler";

import { getNextOf, getStartOf } from "../../utils/date";

export function getTimes(date: Date | string, view: SchedulerView) {
  const times = { start: getStartOf(date, view), end: getNextOf(date, view) };

  return view === "month"
    ? {
        start: getStartOf(times.start, "week"),
        end: getNextOf(times.end, "week"),
      }
    : times;
}
