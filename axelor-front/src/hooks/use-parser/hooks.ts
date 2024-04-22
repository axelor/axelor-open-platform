import { atom, useAtomValue } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import cloneDeep from "lodash/cloneDeep";
import get from "lodash/get";
import set from "lodash/set";
import {
  createElement,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { BaseHilite, Hilite, Schema } from "@/services/client/meta.types";
import {
  findViewItem,
  useViewAction,
  useViewMeta,
} from "@/view-containers/views/scope";
import { FormAtom } from "@/views/form/builder";
import { FormActionHandler, useFormScope } from "@/views/form/builder/scope";

import {
  EvalContextOptions,
  createEvalContext,
  createScriptContext,
} from "./context";
import { processLegacyTemplate } from "./template-legacy";
import { processReactTemplate } from "./template-react";

import { ActionOptions, DefaultActionExecutor } from "@/view-containers/action";
import {
  isLegacyExpression,
  isReactTemplate,
  parseAngularExp,
  parseExpression,
} from "./utils";

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

function useCreateParentContext(formAtom: FormAtom, parent?: boolean) {
  const _parentAtom = useAtomValue(
    useMemo(
      () => selectAtom(formAtom, (x) => x.parent ?? atom(null)),
      [formAtom],
    ),
  );
  const parentAtom = parent ? formAtom : _parentAtom;
  const parentState = useAtomValue(parentAtom);

  const findAttrs = useFindAttrs();
  const $getField = useAtomCallback(
    useCallback(
      (get, set, name: string) => {
        const parent = parentAtom as FormAtom;
        if (parent) {
          const schema = findViewItem(get(parent).meta, name) ?? { name };
          return findAttrs(parent, schema);
        }
      },
      [findAttrs, parentAtom],
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

export function useExpression(expression: string) {
  return useCallback(
    (context: DataContext, options?: EvalContextOptions) => {
      const func = isLegacyExpression(expression)
        ? parseAngularExp(expression)
        : parseExpression(expression);
      const evalContext = createEvalContext(context, options);
      return func(evalContext);
    },
    [expression],
  );
}

export function useTemplate(
  template: string,
  { field, parent: parentFormAtom }: { field?: Schema; parent?: FormAtom } = {},
) {
  const { findItem, findField } = useViewMeta();
  const { actionExecutor, formAtom } = useFormScope();
  const _createParentContext = useCreateParentContext(
    parentFormAtom ?? formAtom,
    Boolean(parentFormAtom),
  );
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

export function useTemplateContext(
  record: DataRecord,
  onRefresh?: () => Promise<void>,
) {
  // state to store updated action values
  const [values, setValues] = useState<DataRecord>({});
  const action = useViewAction();

  const getContext = useCallback(
    () => ({
      ...action.context,
      _model: action.model,
      _viewName: action.name,
      _viewType: action.viewType,
      _views: action.views,
    }),
    [action.context, action.model, action.name, action.viewType, action.views],
  );

  const { context, actionExecutor } = useMemo(() => {
    const $record = { ...record, ...values };
    const context = { ...getContext?.(), ...$record };
    const actionHandler = new FormActionHandler(() => context);

    onRefresh && actionHandler.setRefreshHandler(onRefresh);

    const actionExecutor = new DefaultActionExecutor(actionHandler);
    return { context, actionExecutor };
  }, [getContext, onRefresh, record, values]);

  const execute = useCallback(
    async (action: string, options?: ActionOptions) => {
      const res = await actionExecutor.execute(action, options);
      const values = res?.reduce?.(
        (obj, { values }) => ({
          ...obj,
          ...values,
        }),
        {},
      );
      values && setValues(values);
    },
    [actionExecutor],
  );

  // reset values on record update(fetch)
  useEffect(() => {
    setValues({});
  }, [record]);

  return { context, options: { execute } };
}

export function useHilites<T extends BaseHilite>(hilites?: T[]) {
  return useCallback(
    (context: DataContext, options?: EvalContextOptions) => {
      const evalContext = createEvalContext(context, options);
      return (hilites ?? []).filter((x) =>
        parseExpression(x.condition ?? "")(evalContext),
      );
    },
    [hilites],
  );
}
