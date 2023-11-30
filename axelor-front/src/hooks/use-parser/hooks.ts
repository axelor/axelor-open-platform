import { atom, useAtomValue } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import get from "lodash/get";
import set from "lodash/set";
import cloneDeep from "lodash/cloneDeep";
import { createElement, useCallback, useMemo } from "react";

import { DataContext } from "@/services/client/data.types";
import { Hilite, Schema } from "@/services/client/meta.types";
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

function useFindAttrs() {
  return useAtomCallback(
    useCallback((get, set, formAtom: FormAtom, field: Schema) => {
      const { uid, name } = field;
      const { states, statesByName } = get(formAtom);
      const { attrs: attrsById } = states[uid] ?? {};
      const { attrs: attrsByName } = statesByName[name!] ?? {};
      return { ...field, ...attrsByName, ...attrsById };
    }, []),
  );
}

function useCreateParentContext(formAtom: FormAtom) {
  const parentAtom = useAtomValue(
    useMemo(
      () => selectAtom(formAtom, (x) => x.parent ?? atom(null)),
      [formAtom],
    ),
  );

  const parentState = useAtomValue(parentAtom);

  const findAttrs = useFindAttrs();
  const $getField = useAtomCallback(
    useCallback(
      (get, set, name: string) => {
        const { parent } = get(formAtom);
        if (parent) {
          const schema = findViewItem(get(parent).meta, name) ?? { name };
          return findAttrs(parent, schema);
        }
      },
      [findAttrs, formAtom],
    ),
  );

  return useCallback(() => {
    if (parentAtom && parentState) {
      const { record, model: _model, fields } = parentState;
      const ctx = { ...record, _model };
      return createScriptContext(ctx, {
        fields,
        helpers: {
          $getField,
        },
      });
    }
  }, [$getField, parentAtom, parentState]);
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

export function useTemplate(template: string, field?: Schema) {
  const { findItem, findField } = useViewMeta();
  const { actionExecutor, formAtom } = useFormScope();
  const _createParentContext = useCreateParentContext(formAtom);
  const findAttrs = useFindAttrs();
  const $getField = useCallback(
    (name: string) => {
      if (field && field.name === name) {
        const serverField = findField(name);
        const serverType = field?.serverType || serverField?.type;
        const more = serverType ? { serverType } : {};
        const schema = {
          ...serverField,
          ...field,
          ...field?.widgetAttrs,
          ...more,
        };
        return findAttrs(formAtom, schema);
      }
      return findAttrs(formAtom, findItem(name) ?? { name });
    },
    [field, findAttrs, findField, findItem, formAtom],
  );

  return useMemo(() => {
    const Comp = isReactTemplate(template)
      ? processReactTemplate(template)
      : processLegacyTemplate(template);
    return (props: { context: DataContext; options?: EvalContextOptions }) => {
      // Deep clone of all fields excluding dotted fields
      const _context = Object.keys(props.context)
        .filter((key) => !key.includes("."))
        .reduce((cur, key) => {
          return Object.assign(cur, { [key]: cloneDeep(props.context[key]) });
        }, {} as any);

      // Merge dot fields (ie some.foo.bar) into object (ie product[foo])
      Object.keys(props.context)
        .filter((key) => key.includes("."))
        .reduce((cur, key) => {
          const value = cloneDeep(props.context[key]);
          const prev = get(cur, key);
          return set(
            cur,
            key,
            value && typeof value === "object"
              ? Array.isArray(value)
                ? [...value]
                : { ...prev, ...value }
              : value,
          );
        }, _context);

      const {
        helpers,
        execute = actionExecutor.execute.bind(actionExecutor),
        ...options
      } = props.options ?? {};

      const opts = {
        ...options,
        execute,
        helpers: {
          $getField,
          ...helpers,
        },
      };

      const ctx = { ..._context, _createParentContext };
      const context = isReactTemplate(template)
        ? createScriptContext(ctx, opts)
        : createEvalContext(ctx, opts);

      return createElement(Comp, { context });
    };
  }, [$getField, _createParentContext, actionExecutor, template]);
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
