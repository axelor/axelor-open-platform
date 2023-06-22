import { useMemo } from "react";
import { i18n } from "@/services/client/i18n";
import { toKebabCase } from "@/utils/names";
import { Field } from "@/services/client/meta.types";

type Config = {
  types: string[];
  operators: string[];
};

const typesConfig: Config[] = [
  {
    types: ["enum"],
    operators: ["=", "!=", "isNull", "notNull"],
  },
  {
    types: ["text"],
    operators: ["like", "notLike", "$isEmpty", "$notEmpty"],
  },
  {
    types: ["string"],
    operators: ["=", "!=", "like", "notLike", "$isEmpty", "$notEmpty"],
  },
  {
    types: ["integer", "long", "decimal", "date", "time", "datetime"],
    operators: [
      "=",
      "!=",
      ">=",
      "<=",
      ">",
      "<",
      "between",
      "notBetween",
      "isNull",
      "notNull",
    ],
  },
  {
    types: ["date", "datetime"],
    operators: [
      "=",
      "!=",
      ">=",
      "<=",
      ">",
      "<",
      "between",
      "notBetween",
      "isNull",
      "notNull",
      "$inPast",
      "$inNext",
      "$inCurrent",
    ],
  },
  {
    types: ["boolean"],
    operators: ["$isTrue", "$isFalse"],
  },
  {
    types: ["one-to-one", "many-to-one", "many-to-many"],
    operators: ["like", "notLike", "in", "notIn", "isNull", "notNull"],
  },
  {
    types: ["one-to-many"],
    operators: ["isNull", "notNull"],
  },
];

const types: Record<string, string[]> = typesConfig.reduce(
  (typeSet, { types, operators }) =>
    types.reduce(
      (typeSet, type) => ({ ...typeSet, [type]: operators }),
      typeSet
    ),
  {}
);

const operators = [
  { name: "=", title: i18n.get("equals") },
  { name: "!=", title: i18n.get("not equal") },
  { name: ">", title: i18n.get("greater than") },
  { name: ">=", title: i18n.get("greater or equal") },
  { name: "<", title: i18n.get("less than") },
  { name: "<=", title: i18n.get("less or equal") },
  { name: "in", title: i18n.get("in") },
  { name: "between", title: i18n.get("in range") },
  { name: "notBetween", title: i18n.get("not in range") },
  { name: "notIn", title: i18n.get("not in") },
  { name: "isNull", title: i18n.get("is null") },
  { name: "notNull", title: i18n.get("is not null") },
  { name: "like", title: i18n.get("contains") },
  { name: "notLike", title: i18n.get("doesn't contain") },
  { name: "$isTrue", title: i18n.get("is true") },
  { name: "$isFalse", title: i18n.get("is false") },
  { name: "$isEmpty", title: i18n.get("is empty") },
  { name: "$notEmpty", title: i18n.get("is not empty") },
  { name: "$inPast", title: i18n.get("in the past") },
  { name: "$inNext", title: i18n.get("in the next") },
  { name: "$inCurrent", title: i18n.get("in the current") },
  { name: "$isCurrentUser", title: i18n.get("is current user") },
  { name: "$isCurrentGroup", title: i18n.get("is current group") },
];

const EXTRA_OPERATORS_BY_TARGET = {
  "com.axelor.auth.db.User": ["$isCurrentUser"],
  "com.axelor.auth.db.Group": ["$isCurrentGroup"],
};

export function useField(fields?: Field[], name?: string) {
  return useMemo(() => {
    const field = fields?.find((item) => item.name === name);
    
    let type = toKebabCase(field?.type!);

    if (field && field.selectionList) {
      type = "enum";
    }

    let typeOperators = types[type] || [];

    if (field?.target) {
      if (!field?.targetName) {
        typeOperators = ["isNull", "notNull"];
      } else {
        typeOperators = typeOperators.concat(
          (EXTRA_OPERATORS_BY_TARGET as any)[field.target] || []
        );
      }
    }

    return {
      type,
      field,
      options: operators.filter((item) => typeOperators.includes(item.name)),
    };
  }, [name, fields]);
}
