import { moment } from "@/services/client/l10n";
import { toKebabCase } from "@/utils/names";

function getNextOf(mm, timeUnit) {
  return mm.add(1, timeUnit).startOf(timeUnit);
}

function getPreferredLocale() {
  return "en";
}

export function getFieldName(field) {
  switch (toKebabCase(field?.type)) {
    case "one-to-one":
    case "many-to-one":
      return `${field.name}.${field.targetName}`;
    case "one-to-many":
    case "many-to-many":
      return `${field.name}.id`;
    default:
      return field.name;
  }
}

function fieldNameAppend(fieldName, append) {
  return (fieldName || "").endsWith(`.${append}`)
    ? fieldName
    : `${fieldName}.${append}`;
}

export function getCriteria(criteria, fields, { userId, userGroup } = {}) {
  let { fieldName, operator, timeUnit, value, value2 } = criteria;
  const field = fields[criteria.fieldName];

  function getValue(value) {
    if (value instanceof moment) {
      if ((field?.type || "").toLowerCase() === "date") {
        return value.startOf("day").format("YYYY-MM-DD");
      }
      return value.toDate();
    }
    return value;
  }

  if (operator === "$isCurrentUser") {
    return {
      fieldName: fieldNameAppend(fieldName, "id"),
      operator: "=",
      value: userId,
    };
  }

  if (operator === "$isCurrentGroup") {
    return {
      fieldName: fieldNameAppend(fieldName, "code"),
      operator: "=",
      value: userGroup,
    };
  }

  if (operator === "$inCurrent") {
    const now = moment().locale(getPreferredLocale());
    return {
      operator: "and",
      criteria: [
        {
          fieldName,
          operator: ">=",
          value: getValue(now.clone().startOf(timeUnit)),
        },
        {
          fieldName,
          operator: "<",
          value: getValue(getNextOf(now.clone(), timeUnit)),
        },
      ],
    };
  }

  if (operator === "$inPast") {
    const now = moment().locale(getPreferredLocale());
    return {
      operator: "and",
      criteria: [
        {
          fieldName,
          operator: ">=",
          value: getValue(
            now.clone().subtract(value, timeUnit).startOf(timeUnit)
          ),
        },
        {
          fieldName,
          operator: "<",
          value: getValue(getNextOf(now.clone(), timeUnit)),
        },
      ],
    };
  }

  if (operator === "$inNext") {
    const now = moment().locale(getPreferredLocale());
    return {
      operator: "and",
      criteria: [
        {
          fieldName,
          operator: ">=",
          value: getValue(now.clone().startOf("day")),
        },
        {
          fieldName,
          operator: "<",
          value: getValue(
            getNextOf(now.clone().add(value, timeUnit), timeUnit)
          ),
        },
      ],
    };
  }

  if (["$isTrue", "$isFalse"].includes(operator)) {
    if (operator === "$isFalse") {
      return {
        operator: "or",
        criteria: [
          {
            fieldName,
            operator: "isNull",
          },
          {
            fieldName,
            operator: "=",
            value: false,
          },
        ],
      };
    }
    return {
      fieldName,
      operator: "=",
      value: operator === "$isTrue",
    };
  }

  if (["$isEmpty", "$notEmpty"].includes(operator)) {
    const isEmpty = operator === "$isEmpty";
    return {
      operator: isEmpty ? "or" : "and",
      criteria: [
        {
          fieldName,
          operator: isEmpty ? "isNull" : "notNull",
        },
        {
          fieldName,
          operator: isEmpty ? "=" : "!=",
          value: "",
        },
      ],
    };
  }

  if (field) {
    switch (toKebabCase(field.type)) {
      case "one-to-one":
      case "many-to-one": {
        if (!["isNull", "notNull"].includes(operator)) {
          if (!value) return null;
          let subField = field.targetName;
          if (["in", "notIn"].includes(operator)) {
            subField = "id";
            value = value && value.map((v) => v[subField]);
          }
          fieldName = `${field.name}.${subField}`;
        }
        break;
      }
      case "one-to-many":
      case "many-to-many": {
        if (!["isNull", "notNull"].includes(operator)) {
          if (!value) return null;
          fieldName = `${field.name}.id`;
          if (["in", "notIn"].includes(operator)) {
            value = value && value.map((v) => v.id);
          }
        }
        break;
      }
      case "date":
      case "datetime":
        if (value) {
          switch (operator) {
            case "=":
            case "!=":
            case "between":
            case "notBetween":
              let v1 = value;
              let v2 = ["=", "!="].includes(operator) ? v1 : value2 || v1;

              v1 = getValue(moment(v1).startOf("day"));
              v2 = getValue(getNextOf(moment(v2), "day"));

              const flag = ["=", "between"].includes(operator);

              return {
                operator: flag ? "and" : "or",
                criteria: [
                  {
                    fieldName,
                    operator: flag ? ">=" : "<",
                    value: v1,
                  },
                  {
                    fieldName,
                    operator: flag ? "<" : ">=",
                    value: v2,
                  },
                ],
              };
            case "<":
            case ">=":
              value = getValue(moment(value).startOf("day"));
              break;
            case ">":
              operator = ">=";
              value = getValue(getNextOf(moment(value), "day"));
              break;
            case "<=":
              operator = "<";
              value = getValue(getNextOf(moment(v1), "day"));
              break;
            default:
              break;
          }
        }
        break;
      default:
        break;
    }

    return {
      ...criteria,
      operator,
      fieldName,
      value,
      value2: value2 === null ? undefined : value2,
      timeUnit: !timeUnit ? undefined : timeUnit,
    };
  }

  return criteria;
}
