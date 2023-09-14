import { GanttView } from "@/services/client/meta.types";
import { getRandomColor } from "../calendar/colors";
import { DataRecord } from "@/services/client/data.types";

export function formatRecord(view: GanttView, record: DataRecord) {
  const {
    [view.taskParent!]: parent,
    [view.taskStart!]: startDate,
    [view.taskEnd!]: endDate,
    [view.taskDuration!]: duration,
    [view.taskProgress!]: progress,
    [view.taskSequence!]: sequence,
    [view.startToStart!]: startToStart = [],
    [view.startToFinish!]: startToFinish = [],
    [view.finishToStart!]: finishToStart = [],
    [view.finishToFinish!]: finishToFinish = [],
    ...values
  } = record;
  if (view.taskUser && record[view.taskUser]) {
    values.$color = getRandomColor(record[view.taskUser].id);
  }
  return {
    ...values,
    sequence,
    data: record,
    parent,
    startDate,
    endDate,
    duration,
    progress,
    startToStart,
    startToFinish,
    finishToStart,
    finishToFinish,
  };
}

export function transformRecord(view: GanttView, record: DataRecord) {
  const {
    parent,
    startDate,
    endDate,
    duration,
    startToStart,
    startToFinish,
    finishToStart,
    finishToFinish,
    progress,
    sequence,
    data,
    ...values
  } = record;
  return {
    ...values,
    [view.taskParent!]: parent,
    [view.taskStart!]: startDate,
    [view.taskEnd!]: endDate,
    [view.taskDuration!]: duration,
    [view.taskProgress!]: progress,
    [view.taskSequence!]: sequence,
    [view.startToStart!]: startToStart,
    [view.startToFinish!]: startToFinish,
    [view.finishToStart!]: finishToStart,
    [view.finishToFinish!]: finishToFinish,
  };
}
