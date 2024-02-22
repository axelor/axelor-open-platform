import { DataRecord } from "@/services/client/data.types.ts";
import { Field, JsonField } from "@/services/client/meta.types.ts";
import _ from "lodash";

/**
 * Extract the field value from the given DataRecord
 * 
 * @param record the DataRecord to extract the field value
 * @param field the field definition
 */
export function getFieldValue(
  record: DataRecord,
  field: Field,
) {
  let fieldName = field.name;
  let data = record;
  if ((field as JsonField)?.jsonField) {
    const { jsonField, jsonPath } = field as JsonField;
    data = getJSON(_.get(record, jsonField as string));
    fieldName = jsonPath as string;
  }
  return _.get(data, fieldName);
}

/**
 * Converts a JSON string into a value or object.
 *
 * @param jsonStr the json string to convert
 *
 * Return the converted value. If an error occurs if the json isn't valid, return empty object.
 */
export function getJSON(jsonStr: string) {
  try {
    return JSON.parse(jsonStr);
  } catch (e) {
    return {};
  }
}
