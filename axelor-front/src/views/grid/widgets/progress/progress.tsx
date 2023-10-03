import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

import { ProgressComponent } from "@/views/form/widgets/progress";

export function Progress(props: GridColumnProps) {
  const { data, value, record } = props;
  const rawValue = record?.[data.name] ?? value;
  return (
    <Box d="flex" flex={1} alignItems="center">
      <ProgressComponent value={rawValue ?? 0} schema={data} />
    </Box>
  );
}
