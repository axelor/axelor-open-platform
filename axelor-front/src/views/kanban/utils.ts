import { KanbanColumn } from "./types";

function getIndex(list: any[], key: string, value: any) {
  return list.findIndex((item: any) => item[key] === value);
}

export function getColumnIndex(columns: KanbanColumn[], columnName?: string) {
  return getIndex(columns, "name", columnName);
}

export function getRecordIndex(recordId: string | number, records: any) {
  return getIndex(records, "id", recordId);
}

export function getColumnRecords(columns: KanbanColumn[], columnName?: string) {
  const columnIndex = getColumnIndex(columns, columnName);
  return columns[columnIndex].records || [];
}

function reorder(list: any[], startIndex: number, endIndex: number) {
  const result = Array.from(list);
  const [removed] = result.splice(startIndex, 1);
  result.splice(endIndex, 0, removed);
  return result;
}

export function reorderCards({
  columns,
  sourceColumn,
  destinationColumn,
  sourceIndex,
  destinationIndex,
}: {
  columns: KanbanColumn[];
  sourceColumn: KanbanColumn;
  destinationColumn: KanbanColumn;
  sourceIndex: number;
  destinationIndex: number;
}) {
  const current = getColumnRecords(columns, sourceColumn.name)?.slice();
  const next = getColumnRecords(columns, destinationColumn.name)?.slice();
  const target = current[sourceIndex];

  // moving to same list
  if (sourceColumn.name === destinationColumn.name) {
    const reordered = reorder(current, sourceIndex, destinationIndex);
    const newColumns = columns.map((c) => ({ ...c }));
    newColumns[getColumnIndex(columns, sourceColumn.name)].records = reordered;
    return newColumns;
  }

  // moving to different list
  current.splice(sourceIndex, 1);
  next.splice(destinationIndex, 0, target);
  const newColumns = columns.map((c) => ({ ...c }));
  newColumns[getColumnIndex(columns, sourceColumn.name)].records = current;
  newColumns[getColumnIndex(columns, destinationColumn.name)].records = next;
  return newColumns;
}
