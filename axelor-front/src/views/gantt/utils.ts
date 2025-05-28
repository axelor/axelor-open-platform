import { GanttData, GanttRecord } from "@axelor/ui/gantt";
import uniq from "lodash/uniq";

import { GanttView } from "@/services/client/meta.types";
import { DataRecord } from "@/services/client/data.types";
import { getRandomColor } from "../calendar/colors";

const defaultValues: Record<string, any> = {
  taskDuration: 1,
  startToStart: [],
  startToFinish: [],
  finishToStart: [],
  finishToFinish: [],
};

const transformKeys: Record<string, string> = {
  taskUser: "taskUser",
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

export function getFieldNames(view: GanttView) {
  return uniq(
    Object.keys(transformKeys)
      .map((k) => view[k as keyof GanttView])
      .concat((view.items ?? []).map((item) => item.name))
      .filter(Boolean),
  ) as string[];
}

/**
 * Format the given record into a gantt record
 *
 * @param view the gantt view containing the mapping definitions
 * @param record the record to format
 */
export function formatRecord(view: GanttView, record: DataRecord) {
  const values: GanttRecord = {
    taskData: record as unknown as GanttData,
    id: record.id!,
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

  if (view.items && view.items[0].name) {
    formattedValues.name = record[view.items[0].name];
  }

  return { ...formattedValues } as GanttRecord;
}

/**
 * Transform the given gantt data into record values according to the view mapping definitions
 *
 * @param view the gantt view containing the mapping definitions
 * @param data the data to transform
 */
export function transformRecord(view: GanttView, data: GanttRecord) {
  return {
    ...Object.keys(transformKeys).reduce((vals, k) => {
      const viewKey = view[k as keyof GanttView] as string;
      const valueKey = transformKeys[k] as keyof GanttRecord;
      if (viewKey && data[valueKey] !== undefined) {
        return { ...vals, [viewKey]: data[valueKey] };
      }
      return vals;
    }, {}),
  } as DataRecord;
}
