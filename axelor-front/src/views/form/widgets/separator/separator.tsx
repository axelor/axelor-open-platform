import { Box } from "@axelor/ui";
import { useAtomValue } from "jotai";
import { WidgetProps } from "../../builder";

export function Separator({ schema, widgetAtom }: WidgetProps) {
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;
  const showTitle = title && schema.showTitle !== false;
  return (
    <Box>
      {showTitle && <Box fontWeight="bold">{title}</Box>}
      <Box as="hr" my={2} />
    </Box>
  );
}
