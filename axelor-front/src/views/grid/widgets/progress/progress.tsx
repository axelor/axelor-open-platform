import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

import { ProgressComponent } from "@/views/form/widgets/progress";

export function Progress(props: GridColumnProps) {
  const { data, rawValue } = props;
  return (
    <Box d="flex" flex={1} alignItems="center">
      <ProgressComponent value={rawValue ?? 0} schema={data} />
    </Box>
  );
}
