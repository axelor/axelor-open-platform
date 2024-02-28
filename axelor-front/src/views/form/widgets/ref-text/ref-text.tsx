import { DataRecord } from "@/services/client/data.types";
import { atom } from "jotai";
import { useMemo } from "react";
import { FieldProps, ValueAtom, WidgetAtom } from "../../builder";
import { ManyToOne } from "../many-to-one";

export function RefText(props: FieldProps<any>) {
  const { valueAtom, widgetAtom: _widgetAtom } = props;
  const schema = useMemo(() => {
    return {
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
          [targetName]: value ?? "",
        };
      },
      (get, set, value: DataRecord, fireOnChange?: boolean) => {
        set(valueAtom, value?.[targetName] || null, fireOnChange);
      },
    ) as ValueAtom<DataRecord>;
  }, [targetName, valueAtom]);

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

  return (
    <ManyToOne
      {...props}
      schema={schema}
      valueAtom={textAtom}
      widgetAtom={widgetAtom}
    />
  );
}
