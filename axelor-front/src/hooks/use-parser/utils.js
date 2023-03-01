import { parseSafe } from "./parser";

export const parseTemplate = (str) => {
  const text1 = str.replace(/(?!(\$|\\))\{{/g, "$1${");
  const text = text1.replace(/(?!(\$|\\))\}}/g, "}");

  if (!text.includes("${")) {
    return () => text;
  }
  const resFn = parseSafe("`" + text + "`");
  return (...args) => {
    try {
      const result = resFn(...args);
      return result && result.replace(/undefined/gi, "").replace(/null/g, "");
    } catch {
      console.error(`Invalid template line : ${text}`);
      return null;
    }
  };
};

export const parseExpression = (str) => {
  const fn = parseSafe(str);
  return (...args) => {
    try {
      return fn(...args);
    } catch {
      console.error(`Invalid expression : ${str}`);
      return false;
    }
  };
};

export const isObject = (v) => v.startsWith("{");
export const isArray = (v) => v.startsWith("[");

const formatStringToCamelCase = (str) => {
  const splitted = str.split("-");
  if (splitted.length === 1) return splitted[0];
  return (
    splitted[0] +
    splitted
      .slice(1)
      .map((word) => word[0].toUpperCase() + word.slice(1))
      .join("")
  );
};

export const getStyleObject = (str) => {
  const style = {};
  str.split(";").forEach((el) => {
    const [property, value] = el.split(":");
    if (!property) return;

    const formattedProperty = formatStringToCamelCase(property.trim());
    style[formattedProperty] = value.trim().replace("!important", "");
  });

  return style;
};

export const stringToObject = (str) => {
  let classes = {};
  if (str.includes("{") && str.includes("}")) {
    str = str.slice(0, str.lastIndexOf("}") + 1);
  }
  const arr = str.replace("{", "").replace("}", "").split(",");
  arr.forEach((a) => {
    const [code, value] = a.split(":");
    classes[code.trim()] = "{{" + value + "}}";
  });
  return classes;
};

const SUPPORTED_FILTERS = [
  "date",
  "percent",
  "uppercase",
  "lowercase",
  "currency",
  "number",
  "accent",
  "unaccent",
  "t",
];

function resolveFilter(match) {
  let arr = match.replace("{{", "").replace("}}", "").split("|");
  let code = arr.shift().trim();
  arr.forEach((a) => {
    const [filterName, ...values] = a.trim().split(":");
    const value = values.join(":");
    if (filterName && SUPPORTED_FILTERS.includes(filterName)) {
      if (value) {
        code = `__${filterName.trim()}(${code},${value})`;
      } else {
        code = `__${filterName.trim()}(${code})`;
      }
    }
  });
  return "{{" + code + "}}";
}

export function parseAngularExp(str) {
  return parseTemplate(str.replace(/\{\{(.*?)\}\}/g, resolveFilter));
}
