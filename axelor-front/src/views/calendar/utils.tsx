import { SchedulerEvent, View } from "@axelor/ui/scheduler";
import dayjs, { ManipulateType } from "dayjs";
import getObjectValue from "lodash/get";

import { i18n } from "@/services/client/i18n";
import { getNextOf, getStartOf } from "../../utils/date";
import { getColor } from "./colors";

export function getTimes(date: Date | string, view: View) {
  const times = { start: getStartOf(date, view), end: getNextOf(date, view) };

  return view === "month"
    ? {
        start: getStartOf(times.start, "week"),
        end: getNextOf(times.end, "week"),
      }
    : times;
}

export function getEventFilters(events: SchedulerEvent[], colorField: any) {
  const { name, target, targetName, selectionList } = colorField;
  const getValue = (obj: any) => {
    const val = getObjectValue(obj, name);
    return target ? val?.id : val;
  };
  const getLabel = (val: any) => {
    if (selectionList) {
      return (
        selectionList.find((x: any) => `${x.value}` === `${val}`)?.title || val
      );
    }
    if (target && targetName) {
      return val && getObjectValue(val, targetName);
    }
    return val;
  };
  const getRecord = (event: SchedulerEvent) => {
    return (event as any).record;
  };

  return events.reduce((list: any, event: SchedulerEvent) => {
    const record = getRecord(event);
    const _value = getObjectValue(record, name);
    const value = getValue(record);
    if (
      (value || value === 0) &&
      !list.some((item: any) => item.value === value)
    ) {
      return [
        ...list,
        {
          value,
          match: (obj: SchedulerEvent) => getValue(getRecord(obj)) === value,
          color: getColor(value),
          label: getLabel(_value),
        },
      ];
    }
    return list;
  }, []);
}

interface DateFormatMap {
  [key: string]: (start: Date, end: Date | undefined) => string;
}

export function formatDate(start: Date, end: Date, unit: ManipulateType) {
  const DATE_FORMATTERS: DateFormatMap = {
    month: (date) => dayjs(date).format("MMMM YYYY"),
    week: (start, end) => {
      const startMonth = dayjs(start).format("MMM");
      const startYear = dayjs(start).format("YYYY");
      const endMonth = dayjs(end).format("MMM");
      const endYear = dayjs(end).format("YYYY");
      if (startYear === endYear) {
        if (startMonth === endMonth) {
          return `${startMonth} ${startYear}`;
        }
        return `${startMonth} – ${endMonth} ${endYear}`;
      }
      return `${startMonth} ${startYear} – ${endMonth} ${endYear}`;
    },
    day: (date) => dayjs(date).format("LL"),
  };

  const formatter = DATE_FORMATTERS[unit] || DATE_FORMATTERS.day;
  return formatter(start, end);
}

export function toDateOnlyString(date: Date | string) {
  return dayjs(date).format("YYYY-MM-DD");
}

export function toDatetimeString(date: Date | string) {
  return dayjs(date).toISOString();
}

export function toDateOnly(date: Date | string) {
  return dayjs(date).startOf("day").toDate();
}

export function toDatetime(date: Date | string) {
  return dayjs(date).toDate();
}
