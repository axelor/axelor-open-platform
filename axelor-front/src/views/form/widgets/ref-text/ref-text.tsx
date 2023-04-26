import { DataRecord } from "@/services/client/data.types";
import { atom } from "jotai";
import { useMemo } from "react";
import { FieldProps, ValueAtom } from "../../builder";
import { ManyToOne } from "../many-to-one";

export function RefText(props: FieldProps<any>) {
  const { valueAtom } = props;
  const schema = useMemo(() => {
    return {
      canNew: false,
      canView: false,
      ...props.schema,
      ...props.schema.widgetAttrs,
    };
  }, [props.schema]);

  const { targetName } = schema;

  const textAtom = useMemo(() => {
    return atom(
      (get) => {
        const value = get(valueAtom);
        return {
          [targetName]: value,
        };
      },
      (get, set, value: DataRecord, fireOnChange?: boolean) => {
        set(valueAtom, value?.[targetName] || null, fireOnChange);
      }
    ) as ValueAtom<DataRecord>;
  }, [targetName, valueAtom]);

  return <ManyToOne {...props} schema={schema} valueAtom={textAtom} />;
}
