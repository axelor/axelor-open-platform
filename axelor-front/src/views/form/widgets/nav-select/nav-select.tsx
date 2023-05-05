import { NavSelect as NavSelectComponent, TNavSelectItem } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom, useAtomValue } from "jotai";
import { useSelectionList } from "../selection/hooks";

export function NavSelect({
  schema,
  readonly,
  valueAtom,
  formAtom,
  widgetAtom,
}: FieldProps<string | number>) {
  const { uid, showTitle = true } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;
  const list = useSelectionList({ value, widgetAtom, schema });

  return (
    <FieldContainer readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      <NavSelectComponent
        disabled={readonly}
        items={list as TNavSelectItem[]}
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
