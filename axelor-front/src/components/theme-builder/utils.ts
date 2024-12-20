import Color from "color";
import deepGet from "lodash/get";
import deepSet from "lodash/set";
import isNil from "lodash/isNil";
import isEmpty from "lodash/isEmpty";
import { produce } from "immer";

import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";
import { elements } from "./theme-elements";

function isThemeColor(color: string) {
  return [
    "primary",
    "secondary",
    "success",
    "info",
    "warning",
    "danger",
    "light",
    "dark",
    "gray_dark",
    "gray_100",
    "gray_200",
    "gray_300",
    "gray_400",
    "gray_500",
    "gray_600",
    "gray_700",
    "gray_800",
    "gray_900",
  ].includes(color);
}

export function isValidCssValue(property?: string, value?: any) {
  if (!value || !property) return true;
  if (property === "color" && isThemeColor(value)) {
    return true;
  }
  if (window.CSS && CSS?.supports) {
    return CSS.supports(property, value);
  }
  return true;
}

function isValidColor(color?: string | null): boolean {
  if (isThemeColor(color ?? "")) return true;
  try {
    return color ? Color(color) != null : false;
  } catch (e) {
    return false;
  }
}

export function validateThemeOptions(options: ThemeOptions) {
  return produce(options, (draft) => {
    for (const element of elements) {
      element.editors?.forEach((editor) => {
        editor.props
          ?.filter((prop) => prop.type === "color")
          .forEach((prop) => {
            const color = deepGet(draft, prop.path);
            // allow css variables
            // invalid colors should be ignored
            if (color && !color.includes("var(--") && !isValidColor(color)) {
              deepSet(draft, prop.path, undefined);
            }
          });
      });
    }
  });
}

export function compactTheme(value: any): any {
  if (Array.isArray(value)) {
    value = value.map((x) => compactTheme(x)).filter((x) => !isNil(x));
  }
  if (value && typeof value === "object") {
    value = Object.entries(value)
      .map(([k, v]) => [k, compactTheme(v)])
      .filter(([k, v]) => !isNil(v))
      .reduce((prev, [k, v]) => ({ ...prev, [k]: v }), {});
  }
  return isEmpty(value) ? undefined : value;
}
