import { useAtom } from "jotai";

import { NavSelect as NavSelectComponent, TNavSelectItem } from "@axelor/ui";

import { FieldControl, FieldProps } from "../../builder";
import { useSelectionList } from "../selection/hooks";

export function NavSelect(props: FieldProps<string | number>) {
  const { schema, readonly, widgetAtom, valueAtom } = props;
  const [value, setValue] = useAtom(valueAtom);
  const items = useSelectionList({ value, widgetAtom, schema });

  return (
    <FieldControl {...props}>
      <NavSelectComponent
        disabled={readonly}
        items={items as TNavSelectItem[]}
        value={
          (value || value === 0
            ? { value: `${value}` }
            : value) as TNavSelectItem
        }
        onChange={(e) => setValue((e?.value ?? e) as string | number, true)}
      />
    </FieldControl>
  );
}
