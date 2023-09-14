import { Box } from "@axelor/ui";
import { ProgressComponent } from "@/views/form/widgets/progress";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

export function Progress(props: GridColumnProps) {
  const { data, value } = props;
  return (
    <Box d="flex" flex={1} alignItems="center">
      <ProgressComponent value={value} schema={data} />
    </Box>
  );
}
