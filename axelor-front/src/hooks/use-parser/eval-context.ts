import moment from "dayjs";
import { get } from "lodash";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { Field } from "@/services/client/meta.types";
import { session } from "@/services/client/session";
import format from "@/utils/format";
import { ActionOptions } from "@/view-containers/action";

export type EvalContextOptions = {
  valid?: (name?: string) => boolean;
  execute?: (name: string, options?: ActionOptions) => Promise<any>;
  readonly?: boolean;
  required?: boolean;
  popup?: boolean;
  fields?: Record<string, Field>;
};

export function createEvalContext(
  context: DataContext,
  options?: EvalContextOptions
) {
  const {
    execute = () => Promise.resolve(),
    valid = () => true,
    readonly = false,
    required = false,
    popup = false,
    fields = {},
  } = options ?? {};

  const helpers = {
    get $user() {
      return session.info?.user.login;
    },
    get $group() {
      return session.info?.user.group;
    },
    get $userId() {
      return session.info?.user.id;
    },
    $get(path: string) {
      return get(context, path);
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
      return execute(name, {
        context,
      });
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
      if (typeof value === "string") {
        return value
          .normalize("NFKD")
          .replace(/\p{Diacritic}/gu, "")
          .replace(/./g, function (c) {
            return (
              {
                "’": "'",
                æ: "ae",
                Æ: "AE",
                œ: "oe",
                Œ: "OE",
                ð: "d",
                Ð: "D",
                ł: "l",
                Ł: "L",
                ø: "o",
                Ø: "O",
              }[c] || c
            );
          });
      }
    },
    __lowercase(value: any) {
      return typeof value === "string" ? value.toLowerCase() : value;
    },
    __uppercase(value: any) {
      return typeof value === "string" ? value.toUpperCase() : value;
    },
  };

  type EvalContext = DataContext & typeof helpers & typeof filters;

  const proxy = new Proxy<EvalContext>(context as any, {
    get(target, p, receiver) {
      if (p in helpers) return helpers[p as keyof typeof helpers];
      if (p in filters) return filters[p as keyof typeof filters];
      return Reflect.get(target, p, receiver);
    },
    set(target, p, newValue, receiver) {
      throw new Error("Cannot update eval context");
    },
  });

  return proxy;
}
