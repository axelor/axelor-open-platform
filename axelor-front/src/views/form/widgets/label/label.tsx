import { useAtomValue } from "jotai";

import { useTemplate } from "@/hooks/use-parser";
import { legacyClassNames } from "@/styles/legacy";
import { WidgetProps } from "../../builder";

export function Label({ formAtom, widgetAtom }: WidgetProps) {
  const { attrs } = useAtomValue(widgetAtom);
  const { title = "", css } = attrs;
  const { record } = useAtomValue(formAtom);
  const Template = useTemplate(title);
  return (
    <label className={legacyClassNames(css)}>
      <Template context={record} />
    </label>
  );
}
