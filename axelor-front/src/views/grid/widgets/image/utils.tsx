import getValue from "lodash/get";

import { GridColumn } from "@axelor/ui/grid";

import { DataRecord } from "@/services/client/data.types";
import { Schema } from "@/services/client/meta.types";
import { getJSON } from "@/utils/format";

export function getCellValue(record: DataRecord, data: GridColumn) {
  const { name, jsonField, jsonPath } = data as Schema;
  if (!jsonField && name) {
    return record?.[name];
  }
  const json = getJSON(record?.[jsonField]);
  const value = getValue(json, jsonPath);
  return value;
}
