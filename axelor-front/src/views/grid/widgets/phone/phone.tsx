import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

export function Phone(props: GridColumnProps) {
  const { rawValue } = props;
  return (
    <Box as="a" href={`tel:${rawValue}`}>
      {rawValue}
    </Box>
  );
}
