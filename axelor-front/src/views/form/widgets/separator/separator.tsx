import { Box } from "@axelor/ui";
import { useAtomValue } from "jotai";
import { WidgetProps } from "../../builder";

export function Separator({ schema, widgetAtom }: WidgetProps) {
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;
  return (
    <Box>
      {title && <Box fontWeight="bold">{title}</Box>}
      <Box as="hr" my={2} />
    </Box>
  );
}
