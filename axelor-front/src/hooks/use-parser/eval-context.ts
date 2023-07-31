import get from "lodash/get";
import set from "lodash/set";
import { MouseEvent } from "react";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { moment } from "@/services/client/l10n";
import { Field } from "@/services/client/meta.types";
import { session } from "@/services/client/session";
import format, { getJSON } from "@/utils/format";
import { unaccent } from "@/utils/sanitize";
import { ActionOptions } from "@/view-containers/action";

export type EvalContextOptions = {
  valid?: (name?: string) => boolean;
  execute?: (name: string, options?: ActionOptions) => Promise<any>;
  readonly?: boolean;
  required?: boolean;
  popup?: boolean;
  fields?: Record<string, Field>;
  components?: Record<string, (props: any) => JSX.Element | null>;
};

export function createEvalContext(
  context: DataContext,
  options?: EvalContextOptions,
  template?: boolean
) {
  const {
    execute = () => Promise.resolve(),
    valid = () => true,
    readonly = false,
    required = false,
    popup = false,
    fields = {},
    components = {}, // custom components
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
    $component(name: string) {
      return components[name];
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
        props: { scale, currency, type: "decimal" } as any,
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

  type EvalContext = DataContext & typeof helpers & typeof filters;

  const $context = template
    ? Object.keys(context).reduce((ctx, key) => {
        const value = context[key];
        return set(
          ctx,
          key,
          value && typeof value === "object"
            ? Array.isArray(value)
              ? [...value]
              : { ...value }
            : value
        );
      }, {} as any)
    : context;
  const proxy = new Proxy<EvalContext>($context as any, {
    get(target, p, receiver) {
      if (p in helpers) return helpers[p as keyof typeof helpers];
      if (p in filters) return filters[p as keyof typeof filters];
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
      throw new Error("Cannot update eval context");
    },
  });

  return proxy;
}
