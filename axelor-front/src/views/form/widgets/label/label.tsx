import { useTemplate } from "@/hooks/use-parser";
import { useAtomValue } from "jotai";
import { WidgetProps } from "../../builder";
import { useFormField } from "../../builder/scope";

export function Label({ formAtom, widgetAtom }: WidgetProps) {
  const { attrs } = useAtomValue(widgetAtom);
  const { title = "" } = attrs;
  const { record } = useAtomValue(formAtom);
  const Template = useTemplate(title);
  const $getField = useFormField(formAtom);
  return (
    <label>
      <Template
        context={record}
        options={{
          helpers: {
            $getField,
          },
        }}
      />
    </label>
  );
}
