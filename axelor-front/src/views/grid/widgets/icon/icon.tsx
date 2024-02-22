import { Icon as IconComp } from "@/components/icon";
import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

export function Icon({ rawValue }: GridColumnProps) {
  return (
    <Box d="inline-flex" textAlign="center">{rawValue && <IconComp icon={rawValue as string} />}</Box>
  );
}
