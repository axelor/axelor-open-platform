import { Dayjs, ManipulateType, OpUnitType } from "dayjs";
import { Field, GridView } from "@/services/client/meta.types";
import { SearchState } from "./scope";
import { Criteria } from "@/services/client/data.types";
import { Filter } from "@/services/client/data.types";
import { toKebabCase } from "@/utils/names";
import { moment } from "@/services/client/l10n";
import * as FormatUtils from "@/utils/format";

function getNextOf(mm: Dayjs, timeUnit: OpUnitType) {
  return mm.add(1, timeUnit as ManipulateType).startOf(timeUnit);
}

function getTimeFormat(field?: Field) {
  return FormatUtils.getTimeFormat({ props: field });
}

function getDateFormat(field?: Field) {
  return FormatUtils.getDateFormat({ props: field });
}

function getDateTimeFormat(field?: Field) {
  return FormatUtils.getDateTimeFormat({ props: field });
}

function stripOperator(
  value: any,
  field: Field,
  formatter: (value: any, field?: Field) => any = (e) => e
) {
  let match = /(<)(.*)(<)(.*)/.exec(value);
  if (match) {
    return {
      operator: "between",
      value: formatter(match[4], field),
      value2: formatter(match[2], field),
    };
  }

  match = /(<=?|>=?|!?=)(.*)/.exec(value);
  if (match) {
    const [, op, value] = match;
    const val = formatter(value.trim(), field);
    return {
      operator: op,
      value: val,
    };
  }

  return { operator: "=", value: formatter(value, field) };
}

function toMoment(val: string, field: Field) {
  let granularity: OpUnitType = "month";
  let monthFirst = /M+.+D+/.test(getDateFormat(field));
  let format = monthFirst ? "MM/DD" : "MM/YYYY";
  let operator;

  if (/^\D*\d{4,}\D*$/.test(val)) {
    format = "YYYY";
    granularity = "year";
  } else if (/^\D*\d{1,2}\D*$/.test(val)) {
    format = "MM";
  } else if (/^\D*\d{4,}\D+\d{1,2}\D*$/.test(val)) {
    format = "YYYY/MM";
  } else if (/^\D*\d{1,2}\D+\d{1,2}\D*$/.test(val)) {
    format = monthFirst ? "MM/DD" : "DD/MM";
    granularity = "day";
  } else if (/^\D*\d{1,2}\D+\d{4,}\D*$/.test(val)) {
    format = "MM/YYYY";
  } else if (/^\D*\d+\D+\d+\D+\d+\D*$/.test(val)) {
    format = getDateFormat(field);
    granularity = "day";
    operator = "=";
  } else if (/^\D*\d+\D+\d+\D+\d+\D+\d+\D*$/.test(val)) {
    format = getDateTimeFormat(field).replace(/\W+m+$/, "");
    granularity = "hour";
  } else if (/^\D*\d+\D+\d+\D+\d+\D+\d+\D+\d+\D*$/.test(val)) {
    format = getDateTimeFormat(field);
    granularity = "minute";
  } else if (/^\D*\d+\D+\d+\D+\d+\D+\d+\D+\d+\D+\d+\D*$/.test(val)) {
    format = getDateTimeFormat({ ...field, seconds: true } as Field);
    granularity = "second";
  }

  return [val ? moment(val, format) : moment(), granularity, operator];
}

function toTimeMoment(val: any, field: Field) {
  let format = getTimeFormat(field);
  let granularity = "minute";
  if (/^\D*\d+\D*$/.test(val)) {
    format = (format.match(/\w+/) || [])[0] || format;
    granularity = "hour";
  } else if (/^\D*\d+\D+\d+\D+\d+\D*$/.test(val)) {
    format = getTimeFormat({ seconds: true } as Field);
    granularity = "second";
  }
  return [val ? moment(val, format) : moment(), granularity];
}

function getSearchCriteria(
  fields: Record<string, Field>,
  items: GridView["items"],
  values: SearchState
): Filter[] {
  let options = {};

  const getFilterCriteria = (_field: Field, value: any) => {
    const item = (items || []).find((item) => item?.name === _field?.name);
    const field = { ...item, ..._field } as Field;
    let type = toKebabCase(field.type || "");
    let op = "like";
    if (field.selection) {
      type = "selection";
    }

    switch (type) {
      case "decimal":
      case "number":
      case "long":
      case "integer":
        options = stripOperator(value, field, (value) => {
          return isNaN(value) ? 0 : Number(value);
        });
        break;
      case "boolean":
        op = "=";
        value = !/f|n|false|no|0/.test(value);
        break;
      case "time":
      case "date":
      case "datetime":
        const mappers = {
          time: (v?: Dayjs) =>
            v && v.format(getTimeFormat({ seconds: true } as Field)),
          date: (v?: Dayjs) => v && v.format("YYYY-MM-DD"),
          datetime: (v?: Dayjs) => {
            const d: any = v && v.toDate();
            return d && !isNaN(d) && d instanceof Date ? d.toISOString() : null;
          },
        };
        const toValue = mappers[type];
        const {
          operator: _operator,
          value: _value,
          value2,
        } = stripOperator(value, field);
        const [$value, granularity, $operator] = (
          type === "time" ? toTimeMoment : toMoment
        )(_value, field) as [Dayjs, OpUnitType, Filter["operator"]];
        if ($value) {
          const $op = _operator === $operator ? $operator : _operator;
          switch ($op) {
            case "=":
            case "<":
            case ">=":
              op = $op;
              value = toValue(moment($value).startOf(granularity));
              break;
            case ">":
              op = ">=";
              value = toValue(getNextOf(moment($value), granularity));
              break;
            case "<=":
              op = "<";
              value = toValue(getNextOf(moment($value), granularity));
              break;
            case "!=":
            case "between":
              const hasNot = $op === "!=";
              let $v1: any = $value;
              let $v2: any = (
                value2 ? toMoment(value2, field)[0] : $v1.clone()
              ) as Dayjs;

              if ($v1 > $v2 && !hasNot) {
                [$v1, $v2] = [$v2, $v1];
              }

              $v1 = toValue($v1.startOf(granularity));
              $v2 = toValue(getNextOf($v2, granularity));

              return {
                operator: hasNot ? "or" : "and",
                criteria: [
                  {
                    fieldName: field.name,
                    operator: hasNot ? "<" : ">=",
                    value: $v1,
                  },
                  {
                    fieldName: field.name,
                    operator: hasNot ? ">=" : "<",
                    value: $v2,
                  },
                ],
              };
          }
        }
        break;
      case "enum":
      case "selection":
        if (!(field.widget || "").toLowerCase().startsWith("multi")) {
          op = "=";
        }
        break;
    }
    return {
      fieldName: field.name,
      value,
      operator: op,
      ...options,
    };
  };

  const getFilterValue = (name: string, value: any) => {
    const field = { ...(fields[name] || { name }) };
    if (toKebabCase(field.type) === "many-to-one") {
      field.name = `${name}.${field.targetName}`;
    }
    return getFilterCriteria(field, value) as Filter;
  };

  return Object.keys(values)
    .filter((x) => values[x])
    .map((key) => getFilterValue(key, values[key]));
}

export function getSearchFilter(
  fields: Record<string, Field>,
  viewItems: GridView["items"],
  search: SearchState
): Criteria | null {
  return Object.keys(search || {}).length > 0
    ? {
        operator: "and",
        criteria: getSearchCriteria(fields, viewItems, search),
      }
    : null;
}
