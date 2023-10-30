import { atom, useAtomValue } from "jotai";
import { useMemo } from "react";

import { DataRecord } from "@/services/client/data.types";

import { FieldProps, ValueAtom } from "../../builder";
import { createValueFocusAtom } from "../../builder/atoms";
import { ManyToOne } from "../many-to-one";

const evalVar = (name: string) => {
  if (name.startsWith("record.")) {
    console.warn(
      "Using `record.` prefix is deprecated for `evalXXX` props of `EvalRefSelect` widget:",
      name,
    );
    return name.substring(7);
  }
  return name;
};

export function EvalRefSelect(props: FieldProps<any>) {
  const { formAtom } = props;
  const evalSchema = useMemo(() => {
    return {
      canNew: false,
      canView: false,
      ...props.schema,
      ...props.schema.widgetAttrs,
    };
  }, [props.schema]);

  const { evalTarget, evalTargetName, evalTitle, evalValue } = evalSchema;

  const targetAtom = useMemo(
    () => createValueFocusAtom(formAtom, evalVar(evalTarget)),
    [evalTarget, formAtom],
  );

  const targetNameAtom = useMemo(
    () => createValueFocusAtom(formAtom, evalVar(evalTargetName)),
    [evalTargetName, formAtom],
  );

  const target = useAtomValue(targetAtom);
  const targetName = useAtomValue(targetNameAtom);

  const titleAtom = useMemo(
    () => createValueFocusAtom(formAtom, evalVar(evalTitle)),
    [evalTitle, formAtom],
  );

  const valueAtom = useMemo(
    () => createValueFocusAtom(formAtom, evalVar(evalValue)),
    [evalValue, formAtom],
  );

  const myAtom: ValueAtom<DataRecord> = useMemo(() => {
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
        set(valueAtom, id);
        // if evalValue points to the same field, we need to set it to null first
        set(props.valueAtom, null);
        set(props.valueAtom, id, fireOnChange);
      },
    );
  }, [props.valueAtom, targetName, titleAtom, valueAtom]);

  const schema = useMemo(() => {
    return {
      ...evalSchema,
      target,
      targetName,
    };
  }, [evalSchema, target, targetName]);

  return <ManyToOne {...props} schema={schema} valueAtom={myAtom} />;
}
