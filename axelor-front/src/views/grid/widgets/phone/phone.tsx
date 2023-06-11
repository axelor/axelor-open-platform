import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

export function Phone(props: GridColumnProps) {
  const { value } = props;
  return (
    <Box as="a" href={`tel:${value}`}>
      {value}
    </Box>
  );
}
