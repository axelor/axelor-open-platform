import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";
import { escape } from "lodash"

export function Url(props: GridColumnProps) {
  const { value } = props;
  return (
    <Box as="a" target="_blank" href={escape(value)}>
      {escape(value)}
    </Box>
  );
}
