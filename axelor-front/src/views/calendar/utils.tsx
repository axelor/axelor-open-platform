import { View, SchedulerEvent } from "@axelor/ui/scheduler";
import getObjectValue from "lodash/get";
import dayjs, { ManipulateType } from "dayjs";

import { i18n } from "@/services/client/i18n";
import { getStartOf, getNextOf } from "../../utils/date";
import { getColor } from "./colors";

const { get: t } = i18n;

export function getTimes(date: Date | string, view: View) {
  if (view === "month") {
    return {
      start: getStartOf(getStartOf(date, "month"), "week"),
      end: getNextOf(getNextOf(date, "month"), "week"),
    };
  }

  return { start: getStartOf(date, view), end: getNextOf(date, view) };
}

export function getEventFilters(events: SchedulerEvent[], colorField: any) {
  const { name, target, targetName, selectionList } = colorField;
  const getValue = (obj: any) => {
    const val = getObjectValue(obj, name);
    return target ? val && val.id : val;
  };
  const getLabel = (val: any) => {
    if (selectionList) {
      return (
        (selectionList.find((x: any) => `${x.value}` === `${val}`) || {})
          .title || val
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
  [key: string]: (date: Date) => string;
}

export function formatDate(date: Date, unit: ManipulateType) {
  const DATE_FORMATTERS: DateFormatMap = {
    month: (date) => dayjs(date).format("MMMM YYYY"),
    week: (date) =>
      t(
        "{0} – Week {1}",
        dayjs(date).format("MMMM YYYY"),
        dayjs(date).format("w")
      ),
    day: (date) => dayjs(date).format("LL"),
  };

  const formatter = DATE_FORMATTERS[unit] || DATE_FORMATTERS.day;
  return formatter(date);
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
