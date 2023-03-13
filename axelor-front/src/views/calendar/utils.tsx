import { View } from "@axelor/ui/scheduler";
import { getStartOf, getNextOf } from "../../utils/date";
import getObjectValue from "lodash/get";

import {
  Scheduler,
  SchedulerProps,
  SchedulerEvent,
  Event,
} from "@axelor/ui/scheduler";
import { getColor } from "./colors";

export function getTimes(date: Date | string, view: View) {
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
