import { GanttData, GanttRecord } from "@axelor/ui/gantt";
import uniq from "lodash/uniq";
import getValue from "lodash/get";

import { updateJsonFieldValue } from "@/services/client/data-utils";
import { MetaData } from "@/services/client/meta";
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
  const names = Object.keys(transformKeys)
    .map((k) => view[k as keyof GanttView])
    .concat((view.items ?? []).map((item) => item.name))
    .filter(Boolean) as string[];
  return uniq(names);
}

function getJsonFieldMapping(
  jsonFields: MetaData["jsonFields"],
  fieldName: string,
) {
  const dotIndex = fieldName.indexOf(".");
  if (dotIndex <= 0) return;

  const jsonField = fieldName.substring(0, dotIndex);
  const jsonPath = fieldName.substring(dotIndex + 1);
  return jsonFields?.[jsonField]?.[jsonPath]
    ? { jsonField, jsonPath }
    : undefined;
}

export function getJsonFieldNames(
  view: GanttView,
  jsonFields?: MetaData["jsonFields"],
) {
  const names = getFieldNames(view);
  const resolvedNames = names.map((name) => {
    const mapping = getJsonFieldMapping(jsonFields, name);
    return mapping ? mapping.jsonField : name;
  });
  return uniq(resolvedNames);
}

/**
 * Read a field value from a record, handling JSON custom fields.
 * For dotted paths like "attrs.startDate", prefer the parent JSON field
 * because in-place updates refresh that blob without repopulating dotted aliases.
 */
export function getFieldValue(
  record: DataRecord,
  fieldName: string,
  jsonFields?: MetaData["jsonFields"],
) {
  const mapping = getJsonFieldMapping(jsonFields, fieldName);
  if (mapping) {
    const raw = record[mapping.jsonField];
    if (raw != null) {
      try {
        const json = typeof raw === "string" ? JSON.parse(raw) : raw;
        return getValue(json, mapping.jsonPath);
      } catch {
        // Fall back to any literal dotted alias if the JSON blob is malformed.
      }
    }
  }
  return record[fieldName];
}

/**
 * Format the given record into a gantt record
 *
 * @param view the gantt view containing the mapping definitions
 * @param record the record to format
 */
export function formatRecord(
  view: GanttView,
  record: DataRecord,
  jsonFields?: MetaData["jsonFields"],
) {
  const values: GanttRecord = {
    taskData: record as unknown as GanttData,
    id: record.id!,
  };

  if (view.taskUser) {
    const userValue = getFieldValue(record, view.taskUser, jsonFields);
    if (userValue) {
      values.$color = getRandomColor(userValue.id);
    }
  }

  const formattedValues = Object.keys(transformKeys).reduce((vals, k) => {
    const viewKey = view[k as keyof GanttView] as string;
    return viewKey
      ? {
          ...vals,
          [transformKeys[k]]:
            getFieldValue(record, viewKey, jsonFields) ?? defaultValues[k],
        }
      : vals;
  }, values);

  if (view.items && view.items[0].name) {
    formattedValues.name = getFieldValue(
      record,
      view.items[0].name,
      jsonFields,
    );
  }

  return { ...formattedValues } as GanttRecord;
}

/**
 * Transform the given gantt data into record values according to the view mapping definitions
 *
 * @param view the gantt view containing the mapping definitions
 * @param data the data to transform
 * @param source the original record (needed for JSON field merging)
 */
export function transformRecord(
  view: GanttView,
  data: GanttRecord,
  source?: DataRecord,
  jsonFields?: MetaData["jsonFields"],
) {
  const result: DataRecord = {};
  const jsonUpdates: Record<string, Record<string, unknown>> = {};

  for (const k of Object.keys(transformKeys)) {
    const viewKey = view[k as keyof GanttView] as string;
    const valueKey = transformKeys[k] as keyof GanttRecord;
    if (!viewKey || data[valueKey] === undefined) continue;

    const mapping = getJsonFieldMapping(jsonFields, viewKey);
    if (mapping) {
      (jsonUpdates[mapping.jsonField] ??= {})[mapping.jsonPath] =
        data[valueKey];
    } else {
      result[viewKey] = data[valueKey];
    }
  }

  for (const [jsonField, updates] of Object.entries(jsonUpdates)) {
    updateJsonFieldValue(result, jsonField, source ?? result, updates);
  }

  return result;
}
