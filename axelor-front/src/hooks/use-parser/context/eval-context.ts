import { DataContext } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { moment } from "@/services/client/l10n";
import format from "@/utils/format";
import { unaccent } from "@/utils/sanitize";

import { ScriptContextOptions, createScriptContext } from "./script-context";

export type EvalContextOptions = ScriptContextOptions & {
  components?: Record<string, (props: any) => JSX.Element | null>;
};

export function createEvalContext(
  context: DataContext,
  options: EvalContextOptions = {}
) {
  const { components = {}, helpers = {} } = options;

  const moreHelpers = {
    $component(name: string) {
      return components[name];
    },
  };

  const filters = {
    __date(value: any, formatText: string) {
      if (value && formatText) {
        return moment(value).format(formatText);
      }
      return format(value, { props: { type: "date" } as any });
    },
    __currency(value: any, currency: string, scale = 2) {
      return format(value, {
        props: { scale, type: "decimal", widgetAttrs: { currencyText: currency } } as any,
      });
    },
    __percent(value: any, scale?: string | number) {
      return format(value, { props: { scale, type: "percent" } as any });
    },
    __number(value: any, scale?: string | number) {
      return format(value, { props: { scale, type: "integer" } as any });
    },
    __unaccent(value: any) {
      return unaccent(value);
    },
    __t(key: string, ...args: any[]) {
      return i18n.get(key, ...args);
    },
    __lowercase(value: any) {
      return typeof value === "string" ? value.toLowerCase() : value;
    },
    __uppercase(value: any) {
      return typeof value === "string" ? value.toUpperCase() : value;
    },
  };

  return createScriptContext(context, {
    ...options,
    helpers: {
      ...helpers,
      ...moreHelpers,
      ...filters,
    },
  });
}
