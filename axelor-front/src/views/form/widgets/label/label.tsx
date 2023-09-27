import { useTemplate } from "@/hooks/use-parser";
import { useAtomValue } from "jotai";
import { WidgetProps } from "../../builder";

export function Label({ formAtom, widgetAtom }: WidgetProps) {
  const { attrs } = useAtomValue(widgetAtom);
  const { title = "" } = attrs;
  const { record } = useAtomValue(formAtom);
  const Template = useTemplate(title);
  return (
    <label>
      <Template context={record} />
    </label>
  );
}
