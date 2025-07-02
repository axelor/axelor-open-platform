import { useAtomValue } from "jotai";

import { TemplateRenderer } from "@/hooks/use-parser";
import { legacyClassNames } from "@/styles/legacy";
import { WidgetProps } from "../../builder";

export function Label({ formAtom, widgetAtom }: WidgetProps) {
  const { attrs } = useAtomValue(widgetAtom);
  const { title = "", css } = attrs;
  const { record } = useAtomValue(formAtom);
  return (
    <label className={legacyClassNames(css)}>
      <TemplateRenderer context={record} template={title} />
    </label>
  );
}
