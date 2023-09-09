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
  const { schema, multiple = true } = props;
  const { colorField = "color" } = schema;

  const renderChip = useCallback(
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
      renderValue={renderChip}
      renderOption={renderChip}
    />
  );
}
