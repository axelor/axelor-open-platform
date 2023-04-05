import { NavSelect as NavSelectComponent, TNavSelectItem } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom, useAtomValue } from "jotai";
import { Field, Selection } from "@/services/client/meta.types";
import { getSelectionList } from "./utils";
import { useMemo } from "react";

export function NavSelect({
  schema,
  readonly,
  valueAtom,
  formAtom,
  widgetAtom,
}: FieldProps<string | number>) {
  const { uid, title } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const { attrs } = useAtomValue(widgetAtom);
  const selectionList = schema.selectionList as Selection[];
  const selectionIn = ((attrs as Field).selectionIn ||
    schema["selection-in"] ||
    schema.selectionIn) as string;

  const record = useAtomValue(formAtom).record;
  const list = useMemo(
    () =>
      getSelectionList(
        record,
        (selectionList || []).map(({ title, value, data }) => ({
          title,
          value: value || (data && data.value),
        })),
        selectionIn,
        value
      ),
    [record, selectionList, selectionIn, value]
  );

  return (
    <FieldContainer readonly={readonly}>
      <label htmlFor={uid}>{title}</label>
      <NavSelectComponent
        disabled={readonly}
        items={list}
        value={
          (value || value === 0
            ? { value: `${value}` }
            : value) as TNavSelectItem
        }
        onChange={(e) => setValue((e?.value ?? e) as string | number, true)}
      />
    </FieldContainer>
  );
}
