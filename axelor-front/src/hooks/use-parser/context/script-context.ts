import { MouseEvent } from "react";
import get from "lodash/get";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { moment } from "@/services/client/l10n";
import { i18n } from "@/services/client/i18n";
import { Field } from "@/services/client/meta.types";
import { session } from "@/services/client/session";
import format, { getJSON } from "@/utils/format";
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
  options?: ScriptContextOptions
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
        const subPath = path.substring(dotIndex + 1);
        const jsonText = context[key];
        if (typeof jsonText === "string") {
          const json = getJSON(jsonText);
          return get(json, subPath);
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
      field2?: string
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
        props: fields[name],
      });
    },
    $image(fieldName: string, imageName: string) {
      let record = context;
      let model = context._model;

      if (fieldName) {
        let field = fields[fieldName];
        if (field && field.target) {
          record = record[fieldName] || {};
          model = field.target;
        }
      }

      const id = record.id || 0;
      const version = record.version || record.$version || 0;

      return id > 0
        ? `ws/rest/${model}/${id}/${imageName}/download?image=true&v=${version}&parentId=${id}&parentModel=${model}`
        : "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
    },
    $action(name: string) {
      return (e: MouseEvent<HTMLElement>) => {
        e.preventDefault();
        e.stopPropagation();
        execute(name, {
          context,
        });
      };
    },
    ...moreHelpers,
  };

  type Context = DataContext & typeof helpers;

  return new Proxy<Context>(context as any, {
    get(target, p, receiver) {
      if (p in helpers) return helpers[p as keyof typeof helpers];
      const value = Reflect.get(target, p, receiver) ?? get(target, p);
      if (p === "id" && value && value <= 0) {
        return null;
      }
      if (value === undefined && typeof p === "string" && p.startsWith("$")) {
        const key = p.substring(1);
        const jsonText = target[key];
        if (typeof jsonText === "string") {
          const json = getJSON(jsonText);
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
