import { legacyClassNames } from "@/styles/legacy";
import { Box, Button as Btn } from "@axelor/ui";
import { useAtomValue } from "jotai";
import { FieldContainer, WidgetProps } from "../../builder";

export function Button({
  schema,
  readonly,
  formAtom,
  widgetAtom,
}: WidgetProps) {
  const { title, link, icon } = schema;
  const { attrs } = useAtomValue(widgetAtom);

  const variant = link ? "link" : "primary";

  return (
    <FieldContainer readonly={readonly}>
      <Btn variant={variant}>
        {icon && <Box as="i" me={2} className={legacyClassNames("fa", icon)} />}
        {title}
      </Btn>
    </FieldContainer>
  );
}
