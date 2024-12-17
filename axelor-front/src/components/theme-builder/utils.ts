import Color from "color";
import deepGet from "lodash/get";
import deepSet from "lodash/set";
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
  return CSS.supports(property, value);
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
