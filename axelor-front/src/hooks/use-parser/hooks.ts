import set from "lodash/set";
import { createElement, useCallback, useMemo } from "react";

import { DataContext } from "@/services/client/data.types";
import { Hilite } from "@/services/client/meta.types";

import { useViewMeta } from "@/view-containers/views/scope";
import { useFormScope } from "@/views/form/builder/scope";
import {
  EvalContextOptions,
  createEvalContext,
  createScriptContext,
} from "./context";
import { processLegacyTemplate } from "./template-legacy";
import { processReactTemplate } from "./template-react";
import { parseAngularExp, parseExpression } from "./utils";

const isSimple = (expression: string) => {
  return !expression.includes("{{") && !expression.includes("}}");
};

function isReact(template: string | undefined | null) {
  const tmpl = template?.trim();
  return tmpl?.startsWith("<>") && tmpl?.endsWith("</>");
}

export function useExpression(expression: string) {
  return useCallback(
    (context: DataContext, options?: EvalContextOptions) => {
      const func = isSimple(expression)
        ? parseExpression(expression)
        : parseAngularExp(expression);
      const evalContext = createEvalContext(context, options);
      return func(evalContext);
    },
    [expression],
  );
}

export function useTemplate(template: string) {
  const { findField } = useViewMeta();
  const { actionExecutor } = useFormScope();

  return useMemo(() => {
    const Comp = isReact(template)
      ? processReactTemplate(template)
      : processLegacyTemplate(template);
    return (props: { context: DataContext; options?: EvalContextOptions }) => {
      const _context = Object.keys(props.context).reduce((ctx, key) => {
        const value = props.context[key];
        return set(
          ctx,
          key,
          value && typeof value === "object"
            ? Array.isArray(value)
              ? [...value]
              : { ...value }
            : value,
        );
      }, {} as any);

      const {
        helpers,
        execute = actionExecutor.execute.bind(actionExecutor),
        ...options
      } = props.options ?? {};

      const opts = {
        ...options,
        execute,
        helpers: {
          $getField: findField,
          ...helpers,
        },
      };

      const contextWithRecord = { ..._context, record: _context };
      const context = isReact(template)
        ? createScriptContext(contextWithRecord, opts)
        : createEvalContext(contextWithRecord, opts);

      return createElement(Comp, { context });
    };
  }, [actionExecutor, findField, template]);
}

export function useHilites(hilites: Hilite[]) {
  return useCallback(
    (context: DataContext, options?: EvalContextOptions) => {
      const evalContext = createEvalContext(context, options);
      return hilites.filter((x) =>
        parseExpression(x.condition ?? "")(evalContext),
      );
    },
    [hilites],
  );
}
