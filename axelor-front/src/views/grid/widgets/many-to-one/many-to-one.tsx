import { RelationalValue } from "@/views/form/widgets";
import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

export function ManyToOne({ data: column, rawValue }: GridColumnProps) {
  return (
    <Box d="inline-flex">
      <RelationalValue schema={column} value={rawValue} />
    </Box>
  );
}
