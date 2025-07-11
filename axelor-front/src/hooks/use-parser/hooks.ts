import { atom, useAtomValue } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import cloneDeep from "lodash/cloneDeep";
import getValue from "lodash/get";
import setValue from "lodash/set";
import {
  createContext,
  createElement,
  memo,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { findViewItem } from "@/utils/schema";
import { BaseHilite, Schema, View } from "@/services/client/meta.types";
import { useViewAction, useViewMeta } from "@/view-containers/views/scope";
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
import { createContextParams } from "@/views/form/builder/utils";

const TemplateScope = createContext<DataContext>({});

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
        const parentFormAtom = parentAtom as FormAtom;
        if (parentFormAtom) {
          const schema = findViewItem(get(parentFormAtom).meta, name) ?? {
            name,
          };
          return findAttrs(parentFormAtom, schema);
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

export function useTemplateContext() {
  return useContext(TemplateScope);
}

const TemplateElement = memo(function TemplateElement({
  template,
}: {
  template: string;
}) {
  const Comp = useMemo(
    () =>
      isReactTemplate(template)
        ? processReactTemplate(template)
        : processLegacyTemplate(template),
    [template],
  );
  const context = useTemplateContext();
  return createElement(Comp, { context });
});

export function TemplateRenderer({
  template,
  field,
  parent: parentFormAtom,
  context: propsContext,
  options: propsOptions,
}: {
  template: string;
  context: DataContext;
  field?: Schema;
  parent?: FormAtom;
  options?: EvalContextOptions;
}) {
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

  const context = useMemo(() => {
    // Deep clone of all fields excluding dotted fields
    const _context = Object.keys(propsContext)
      .filter((key) => !key.includes("."))
      .reduce((cur, key) => {
        return Object.assign(cur, { [key]: cloneDeep(propsContext[key]) });
      }, {} as any);

    // Merge dot fields (ie some.foo.bar) into object (ie product[foo])
    Object.keys(propsContext)
      .filter((key) => key.includes("."))
      .reduce((cur, key) => {
        const value = cloneDeep(propsContext[key]);
        const prev = getValue(cur, key);
        return setValue(
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
    } = propsOptions ?? {};

    const opts = {
      ...options,
      execute,
      helpers: {
        $getField,
        ...helpers,
      },
    };

    const ctx = { ..._context, _createParentContext };
    return isReactTemplate(template)
      ? createScriptContext(ctx, opts)
      : createEvalContext(ctx, opts);
  }, [
    propsContext,
    propsOptions,
    $getField,
    template,
    _createParentContext,
    actionExecutor,
  ]);

  return createElement(TemplateScope.Provider, {
    value: context,
    children: createElement(TemplateElement, {
      template,
    }),
  });
}

export function usePrepareTemplateContext(
  record: DataRecord,
  options: { view: View; onRefresh?: () => Promise<void> },
) {
  const { view, onRefresh } = options;
  // state to store updated action values
  const [values, setValues] = useState<DataRecord>({});
  const action = useViewAction();

  const getContext = useCallback(
    () => createContextParams(view, action),
    [view, action],
  );

  const { context, actionExecutor } = useMemo(() => {
    const $record = { ...record, ...values };
    const $context = { ...getContext?.(), ...$record };
    const actionHandler = new FormActionHandler(() => $context);

    if (onRefresh) {
      actionHandler.setRefreshHandler(onRefresh);
    }

    return {
      context: $context,
      actionExecutor: new DefaultActionExecutor(actionHandler),
    };
  }, [getContext, onRefresh, record, values]);

  const execute = useCallback(
    async (_action: string, _options?: ActionOptions) => {
      const res = await actionExecutor.execute(_action, _options);
      const _values = res?.reduce?.(
        (obj, resultItem) => ({
          ...obj,
          ...resultItem.values,
        }),
        {},
      );
      if (_values) {
        setValues(_values);
      }
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
