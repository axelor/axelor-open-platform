import { useMemo } from "react";

import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

import { SingleSelectValue } from "../single-select";

export function MultiSelect(props: GridColumnProps) {
  const { data, record } = props;
  const value = record?.[data?.name];
  const items = useMemo(
    () => (value && typeof value === "string" ? value.split(",") : []),
    [value],
  );
  return (
    <Box d="flex" flexWrap="wrap" g={1}>
      {items.map((item) => (
        <SingleSelectValue key={item} schema={data} value={item} />
      ))}
    </Box>
  );
}
