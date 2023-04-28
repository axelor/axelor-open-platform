import { useAtomValue, useSetAtom } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import { useCallback, useEffect, useMemo } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { parseExpression } from "@/hooks/use-parser/utils";
import { Schema } from "@/services/client/meta.types";
import { validate } from "@/utils/validate";
import { useViewDirtyAtom } from "@/view-containers/views/scope";

import { i18n } from "@/services/client/i18n";
import { createValueAtom, createWidgetAtom } from "./atoms";
import { FieldEditor } from "./form-editors";
import { FieldViewer } from "./form-viewers";
import { useWidget } from "./hooks";
import { useFormScope } from "./scope";
import { ValueAtom, WidgetAtom, WidgetProps } from "./types";

export function FormWidget(props: Omit<WidgetProps, "widgetAtom">) {
  const { schema, formAtom } = props;

  const widgetAtom = useMemo(
    () => createWidgetAtom({ schema, formAtom }),
    [formAtom, schema]
  );

  const dirtyAtom = useViewDirtyAtom();
  const { actionExecutor } = useFormScope();

  const valueAtom = useMemo(
    () =>
      isField(schema)
        ? createValueAtom({ schema, formAtom, dirtyAtom, actionExecutor })
        : undefined,
    [actionExecutor, dirtyAtom, formAtom, schema]
  );

  // eval field expression showIf, hideIf etc
  useHandleFieldExpression({ schema, widgetAtom });

  const hidden = useAtomValue(
    useMemo(() => selectAtom(widgetAtom, (a) => a.attrs.hidden), [widgetAtom])
  );

  const readonly =
    useAtomValue(
      useMemo(
        () => selectAtom(widgetAtom, (a) => a.attrs.readonly),
        [widgetAtom]
      )
    ) || props.readonly;

  if (hidden) {
    return null;
  }

  if (schema.viewer && valueAtom && readonly) {
    return (
      <FieldViewer {...props} widgetAtom={widgetAtom} valueAtom={valueAtom} />
    );
  }

  if (schema.editor && valueAtom && (!readonly || schema.editor.viewer)) {
    return (
      <FieldEditor {...props} widgetAtom={widgetAtom} valueAtom={valueAtom} />
    );
  }

  return <FormItem {...props} widgetAtom={widgetAtom} valueAtom={valueAtom} />;
}

function isField(schema: Schema) {
  const type = schema.type;
  return type === "field" || type === "panel-related";
}

function FormItem(props: WidgetProps & { valueAtom?: ValueAtom<any> }) {
  const { schema, formAtom, widgetAtom, valueAtom, readonly } = props;
  const attrs = useAtomValue(
    useMemo(() => selectAtom(widgetAtom, (a) => a.attrs), [widgetAtom])
  );
  const Comp = useWidget(schema);

  if (attrs.hidden) return null;

  const widgetProps = {
    schema,
    formAtom,
    widgetAtom,
    readonly: readonly || attrs.readonly,
  };

  if (Comp) {
    return valueAtom ? (
      <FormField component={Comp} {...widgetProps} valueAtom={valueAtom} />
    ) : (
      <Comp {...widgetProps} />
    );
  }
  return <Unknown {...widgetProps} />;
}

function FormField({
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
