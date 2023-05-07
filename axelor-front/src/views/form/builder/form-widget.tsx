import { useAtomValue, useSetAtom } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import { useCallback, useEffect, useMemo } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { parseExpression } from "@/hooks/use-parser/utils";
import { Schema } from "@/services/client/meta.types";
import { validate } from "@/utils/validate";
import { useViewDirtyAtom } from "@/view-containers/views/scope";

import { parseTemplate } from "@/hooks/use-parser/utils";
import { DataContext } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { createValueAtom, createWidgetAtom } from "./atoms";
import { FieldEditor } from "./form-editors";
import { FieldViewer } from "./form-viewers";
import { useWidget } from "./hooks";
import { useFormScope } from "./scope";
import { FieldProps, ValueAtom, WidgetAtom, WidgetProps } from "./types";

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
  useHandleFieldExpression({ schema, widgetAtom, valueAtom });

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
    const viewerProps = props as FieldProps<any>;
    return (
      <FieldViewer
        {...viewerProps}
        widgetAtom={widgetAtom}
        valueAtom={valueAtom}
      />
    );
  }

  if (schema.editor && valueAtom && (!readonly || schema.editor.viewer)) {
    const editorProps = props as FieldProps<any>;
    return (
      <FieldEditor
        {...editorProps}
        widgetAtom={widgetAtom}
        valueAtom={valueAtom}
      />
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
  valueAtom,
}: {
  schema: Schema;
  widgetAtom: WidgetAtom;
  valueAtom?: ValueAtom<any>;
}) {
  const setWidgetAttrs = useSetAtom(widgetAtom);
  const { recordHandler } = useFormScope();

  const handleBind = useAtomCallback(
    useCallback(
      (get, set, context: DataContext, bind: string) => {
        if (valueAtom) {
          const value = parseTemplate(bind)(context) ?? null;
          set(valueAtom, value, false, false);
        }
      },
      [valueAtom]
    )
  );

  const handleCondition = useCallback(
    (context: DataContext, attr: string, expr: string, negate = false) => {
      const value = Boolean(parseExpression(expr)(context));
      setWidgetAttrs((state) => {
        const attrs = { ...state.attrs, [attr]: negate ? !value : value };
        if (isEqual(state.attrs, attrs)) return state;
        return { ...state, attrs };
      });
    },
    [setWidgetAttrs]
  );

  const handleValidation = useCallback(
    (context: DataContext, expr: string) => {
      const value = parseExpression(expr)(context);
      setWidgetAttrs((state) => {
        const errors = {
          ...state.errors,
          invalid: i18n.get("{0} is invalid", state.attrs.title),
        };
        if (value) Reflect.deleteProperty(errors, "invalid");
        if (isEqual(state.errors, errors)) return state;
        return { ...state, errors };
      });
    },
    [setWidgetAttrs]
  );

  useEffect(() => {
    const {
      showIf,
      hideIf,
      readonlyIf,
      requiredIf,
      validIf,
      collapseIf,
      canNew,
      canEdit,
      canSave,
      canCopy,
      canRemove,
      canDelete,
      canArchive,
      canAttach,
      canSelect,
      bind,
    } = schema;

    const hasExpression =
      showIf ||
      hideIf ||
      readonlyIf ||
      requiredIf ||
      validIf ||
      collapseIf ||
      canNew ||
      canEdit ||
      canSave ||
      canCopy ||
      canRemove ||
      canDelete ||
      canArchive ||
      canAttach ||
      canSelect;

    if (hasExpression || bind) {
      return recordHandler.subscribe((record) => {
        if (bind) handleBind(record, bind);
        if (showIf) handleCondition(record, "hidden", showIf, true);
        if (hideIf) handleCondition(record, "hidden", hideIf);
        if (readonlyIf) handleCondition(record, "readonly", readonlyIf);
        if (requiredIf) handleCondition(record, "required", requiredIf);
        if (collapseIf) handleCondition(record, "collapsed", collapseIf);
        if (validIf) handleValidation(record, validIf);

        if (canNew) handleCondition(record, "canNew", canNew);
        if (canEdit) handleCondition(record, "canEdit", canEdit);
        if (canSave) handleCondition(record, "canSave", canSave);
        if (canCopy) handleCondition(record, "canCopy", canCopy);
        if (canRemove) handleCondition(record, "canRemove", canRemove);
        if (canDelete) handleCondition(record, "canDelete", canDelete);
        if (canArchive) handleCondition(record, "canArchive", canArchive);
        if (canAttach) handleCondition(record, "canAttach", canAttach);
        if (canSelect) handleCondition(record, "canSelect", canSelect);
      });
    }
  }, [
    schema,
    recordHandler,
    setWidgetAttrs,
    handleBind,
    handleCondition,
    handleValidation,
  ]);
}

function Unknown(props: WidgetProps) {
  const { schema } = props;
  return <div>{schema.widget}</div>;
}
