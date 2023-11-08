import { LoadingCache } from "@/utils/cache";
import { parseSafe } from "./parser";

const cache = new LoadingCache();

const makeKey = (...args) => args.map((x) => x || "").join(":");

/**
 * Parse the given template.
 *
 * @param {string} tmpl the tempalte to parse
 * @returns {(context: object) => any}
 */
export const parseTemplate = (tmpl) => {
  return cache.get(makeKey("template", tmpl), () => {
    const text1 = tmpl.replace(/(?!(\$|\\))\{{/g, "$1${");
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
  });
};

/**
 * Parse the given expression.
 *
 * @param {string} expr the expression to parse
 * @returns {(context: object) => any}
 */
export const parseExpression = (expr) => {
  return cache.get(makeKey("expr", expr), () => {
    const fn = parseSafe(expr);
    return (...args) => {
      try {
        return fn(...args);
      } catch {
        console.error(`Invalid expression : ${expr}`);
        return false;
      }
    };
  });
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
    const [filterName, ...values] = a.trim().split(/\s*:\s*/);
    const value = values.join(",");
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

/**
 * Parse the given expression.
 *
 * @param {string} expr the expression to parse
 * @returns {(context: object) => any}
 */
export function parseAngularExp(expr) {
  return cache.get(makeKey("ng-expr", expr), () =>
    parseTemplate(expr.replace(/\{\{(.*?)\}\}/g, resolveFilter))
  );
}
