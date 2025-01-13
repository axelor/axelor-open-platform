import { useMemo } from "react";

import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

import { RelationalValue } from "@/views/form/widgets";
import { Field } from "@/services/client/meta.types";

export function ManyToOne({ data: column, value, rawValue }: GridColumnProps) {
  const field = column as Field;

  const $value = useMemo(
    () =>
      rawValue && field.targetName
        ? {
            ...rawValue,
            [field.targetName]: value,
          }
        : rawValue,
    [value, rawValue, field],
  );

  return (
    <Box d="inline-flex">
      <RelationalValue schema={column} value={$value} />
    </Box>
  );
}
