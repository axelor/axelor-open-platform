import { useAtomValue } from "jotai";
import { ButtonGroup as AxButtonGroup } from "@axelor/ui";
import { useMemo } from "react";

import { FormWidget, WidgetControl, WidgetProps } from "../../builder";

export function ButtonGroup(props: WidgetProps) {
  const { formAtom, parentAtom, schema, widgetAtom } = props;
  const { attrs } = useAtomValue(widgetAtom);
  const { readonly } = attrs;

  const items = useMemo(
    () => (schema.items || []).map(item => ({
      ...item,
      widget: item.type === 'button' ? 'button-group-item' : item.type,
    })), [schema]);

  return (
    <WidgetControl {...props}>
      <AxButtonGroup>
        {items?.map((item, ind) => (
          <FormWidget
            key={ind}
            schema={item}
            formAtom={formAtom}
            parentAtom={parentAtom}
            readonly={readonly}
          />
        ))}
      </AxButtonGroup>
    </WidgetControl>
  );
}
