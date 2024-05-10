import omit from "lodash/omit";

import { GanttView } from "@/services/client/meta.types";
import { getRandomColor } from "../calendar/colors";
import { DataRecord } from "@/services/client/data.types";

const defaultValues: Record<string, any> = {
  taskDuration: 1,
  startToStart: [],
  startToFinish: [],
  finishToStart: [],
  finishToFinish: [],
};

const transformKeys: Record<string, string> = {
  taskParent: "parent",
  taskStart: "startDate",
  taskEnd: "endDate",
  taskDuration: "duration",
  taskProgress: "progress",
  taskSequence: "sequence",
  startToStart: "startToStart",
  startToFinish: "startToFinish",
  finishToStart: "finishToStart",
  finishToFinish: "finishToFinish",
};

export function formatRecord(view: GanttView, record: DataRecord) {
  const values: DataRecord = {
    data: record,
  };

  if (view.taskUser && record[view.taskUser]) {
    values.$color = getRandomColor(record[view.taskUser].id);
  }

  const formattedValues = Object.keys(transformKeys).reduce((vals, k) => {
    const viewKey = view[k as keyof GanttView] as string;
    return viewKey
      ? { ...vals, [transformKeys[k]]: record[viewKey] ?? defaultValues[k] }
      : vals;
  }, values);

  return {
    ...omit(
      record,
      Object.keys(transformKeys).map(
        (k) => view[k as keyof GanttView] as string,
      ),
    ),
    ...formattedValues,
  } as DataRecord;
}

export function transformRecord(view: GanttView, record: DataRecord) {
  const keys = Object.values(transformKeys);

  return {
    ...omit(record, [...keys, "data", "$color"]),
    ...Object.keys(transformKeys).reduce((vals, k) => {
      const viewKey = view[k as keyof GanttView] as string;
      if (viewKey) {
        const valueKey = transformKeys[k];
        return { ...vals, [viewKey]: record[valueKey] };
      }
      return vals;
    }, {}),
  } as DataRecord;
}
