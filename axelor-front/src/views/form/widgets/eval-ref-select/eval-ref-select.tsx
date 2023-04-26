import { DataRecord } from "@/services/client/data.types";
import { atom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { useMemo } from "react";
import { FieldProps, ValueAtom } from "../../builder";
import { ManyToOne } from "../many-to-one";

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
    () => focusAtom(formAtom, (o) => o.path(evalTarget)),
    [evalTarget, formAtom]
  );

  const targetNameAtom = useMemo(
    () => focusAtom(formAtom, (o) => o.path(evalTargetName)),
    [evalTargetName, formAtom]
  );

  const target = useAtomValue(targetAtom);
  const targetName = useAtomValue(targetNameAtom);

  const titleAtom = useMemo(
    () => focusAtom(formAtom, (o) => o.path(evalTitle)),
    [evalTitle, formAtom]
  );

  const valueAtom = useMemo(
    () => focusAtom(formAtom, (o) => o.path(evalValue)),
    [evalValue, formAtom]
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
      }
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
