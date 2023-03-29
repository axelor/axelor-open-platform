import { toKebabCase } from "@/utils/names";
import React from "react";

const typesConfig = [
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

const types = typesConfig.reduce(
  (typeSet, { types, operators }) =>
    types.reduce(
      (typeSet, type) => ({ ...typeSet, [type]: operators }),
      typeSet
    ),
  {}
);

const operators = [
  { name: "=", title: "equals" },
  { name: "!=", title: "not equal" },
  { name: ">", title: "greater than" },
  { name: ">=", title: "greater or equal" },
  { name: "<", title: "less than" },
  { name: "<=", title: "less or equal" },
  { name: "in", title: "in" },
  { name: "between", title: "in range" },
  { name: "notBetween", title: "not in range" },
  { name: "notIn", title: "not in" },
  { name: "isNull", title: "is null" },
  { name: "notNull", title: "is not null" },
  { name: "like", title: "contains" },
  { name: "notLike", title: "doesn't contain" },
  { name: "$isTrue", title: "is true" },
  { name: "$isFalse", title: "is false" },
  { name: "$isEmpty", title: "is empty" },
  { name: "$notEmpty", title: "is not empty" },
  { name: "$inPast", title: "in the past" },
  { name: "$inNext", title: "in the next" },
  { name: "$inCurrent", title: "in the current" },
  { name: "$isCurrentUser", title: "is current user" },
  { name: "$isCurrentGroup", title: "is current group" },
];

const EXTRA_OPERATORS_BY_TARGET = {
  "com.axelor.auth.db.User": ["$isCurrentUser"],
  "com.axelor.auth.db.Group": ["$isCurrentGroup"],
};

export function useField(fields, name, t = (e) => e) {
  return React.useMemo(() => {
    const field = fields.find((item) => item.name === name);

    let type = toKebabCase(field?.type);

    if (field && field.selectionList) {
      type = "enum";
    }

    let typeOperators = types[type] || [];

    if (field?.target) {
      if (!field?.targetName) {
        typeOperators = ["isNull", "notNull"];
      } else {
        typeOperators = typeOperators.concat(
          EXTRA_OPERATORS_BY_TARGET[field.target] || []
        );
      }
    }

    return {
      type,
      field,
      options: operators
        .filter((item) => typeOperators.includes(item.name))
        .map((item) => ({ ...item, title: t(item.title) })),
    };
  }, [t, name, fields]);
}
