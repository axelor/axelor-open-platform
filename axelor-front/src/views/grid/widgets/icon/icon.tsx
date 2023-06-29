import { Icon as IconComp } from "@/components/icon";
import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

export function Icon({ value }: GridColumnProps) {
  return (
    <Box textAlign="center">{value && <IconComp icon={value as string} />}</Box>
  );
}
