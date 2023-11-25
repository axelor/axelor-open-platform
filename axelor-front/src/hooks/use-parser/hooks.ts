import { atom, useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import get from "lodash/get";
import set from "lodash/set";
import { createElement, useCallback, useMemo } from "react";

import { DataContext } from "@/services/client/data.types";
import { Hilite } from "@/services/client/meta.types";
import { findViewItem, useViewMeta } from "@/view-containers/views/scope";
import { FormAtom } from "@/views/form/builder";
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

function useCreateParentContext(formAtom: FormAtom) {
  const parentAtom = useAtomValue(
    useMemo(
      () => selectAtom(formAtom, (x) => x.parent ?? atom(null)),
      [formAtom],
    ),
  );

  const parentState = useAtomValue(parentAtom);

  return useCallback(() => {
    if (parentState) {
      const { record, model: _model, fields, meta } = parentState;
      const $getField = (name: string) => findViewItem(meta, name);
      const ctx = { ...record, _model };
      return createScriptContext(ctx, {
        fields,
        helpers: {
          $getField,
        },
      });
    }
  }, [parentState]);
}

export function isReactTemplate(template: string | undefined | null) {
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
  const { findItem } = useViewMeta();
  const { actionExecutor, formAtom } = useFormScope();
  const _createParentContext = useCreateParentContext(formAtom);

  return useMemo(() => {
    const Comp = isReactTemplate(template)
      ? processReactTemplate(template)
      : processLegacyTemplate(template);
    return (props: { context: DataContext; options?: EvalContextOptions }) => {
      const _context = Object.keys(props.context).reduce((ctx, key) => {
        const value = props.context[key];
        const prev = get(ctx, key);
        return set(
          ctx,
          key,
          value && typeof value === "object"
            ? Array.isArray(value)
              ? [...value]
              : { ...prev, ...value }
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
          $getField: findItem,
          ...helpers,
        },
      };

      const ctx = { ..._context, _createParentContext };
      const context = isReactTemplate(template)
        ? createScriptContext(ctx, opts)
        : createEvalContext(ctx, opts);

      return createElement(Comp, { context });
    };
  }, [_createParentContext, actionExecutor, findItem, template]);
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
