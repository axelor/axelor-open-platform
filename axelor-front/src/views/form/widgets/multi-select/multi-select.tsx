import { useAtom } from "jotai";
import { useCallback } from "react";

import { SelectOptionProps } from "@/components/select";
import { Selection as SelectionType } from "@/services/client/meta.types";

import { FieldProps } from "../../builder";
import { Selection, SelectionTag } from "../selection";

export function MultiSelect(
  props: FieldProps<string | number | null> & {
    multiple?: boolean;
  },
) {
  const { schema, multiple = true, readonly, valueAtom } = props;
  const { colorField = "color" } = schema;
  const [value, setValue] = useAtom(valueAtom);

  const removeItem = useCallback(
    (item: SelectionType) => {
      const items = value ? String(value).split(",") : [];
      const next = items
        .filter((x) => String(x) !== String(item.value))
        .join(",");
      setValue(next, true);
    },
    [setValue, value],
  );

  const renderValue = useCallback(
    ({ option }: SelectOptionProps<SelectionType>) => {
      return (
        <SelectionTag
          title={option.title}
          color={option[colorField as keyof typeof option]}
          onRemove={readonly ? undefined : () => removeItem(option)}
        />
      );
    },
    [colorField, readonly, removeItem],
  );

  const renderOption = useCallback(
    ({ option }: SelectOptionProps<SelectionType>) => {
      return (
        <SelectionTag
          title={option.title}
          color={option[colorField as keyof typeof option]}
        />
      );
    },
    [colorField],
  );

  return (
    <Selection
      {...props}
      multiple={multiple}
      renderValue={renderValue}
      renderOption={renderOption}
    />
  );
}
