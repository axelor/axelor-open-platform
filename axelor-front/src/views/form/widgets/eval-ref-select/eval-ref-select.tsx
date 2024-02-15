import { produce } from "immer";
import { atom, useAtomValue } from "jotai";
import { useMemo } from "react";

import { DataRecord } from "@/services/client/data.types";
import { focusAtom } from "@/utils/atoms";
import { deepGet, deepSet } from "@/utils/objects";

import { SuggestBox } from "..";
import { FieldProps, FormAtom, ValueAtom, WidgetAtom } from "../../builder";

const evalVar = (name: string) => {
  if (name.startsWith("record.")) {
    console.warn(
      "Using `record.` prefix is deprecated for `x-eval-*` attributes of `EvalRefSelect` widget:",
      name,
    );
    return name.substring(7);
  }
  return name;
};

function evalAtom(formAtom: FormAtom, prop: string) {
  const name = evalVar(prop);
  return focusAtom(
    formAtom,
    (form) => deepGet(form.record, name),
    (form, value) => {
      return produce(form, (draft) => {
        deepSet(draft.record, name, value);
      });
    },
  );
}

export function EvalRefSelect(props: FieldProps<any>) {
  const { formAtom, widgetAtom: _widgetAtom } = props;
  const evalSchema = useMemo(() => {
    return {
      ...props.schema,
      ...props.schema.widgetAttrs,
    };
  }, [props.schema]);

  const { evalTarget, evalTargetName, evalTitle, evalValue } = evalSchema;

  const targetAtom = useMemo(
    () => evalAtom(formAtom, evalTarget),
    [evalTarget, formAtom],
  );

  const targetNameAtom = useMemo(
    () => evalAtom(formAtom, evalTargetName),
    [evalTargetName, formAtom],
  );

  const target = useAtomValue(targetAtom);
  const targetName = useAtomValue(targetNameAtom);

  const titleAtom = useMemo(
    () => evalAtom(formAtom, evalTitle),
    [evalTitle, formAtom],
  );

  const _valueAtom = useMemo(
    () => evalAtom(formAtom, evalValue),
    [evalValue, formAtom],
  );

  const valueAtom: ValueAtom<DataRecord> = useMemo(() => {
    return atom(
      (get) => {
        const value = get(titleAtom);
        return {
          [targetName]: value,
        };
      },
      (get, set, value, fireOnChange) => {
        const record = value || {};
        const id = record.id && record.id > 0 ? record.id : null;
        set(titleAtom, record[targetName] || null);
        set(_valueAtom, id);
        // if evalValue points to the same field, we need to set it to null first
        set(props.valueAtom, null);
        set(props.valueAtom, id, fireOnChange);
      },
    );
  }, [props.valueAtom, targetName, titleAtom, _valueAtom]);

  const widgetAtom: WidgetAtom = useMemo(() => {
    return atom(
      (get) => {
        const value = get(_widgetAtom);
        const { attrs, ...rest } = value;
        return {
          attrs: {
            ...attrs,
            canNew: false,
            canView: false,
          },
          ...rest,
        };
      },
      (get, set, value) => {
        set(_widgetAtom, value);
      },
    );
  }, [_widgetAtom]);

  const schema = useMemo(() => {
    return {
      ...evalSchema,
      target,
      targetName,
    };
  }, [evalSchema, target, targetName]);

  return (
    <SuggestBox
      {...props}
      schema={schema}
      valueAtom={valueAtom}
      widgetAtom={widgetAtom}
    />
  );
}
