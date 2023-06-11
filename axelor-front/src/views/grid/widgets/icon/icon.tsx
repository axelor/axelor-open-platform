import { legacyClassNames } from "@/styles/legacy";
import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

export function Icon({ value }: GridColumnProps) {
  return (
    <Box textAlign="center">
      {value && <i className={legacyClassNames("fa", value)} />}
    </Box>
  );
}
