import get from "lodash/get";
import isNumber from "lodash/isNumber";

import { GridColumn } from "@axelor/ui/grid";

import {
  Criteria,
  DataRecord,
  Filter,
  FilterOp,
} from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { Moment, l10n, moment } from "@/services/client/l10n";
import { MetaData } from "@/services/client/meta";
import { Field, SavedFilter, Widget } from "@/services/client/meta.types";
import { session } from "@/services/client/session";
import { getNextOf } from "@/utils/date";
import { toKebabCase } from "@/utils/names";
import { AdvancedSearchState } from "./types";

const ADVANCED_SEARCH_VIEWS = new Set(["grid", "cards", "kanban"]);

function fieldNameAppend(fieldName?: string, append?: string) {
  return (fieldName || "").endsWith(`.${append || ""}`)
    ? fieldName
    : `${fieldName}.${append}`;
}

export function isAdvancedSearchView(view?: string) {
  return ADVANCED_SEARCH_VIEWS.has(view ?? "");
}

export function getCriteria(
  criteria: Filter,
  fields?: MetaData["fields"],
  jsonFields?: MetaData["jsonFields"]
) {
  let { fieldName, timeUnit = "d", value, value2 } = criteria;
  let operator = criteria.operator as any;
  const jsonField = get(jsonFields || {}, fieldName!);
  const field = fields?.[fieldName!] || jsonField;
  const user = session.info!.user;
  const userId = user?.id;
  const userGroup = user?.group;

  function getValue(value: Date | Moment) {
    if (typeof value === "object" && value instanceof moment) {
      if (((field as any)?.type || "").toLowerCase() === "date") {
        return (value as Moment).startOf("day").format("YYYY-MM-DD");
      }
      return (value as Moment)?.toDate ? (value as Moment).toDate() : value;
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
    const now = moment().locale(l10n.getLocale());
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
    const now = moment().locale(l10n.getLocale());
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
    const now = moment().locale(l10n.getLocale());
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
    switch (toKebabCase(field.type as string)) {
      case "one-to-one":
      case "many-to-one": {
        if (!["isNull", "notNull"].includes(operator)) {
          if (!value) return null;
          let subField = field.targetName;
          if (["in", "notIn"].includes(operator)) {
            subField = "id";
            value = value && value.map((v: DataRecord) => v.id);
          }
          fieldName = `${jsonField ? fieldName : field.name}.${subField}`;
        }
        break;
      }
      case "one-to-many":
      case "many-to-many": {
        if (["in", "notIn"].includes(operator)) {
          if (!value) return null;
          fieldName = `${jsonField ? fieldName : field.name}.id`;
          value = value && value.map((v: DataRecord) => v.id);
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
      timeUnit: !criteria.timeUnit ? undefined : timeUnit,
    };
  }

  return criteria;
}

export function getFreeSearchCriteria(
  text?: string,
  items?: Widget[],
  fields?: MetaData["fields"]
) {
  if (text) {
    const filters: Filter[] = [];
    const number = +text;

    items?.forEach((item) => {
      let fieldName = "";
      let operator = "like";
      let value: any = text;

      const field = fields?.[item?.name || ""] || item;
      const { name, targetName, jsonField } = field as Field;

      if (item.hidden || (item as GridColumn).visible === false) return;

      const type = toKebabCase((item as Field).serverType || field.type);
      switch (type) {
        case "integer":
        case "long":
        case "decimal":
          if (isNaN(number) || !isNumber(number)) return;
          if (
            type === "integer" &&
            (number > 2147483647 || number < -2147483648)
          )
            return;
          fieldName = name;
          operator = "=";
          value = number;
          break;
        case "text":
        case "string":
          fieldName = name;
          break;
        case "one-to-one":
        case "many-to-one":
          if (jsonField) {
            fieldName = name;
          } else if (targetName) {
            fieldName = name + "." + targetName;
          }
          break;
        case "boolean":
          if (/^(t|f|y|n|true|false|yes|no)$/.test(text)) {
            fieldName = name;
            operator = "=";
            value = /^(t|y|true|yes)$/.test(text);
          }
          break;
        default:
          break;
      }

      if (!fieldName) return;

      filters.push({
        fieldName: fieldName,
        operator: operator as FilterOp,
        value: value,
      });
    });

    return filters;
  }
  return [];
}

export function getContextFieldFilter(
  contextField: AdvancedSearchState["contextField"],
  contextFields: AdvancedSearchState["contextFields"]
): (Filter & { title?: string }) | null {
  const { name, value } = contextField ?? {};
  const { id } = value ?? {};
  const field = contextFields?.find((field) => field.name === name);
  const targetName = field?.targetName ?? "";
  const trTargetName = `$t:${targetName}`;
  const title = value?.[trTargetName] ?? value?.[targetName];
  return name && id >= 0
    ? { fieldName: `${name}.id`, operator: "=", value: id, title }
    : null;
}

export function findContextField(filter: Criteria, contextFields: Field[]) {
  const { operator, criteria } = filter;
  if (operator === "and" && criteria?.length) {
    const {
      fieldName,
      operator,
      value: id,
      title,
    } = criteria[0] as Filter & { title?: string };
    const baseFieldName = fieldName?.replace(/.id$/, "");
    const field = contextFields.find((field) => field.name === baseFieldName);
    if (field && operator === "=" && id) {
      const { name, targetName = "name" } = field;
      const value = { id, [targetName]: title };
      return { name, value };
    }
  }

  return null;
}

export function prepareAdvanceSearchQuery(
  state: AdvancedSearchState,
  hasEditorApply?: boolean
) {
  const {
    domains,
    filters,
    archived: _archived,
    editor,
    contextField,
    contextFields,
    fields = {},
    jsonFields = {},
  } = state;
  const _domains = domains
    ?.filter((d) => d.checked)
    .map(({ checked, ...d }) => ({
      ...d,
    }));

  const getEditorCriteria = (criteria: Criteria["criteria"]) =>
    (criteria || [])
      .map((c) => getCriteria(c as Filter, fields, jsonFields) as Filter)
      .filter((c) => c);

  const getEditor = (filter: SavedFilter) => {
    const editor: Criteria = JSON.parse(filter.filterCustom);
    if (editor.criteria) {
      editor.criteria = getEditorCriteria(editor.criteria);
      if (!editor.operator) {
        editor.operator = "and";
      }
    }
    return editor;
  };

  const {
    id,
    title,
    operator: editorOperator,
    criteria: editorCriteria,
  } = editor ?? {};
  let operator = editorOperator ?? "and";
  let criteria: Criteria["criteria"] = getEditorCriteria(editorCriteria);

  const contextFieldFilter = getContextFieldFilter(contextField, contextFields);

  if (contextFieldFilter) {
    criteria = [
      contextFieldFilter,
      ...(criteria?.length ? [{ operator, criteria }] : []),
    ];
    operator = "and";
  }

  const $filters = filters?.filter(
    (f) => f.id !== id && f.checked && f.filterCustom
  );

  if (!hasEditorApply) {
    if ($filters?.length) {
      criteria = [
        ...criteria,
        {
          operator,
          criteria: $filters.map(getEditor),
        },
      ];
    }
  }

  const allfilters = [...(_domains || []), ...($filters || [])];
  const totalFilters = allfilters.length;
  const totalCriteria =
    criteria?.filter((c) => (c as Filter).operator)?.length ?? 0;
  const filterTitle = id ? title : "";
  const searchTextLabel =
    filterTitle ||
    (totalFilters
      ? totalFilters === 1
        ? allfilters[0]?.title
        : i18n.get("Filters ({0})", totalFilters)
      : "") ||
    (totalCriteria ? i18n.get("Custom ({0})", totalCriteria) : "");

  return {
    ...state,
    appliedContextField: contextField,
    searchTextLabel,
    searchText: "",
    query: {
      _archived,
      _domains,
      operator,
      criteria,
    },
  };
}
