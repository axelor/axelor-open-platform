import get from "lodash/get";
import isEmpty from "lodash/isEmpty";

import { isPlainObject } from "@/services/client/data-utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { moment } from "@/services/client/l10n";
import { Field } from "@/services/client/meta.types";
import { session } from "@/services/client/session";
import format from "@/utils/format";
import { axelor } from "@/utils/globals";
import { ActionOptions } from "@/view-containers/action";

export type ScriptContextOptions = {
  valid?: (name?: string) => boolean;
  execute?: (name: string, options?: ActionOptions) => Promise<any>;
  readonly?: boolean;
  required?: boolean;
  popup?: boolean;
  fields?: Record<string, Field>;
  helpers?: Record<string, any>;
};

export function createScriptContext(
  context: DataContext,
  options?: ScriptContextOptions,
) {
  const {
    execute = () => Promise.resolve(),
    valid = () => true,
    readonly = false,
    required = false,
    popup = false,
    fields = {},
    helpers: moreHelpers = {},
  } = options ?? {};

  const { $getField } = moreHelpers;
  const getField = (name: string) => $getField?.(name) ?? fields[name];
  const noFields = isEmpty(fields) && !$getField;

  const globalHelpers = {
    $alerts: axelor.alerts,
    $dialogs: axelor.dialogs,
    $openView: axelor.openView,
  };

  const helpers = {
    get $user() {
      return session.info?.user?.login;
    },
    get $group() {
      return session.info?.user?.group;
    },
    get $userId() {
      return session.info?.user?.id;
    },
    _t(key: string, ...args: any[]) {
      return i18n.get(key, ...args);
    },
    $get(path: string) {
      const value = get(context, path);
      if (value === undefined) {
        const dotIndex = path.indexOf(".");
        const key = path.substring(0, dotIndex);
        if (isJsonField(key)) {
          const subPath = path.substring(dotIndex + 1);
          const jsonText = context[key];
          const json = tryJson(jsonText);
          if (json) {
            return get(json, subPath);
          }
        }
      }
      return value;
    },
    $moment(date: any) {
      return moment(date);
    },
    $number(value: any) {
      return +value;
    },
    $popup() {
      return popup;
    },
    $contains(iter: any, item: any) {
      return Array.isArray(iter) ? iter.includes(item) : false;
    },
    $json(name: string) {
      const value = helpers.$get(name);
      if (value) {
        try {
          return JSON.parse(value);
        } catch (e) {
          return {};
        }
      }
    },
    $valid(name?: string) {
      return valid(name);
    },
    $invalid(name?: string) {
      return !valid(name);
    },
    $sum(
      items: DataRecord[],
      field: string,
      operation?: string,
      field2?: string,
    ) {
      let total = 0;
      if (items && items.length) {
        items.forEach(function (item) {
          let value = 0;
          let value2 = 0;
          if (field in item) {
            value = +(item[field] || 0);
          }
          if (operation && field2 && field2 in item) {
            value2 = +(item[field2] || 0);
            switch (operation) {
              case "*":
                value = value * value2;
                break;
              case "/":
                value = value2 ? value / value2 : value;
                break;
              case "+":
                value = value + value2;
                break;
              case "-":
                value = value - value2;
                break;
            }
          }
          if (value) {
            total += value;
          }
        });
      }
      return total;
    },
    $readonly() {
      return readonly;
    },
    $required() {
      return required;
    },
    $fmt(name: string) {
      const value = helpers.$get(name);
      return format(value, {
        context,
        props: getField(name),
      });
    },
    $image(fieldName: string, imageName: string) {
      let record = context;
      let model = context._model;

      if (fieldName) {
        const field = getField(fieldName);
        if (field && field.target) {
          record = get(record, fieldName) || {};
          model = field.target;
        }
      }

      const id = record.id || 0;
      const version = record.version || record.$version || 0;

      return id > 0
        ? `ws/rest/${model}/${id}/${imageName}/download?image=true&v=${version}&parentId=${id}&parentModel=${model}`
        : "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
    },
    $execute(action: string, actionContext?: DataContext) {
      const ctx = isPlainObject(actionContext) ? actionContext : {};
      return execute(action, {
        context: { ...context, ...ctx },
      });
    },
    $action(name: string) {
      return (e: React.MouseEvent<HTMLElement>) => {
        e.preventDefault();
        e.stopPropagation();
        execute(name, {
          context,
        });
      };
    },
    ...moreHelpers,
    ...globalHelpers,
  };

  type Context = DataContext & typeof helpers;

  function isJsonField(name: string) {
    return noFields || (getField(name)?.json ?? false);
  }

  function isJsonValue(value: unknown): value is string {
    if (typeof value === "string") {
      const text = value.trim();
      return text.startsWith("{") && text.endsWith("}");
    }
    return false;
  }

  function tryJson(value: unknown) {
    if (isJsonValue(value)) {
      try {
        return JSON.parse(value);
      } catch (e) {
        // Ignore
      }
    }
  }

  return new Proxy<Context>(context as any, {
    get(target, p, receiver) {
      if (p === "record") return receiver;
      if (p in helpers) return helpers[p as keyof typeof helpers];

      // check access of dummy field without `$` prefix and warn
      if (typeof p === "string" && Reflect.has(target, `$${p}`)) {
        console.warn(`Trying to access dummy field "$${p}" as "${p}"?`);
        return undefined;
      }

      const value = Reflect.get(target, p, receiver) ?? get(target, p);
      if (p === "id" && value && value <= 0) {
        return null;
      }
      if (value === undefined && typeof p === "string" && p.startsWith("$")) {
        const key = p.substring(1);
        if (isJsonField(key)) {
          const jsonText = target[key];
          const json = tryJson(jsonText);
          if (json) {
            return json;
          }
        }
      }
      return value;
    },
    set(target, p, newValue, receiver) {
      throw new Error("Cannot update context");
    },
  });
}
