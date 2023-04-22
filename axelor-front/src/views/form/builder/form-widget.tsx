import { atom, useAtomValue, useSetAtom } from "jotai";
import { focusAtom } from "jotai-optics";
import { selectAtom, useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import { useCallback, useEffect, useMemo } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { parseExpression } from "@/hooks/use-parser/utils";
import { isDummy } from "@/services/client/data-utils";
import { Schema } from "@/services/client/meta.types";
import { validate } from "@/utils/validate";
import { useViewDirtyAtom } from "@/view-containers/views/scope";

import { i18n } from "@/services/client/i18n";
import { createWidgetAtom } from "./atoms";
import { FieldEditor } from "./form-editors";
import { FieldViewer } from "./form-viewers";
import { useLazyWidget } from "./hooks";
import { useFormScope } from "./scope";
import { ValueAtom, WidgetAtom, WidgetProps } from "./types";

export function FormWidget(props: Omit<WidgetProps, "widgetAtom">) {
  const { schema, formAtom, readonly } = props;

  const widgetAtom = useMemo(
    () => createWidgetAtom({ schema, formAtom }),
    [formAtom, schema]
  );

  const type = schema.type;
  const attrs = useAtomValue(
    useMemo(() => selectAtom(widgetAtom, (a) => a.attrs), [widgetAtom])
  );
  const { loading, Comp } = useLazyWidget(schema);

  // eval field expression showIf, hideIf etc
  useHandleFieldExpression({ schema, widgetAtom });

  if (attrs.hidden) return null;
  if (loading) return null;

  const widgetProps = {
    schema,
    formAtom,
    widgetAtom,
    readonly: readonly || attrs.readonly,
  };

  if (Comp) {
    return ["field", "panel-related"].includes(type!) ? (
      <FormField component={Comp} {...widgetProps} />
    ) : (
      <Comp {...widgetProps} />
    );
  }
  return <Unknown {...widgetProps} />;
}

function FormField({
  component: Comp,
  ...props
}: WidgetProps & { component: React.ElementType }) {
  const { schema, formAtom, readonly } = props;
  const name = schema.name!;
  const onChange = schema.onChange;
  const dirtyAtom = useViewDirtyAtom();
  const setDirty = useSetAtom(dirtyAtom);

  const { actionExecutor } = useFormScope();

  const valueAtom = useMemo(() => {
    const lensAtom = focusAtom(formAtom, (o) => {
      let lens = o.prop("record");
      let path = name.split(".");
      let next = path.shift();
      while (next) {
        lens = lens.valueOr({});
        lens = lens.prop(next);
        next = path.shift();
      }
      return lens;
    });
    return atom(
      (get) => get(lensAtom) as any,
      (get, set, value: any, fireOnChange: boolean = false) => {
        const prev = get(lensAtom);
        if (prev !== value) {
          const dirty = !isDummy(name);
          set(lensAtom, value);
          set(formAtom, (prev) => ({ ...prev, dirty: prev.dirty ?? dirty }));
          if (dirty) {
            setDirty(true);
          }
        }
        if (onChange && fireOnChange) {
          actionExecutor.execute(onChange, {
            context: {
              _source: name,
            },
          });
        }
      }
    );
  }, [actionExecutor, formAtom, name, onChange, setDirty]);

  if (readonly && schema.viewer) {
    return <FieldViewer {...props} valueAtom={valueAtom} />;
  }

  if (schema.editor && (!readonly || schema.editor.viewer)) {
    return <FieldEditor {...props} valueAtom={valueAtom} />;
  }

  return <ValidatingField {...props} component={Comp} valueAtom={valueAtom} />;
}

function ValidatingField({
  component: Comp,
  ...props
}: WidgetProps & { component: React.ElementType; valueAtom: ValueAtom<any> }) {
  const { schema, formAtom, widgetAtom, valueAtom } = props;

  const valueCheck = useAtomCallback(
    useCallback(
      (get, set) => {
        const value = get(valueAtom);
        const prev = get(widgetAtom);
        const record = get(formAtom).record;

        let errors = validate(value, {
          props: {
            ...schema,
            ...prev.attrs,
          } as any,
          context: record,
        });

        if (isEqual(prev.errors ?? {}, errors ?? {})) return;

        set(widgetAtom, (prev) => ({ ...prev, errors }));
      },
      [formAtom, schema, valueAtom, widgetAtom]
    )
  );

  // trigger validation on value change
  useAtomValue(valueAtom);
  useAsyncEffect(async (signal) => {
    signal.aborted || valueCheck();
  });

  const invalidAtom = useMemo(
    () =>
      selectAtom(
        widgetAtom,
        ({ errors = {} }) => Object.values(errors).filter(Boolean).length > 0
      ),
    [widgetAtom]
  );
  const invalid = useAtomValue(invalidAtom);

  return <Comp {...props} invalid={invalid} />;
}

function useHandleFieldExpression({
  schema,
  widgetAtom,
}: {
  schema: Schema;
  widgetAtom: WidgetAtom;
}) {
  const setWidgetAttrs = useSetAtom(widgetAtom);
  const { recordHandler } = useFormScope();

  useEffect(() => {
    const { showIf, hideIf, readonlyIf, requiredIf, validIf } = schema;
    const hasExpression =
      showIf || hideIf || readonlyIf || requiredIf || validIf;

    if (hasExpression) {
      return recordHandler.subscribe((record) => {
        setWidgetAttrs((state) => {
          const attrs = { ...state.attrs };
          let errors = state.errors;

          if (showIf) {
            attrs.hidden = !parseExpression(showIf)(record);
          } else if (hideIf) {
            attrs.hidden = parseExpression(hideIf)(record);
          }

          if (readonlyIf) {
            attrs.readonly = parseExpression(readonlyIf)(record);
          }

          if (requiredIf) {
            attrs.required = parseExpression(requiredIf)(record);
          }

          if (validIf) {
            errors = {
              ...errors,
              invalid: i18n.get("{0} is invalid", attrs.title),
            };
            const valid = parseExpression(validIf)(record);
            if (valid) delete errors.invalid;
          }

          if (!isEqual(attrs, state.attrs) || !isEqual(errors, state.errors)) {
            return { ...state, attrs, errors };
          }

          return state;
        });
      });
    }
  }, [schema, recordHandler, setWidgetAttrs]);
}

function Unknown(props: WidgetProps) {
  const { schema } = props;
  return <div>{schema.widget}</div>;
}
