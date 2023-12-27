import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

export function Phone(props: GridColumnProps) {
  const { record, data } = props;
  const value = record?.[data?.name];
  return (
    <Box as="a" href={`tel:${value}`}>
      {value}
    </Box>
  );
}
